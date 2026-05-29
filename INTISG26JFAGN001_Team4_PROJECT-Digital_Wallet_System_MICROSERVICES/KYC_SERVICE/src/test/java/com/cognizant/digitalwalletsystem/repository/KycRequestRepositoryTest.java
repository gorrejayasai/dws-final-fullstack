package com.cognizant.digitalwalletsystem.repository;

import com.cognizant.digitalwalletsystem.entity.KycDocument;
import com.cognizant.digitalwalletsystem.entity.KycRequest;
import com.cognizant.digitalwalletsystem.entity.enums.DocumentType;
import com.cognizant.digitalwalletsystem.entity.enums.KycStatus;
import com.cognizant.digitalwalletsystem.entity.enums.KycVerificationType;
import com.cognizant.digitalwalletsystem.entity.enums.RequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("KycRequestRepository Tests")
class KycRequestRepositoryTest {

    @Autowired
    private KycRequestRepository kycRequestRepository;

    @Autowired
    private KycDocumentRepository kycDocumentRepository;

    private KycRequest savedRequest;

    @BeforeEach
    void setUp() {
        kycDocumentRepository.deleteAll();
        kycRequestRepository.deleteAll();

        KycRequest request = KycRequest.builder()
                .userId(3001L)
                .status(KycStatus.PENDING)
                .requestType(RequestType.NEW_KYC)
                .submittedAt(LocalDateTime.now())
                .build();
        savedRequest = kycRequestRepository.save(request);
    }

    @Test
    @DisplayName("Should save and find KycRequest by id")
    void shouldSaveAndFindById() {
        Optional<KycRequest> found = kycRequestRepository.findById(savedRequest.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(3001L);
    }

    @Test
    @DisplayName("Should find KycRequest by userId")
    void shouldFindByUserId() {
        Optional<KycRequest> found = kycRequestRepository.findByUserId(3001L);
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(KycStatus.PENDING);
    }

    @Test
    @DisplayName("Should return empty when userId not found")
    void shouldReturnEmptyForUnknownUserId() {
        Optional<KycRequest> found = kycRequestRepository.findByUserId(9999L);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByUserId should return true for existing userId")
    void existsByUserIdShouldReturnTrue() {
        assertThat(kycRequestRepository.existsByUserId(3001L)).isTrue();
    }

    @Test
    @DisplayName("existsByUserId should return false for unknown userId")
    void existsByUserIdShouldReturnFalse() {
        assertThat(kycRequestRepository.existsByUserId(9999L)).isFalse();
    }

    @Test
    @DisplayName("Should find all KycRequests with status PENDING")
    void shouldFindAllByStatusPending() {
        List<KycRequest> pending = kycRequestRepository.findAllByStatus(KycStatus.PENDING);
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getUserId()).isEqualTo(3001L);
    }

    @Test
    @DisplayName("Should return empty list when no APPROVED requests exist")
    void shouldReturnEmptyForApprovedWhenNoneExist() {
        List<KycRequest> approved = kycRequestRepository.findAllByStatus(KycStatus.APPROVED);
        assertThat(approved).isEmpty();
    }

    @Test
    @DisplayName("Should update KycRequest status to APPROVED")
    void shouldUpdateStatusToApproved() {
        savedRequest.setStatus(KycStatus.APPROVED);
        savedRequest.setReviewedAt(LocalDateTime.now());
        kycRequestRepository.save(savedRequest);

        Optional<KycRequest> updated = kycRequestRepository.findByUserId(3001L);
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(updated.get().getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should delete KycRequest and cascade to documents")
    void shouldDeleteKycRequestAndCascadeDocuments() {
        KycDocument doc = KycDocument.builder()
                .kycRequest(savedRequest)
                .documentType(DocumentType.ID_PROOF)
                .verificationType(KycVerificationType.AADHAAR_BASED)
                .verifiedName("Test User")
                .verifiedDob(LocalDate.of(1995, 1, 1))
                .documentNumber("111122223333")
                .fileName("aadhaar.jpg")
                .fileReference("user_3001/aadhaar.jpg")
                .uploadedAt(LocalDateTime.now())
                .build();
        savedRequest.getDocuments().add(kycDocumentRepository.save(doc));

        kycRequestRepository.delete(savedRequest);

        assertThat(kycRequestRepository.findById(savedRequest.getId())).isEmpty();
        assertThat(kycDocumentRepository.findAllByKycRequestId(savedRequest.getId())).isEmpty();
    }
}
