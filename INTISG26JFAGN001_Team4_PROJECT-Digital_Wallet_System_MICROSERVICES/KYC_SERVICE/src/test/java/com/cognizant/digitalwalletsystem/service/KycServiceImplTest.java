package com.cognizant.digitalwalletsystem.service;

import com.cognizant.digitalwalletsystem.dto.CreateWalletRequest;

import com.cognizant.digitalwalletsystem.dto.KycResponse;
import com.cognizant.digitalwalletsystem.dto.KycSubmitRequest;
import com.cognizant.digitalwalletsystem.dto.KycUpdateRequest;
import com.cognizant.digitalwalletsystem.dto.WalletResponse;
import com.cognizant.digitalwalletsystem.entity.KycDocument;
import com.cognizant.digitalwalletsystem.entity.KycRequest;

import com.cognizant.digitalwalletsystem.entity.enums.KycStatus;
import com.cognizant.digitalwalletsystem.entity.enums.KycVerificationType;
import com.cognizant.digitalwalletsystem.entity.enums.RequestType;
import com.cognizant.digitalwalletsystem.entity.enums.UserType;
import com.cognizant.digitalwalletsystem.exception.DeniedAccessException;
import com.cognizant.digitalwalletsystem.exception.InvalidDocumentException;
import com.cognizant.digitalwalletsystem.exception.KycAlreadyExistsException;
import com.cognizant.digitalwalletsystem.exception.KycNotFoundException;
import com.cognizant.digitalwalletsystem.exception.KycStatusException;
import com.cognizant.digitalwalletsystem.repository.KycDocumentRepository;
import com.cognizant.digitalwalletsystem.repository.KycRequestRepository;
import com.cognizant.digitalwalletsystem.util.NotificationServiceClient;
import com.cognizant.digitalwalletsystem.util.UserServiceClient;
import com.cognizant.digitalwalletsystem.util.WalletServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KycServiceImpl Unit Tests")
class KycServiceImplTest {

    @Mock
    private KycRequestRepository kycRequestRepository;
    @Mock
    private KycDocumentRepository kycDocumentRepository;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private WalletServiceClient walletServiceClient;
    @Mock
    private NotificationServiceClient notificationServiceClient;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private KycServiceImpl kycService;

    private static MultipartFile aadhaarFile() {
        return new MockMultipartFile("file", "aadhaar.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private static MultipartFile panFile() {
        return new MockMultipartFile("file", "pan.jpg", "image/jpeg", new byte[]{4, 5, 6});
    }

    private KycSubmitRequest buildAadhaarSubmit() {
        return KycSubmitRequest.builder()
                .verificationType(KycVerificationType.AADHAAR_BASED)
                .verifiedName("Test User")
                .verifiedDob(LocalDate.of(1995, 1, 1))
                .documentNumber("123456789012")
                .build();
    }

    private KycRequest buildPersistedRequest(Long userId, KycStatus status) {
        return KycRequest.builder()
                .id(1L)
                .userId(userId)
                .status(status)
                .requestType(RequestType.NEW_KYC)
                .submittedAt(LocalDateTime.now())
                .documents(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("submitKyc()")
    class SubmitKycTests {

        @Test
        @DisplayName("Should submit KYC successfully for USER role")
        void shouldSubmitWithUserRole() {
            Long userId = 5001L;
            KycSubmitRequest req = buildAadhaarSubmit();
            MultipartFile file = aadhaarFile();

            when(kycRequestRepository.existsByUserId(userId)).thenReturn(false);
            when(kycDocumentRepository.existsByDocumentNumber(req.getDocumentNumber())).thenReturn(false);
            when(fileStorageService.saveFile(any(MultipartFile.class), anyLong()))
                    .thenReturn("user_" + userId + "/uuid_aadhaar.jpg");
            when(kycRequestRepository.save(any(KycRequest.class)))
                    .thenReturn(buildPersistedRequest(userId, KycStatus.PENDING));
            when(kycDocumentRepository.save(any(KycDocument.class))).thenAnswer(inv -> inv.getArgument(0));

            KycResponse response = kycService.submitKyc(req, file, UserType.USER.name(), userId);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getStatus()).isEqualTo(KycStatus.PENDING);
            verify(fileStorageService).saveFile(file, userId);
            verify(kycDocumentRepository, times(1)).save(any(KycDocument.class));
        }

        @Test
        @DisplayName("Should throw DeniedAccessException for non-USER role")
        void shouldThrowDeniedForNonUserRole() {
            assertThatThrownBy(() ->
                    kycService.submitKyc(buildAadhaarSubmit(), aadhaarFile(), UserType.ADMIN.name(), 5002L))
                    .isInstanceOf(DeniedAccessException.class)
                    .hasMessageContaining("User role required");
        }

        @Test
        @DisplayName("Should throw KycAlreadyExistsException on duplicate submission")
        void shouldThrowOnDuplicateKycSubmission() {
            Long userId = 5003L;
            when(kycRequestRepository.existsByUserId(userId)).thenReturn(true);

            assertThatThrownBy(() ->
                    kycService.submitKyc(buildAadhaarSubmit(), aadhaarFile(), UserType.USER.name(), userId))
                    .isInstanceOf(KycAlreadyExistsException.class)
                    .hasMessageContaining("5003");
        }

        @Test
        @DisplayName("Should throw InvalidDocumentException when no file is provided")
        void shouldThrowWhenNoFileProvided() {
            Long userId = 5004L;
            when(kycRequestRepository.existsByUserId(userId)).thenReturn(false);

            MultipartFile empty = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() ->
                    kycService.submitKyc(buildAadhaarSubmit(), empty, UserType.USER.name(), userId))
                    .isInstanceOf(InvalidDocumentException.class)
                    .hasMessageContaining("Document file is required");
        }
    }

    @Nested
    @DisplayName("getKycByUserId()")
    class GetKycTests {

        @Test
        @DisplayName("Should return KYC when userId exists")
        void shouldReturnKycForExistingUser() {
            when(kycRequestRepository.findByUserId(6001L))
                    .thenReturn(Optional.of(buildPersistedRequest(6001L, KycStatus.APPROVED)));

            KycResponse response = kycService.getKycByUserId(6001L, UserType.ADMIN.name(), 9999L);

            assertThat(response.getUserId()).isEqualTo(6001L);
            assertThat(response.getStatus()).isEqualTo(KycStatus.APPROVED);
        }

        @Test
        @DisplayName("Should return KYC when USER views their own record")
        void shouldReturnKycForUserViewingOwnRecord() {
            when(kycRequestRepository.findByUserId(6001L))
                    .thenReturn(Optional.of(buildPersistedRequest(6001L, KycStatus.PENDING)));

            KycResponse response = kycService.getKycByUserId(6001L, UserType.USER.name(), 6001L);

            assertThat(response.getUserId()).isEqualTo(6001L);
            assertThat(response.getStatus()).isEqualTo(KycStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw KycNotFoundException for unknown userId")
        void shouldThrowForUnknownUser() {
            when(kycRequestRepository.findByUserId(9999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    kycService.getKycByUserId(9999L, UserType.USER.name(), 9999L))
                    .isInstanceOf(KycNotFoundException.class)
                    .hasMessageContaining("9999");
        }
    }

    @Nested
    @DisplayName("getAllKycRequests()")
    class GetAllKycTests {

        @Test
        @DisplayName("Should return all KYC requests for ADMIN role")
        void shouldReturnAllForAdmin() {
            when(kycRequestRepository.findAll())
                    .thenReturn(List.of(buildPersistedRequest(9001L, KycStatus.PENDING)));

            List<KycResponse> responses = kycService.getAllKycRequests(UserType.ADMIN.name());

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getUserId()).isEqualTo(9001L);
        }

        @Test
        @DisplayName("Should throw DeniedAccessException for USER role")
        void shouldDenyForUser() {
            assertThatThrownBy(() -> kycService.getAllKycRequests(UserType.USER.name()))
                    .isInstanceOf(DeniedAccessException.class)
                    .hasMessageContaining("Admin role required");
        }
    }

    @Nested
    @DisplayName("approveKyc()")
    class ApproveKycTests {

        @Test
        @DisplayName("Admin should approve PENDING KYC and trigger wallet creation")
        void adminShouldApproveKyc() {
            KycRequest existing = buildPersistedRequest(7001L, KycStatus.PENDING);
            when(kycRequestRepository.findByUserId(7001L)).thenReturn(Optional.of(existing));
            when(kycRequestRepository.save(any(KycRequest.class))).thenAnswer(i -> i.getArgument(0));
            when(walletServiceClient.createWallet(any(CreateWalletRequest.class)))
                    .thenReturn(new WalletResponse());

            KycResponse response = kycService.approveKyc(7001L, "All documents verified", UserType.ADMIN.name());

            assertThat(response.getStatus()).isEqualTo(KycStatus.APPROVED);
            verify(walletServiceClient).createWallet(any(CreateWalletRequest.class));
        }

        @Test
        @DisplayName("Non-admin should not approve KYC")
        void nonAdminShouldNotApprove() {
            assertThatThrownBy(() -> kycService.approveKyc(7001L, "ok", UserType.USER.name()))
                    .isInstanceOf(DeniedAccessException.class)
                    .hasMessageContaining("Admin role required");
        }
    }

    @Nested
    @DisplayName("rejectKyc()")
    class RejectKycTests {

        @Test
        @DisplayName("Admin should reject PENDING KYC with remarks")
        void adminShouldRejectKyc() {
            KycRequest existing = buildPersistedRequest(7002L, KycStatus.PENDING);
            when(kycRequestRepository.findByUserId(7002L)).thenReturn(Optional.of(existing));
            when(kycRequestRepository.save(any(KycRequest.class))).thenAnswer(i -> i.getArgument(0));

            KycResponse response = kycService.rejectKyc(7002L, "Image is blurry", UserType.ADMIN.name());

            assertThat(response.getStatus()).isEqualTo(KycStatus.REJECTED);
            assertThat(response.getReviewRemarks()).isEqualTo("Image is blurry");
        }

        @Test
        @DisplayName("Reject should fail when remarks are missing")
        void rejectShouldFailWithoutRemarks() {
            when(kycRequestRepository.findByUserId(7003L))
                    .thenReturn(Optional.of(buildPersistedRequest(7003L, KycStatus.PENDING)));

            assertThatThrownBy(() -> kycService.rejectKyc(7003L, "   ", UserType.ADMIN.name()))
                    .isInstanceOf(KycStatusException.class)
                    .hasMessageContaining("Rejection remarks are required");
        }
    }

    @Nested
    @DisplayName("updateKyc()")
    class UserUpdateTests {

        private KycUpdateRequest buildUpdateRequest() {
            return KycUpdateRequest.builder()
                    .verificationType(KycVerificationType.AADHAAR_BASED)
                    .verifiedName("Test User Updated")
                    .verifiedDob(LocalDate.of(1995, 1, 1))
                    .documentNumber("123456789012")
                    .build();
        }

        @Test
        @DisplayName("User should update document while PENDING")
        void userShouldUpdateDocumentsWhilePending() {
            KycRequest existing = buildPersistedRequest(8001L, KycStatus.PENDING);

            when(kycRequestRepository.findByUserId(8001L)).thenReturn(Optional.of(existing));
            when(kycRequestRepository.save(any(KycRequest.class))).thenAnswer(i -> i.getArgument(0));
            when(fileStorageService.saveFile(any(MultipartFile.class), anyLong()))
                    .thenReturn("user_8001/uuid_new_aadhaar.jpg");
            doNothing().when(kycDocumentRepository).deleteAllByKycRequestId(anyLong());
            when(kycDocumentRepository.save(any(KycDocument.class))).thenAnswer(inv -> inv.getArgument(0));

            KycResponse response = kycService.updateKyc(
                    8001L, UserType.USER.name(), buildUpdateRequest(), panFile());


            assertThat(response).isNotNull();
            assertThat(response.getRequestType()).isEqualTo(RequestType.UPDATE_KYC);
            verify(fileStorageService).saveFile(any(MultipartFile.class), eq(8001L));
        }

        @Test
        @DisplayName("Admin role should be denied for updateKyc")
        void adminRoleShouldBeDeniedForUpdate() {
            assertThatThrownBy(() ->
                    kycService.updateKyc(8002L, UserType.ADMIN.name(), buildUpdateRequest(), aadhaarFile()))


                    .isInstanceOf(DeniedAccessException.class)
                    .hasMessageContaining("User role required");
        }

        @Test
        @DisplayName("User cannot update when KYC is APPROVED")
        void userCannotUpdateWhenApproved() {
            when(kycRequestRepository.findByUserId(8003L))
                    .thenReturn(Optional.of(buildPersistedRequest(8003L, KycStatus.APPROVED)));

            assertThatThrownBy(() ->
                    kycService.updateKyc(8003L, UserType.USER.name(), buildUpdateRequest(), aadhaarFile()))
                    .isInstanceOf(KycStatusException.class)
                    .hasMessageContaining("PENDING or REJECTED");
        }
    }

}