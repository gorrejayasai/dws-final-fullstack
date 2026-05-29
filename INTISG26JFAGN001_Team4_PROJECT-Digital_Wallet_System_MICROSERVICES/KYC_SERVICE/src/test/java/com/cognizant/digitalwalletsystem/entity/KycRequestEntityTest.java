package com.cognizant.digitalwalletsystem.entity;

import com.cognizant.digitalwalletsystem.entity.enums.KycStatus;
import com.cognizant.digitalwalletsystem.entity.enums.RequestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KycRequest Entity Tests")
class KycRequestEntityTest {

    @Test
    @DisplayName("Should create KycRequest with all required fields via builder")
    void shouldCreateKycRequestWithBuilder() {
        LocalDateTime now = LocalDateTime.now();

        KycRequest request = KycRequest.builder()
                .userId(1001L)
                .status(KycStatus.PENDING)
                .requestType(RequestType.NEW_KYC)
                .submittedAt(now)
                .build();

        assertThat(request.getUserId()).isEqualTo(1001L);
        assertThat(request.getStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(request.getRequestType()).isEqualTo(RequestType.NEW_KYC);
        assertThat(request.getSubmittedAt()).isEqualTo(now);
        assertThat(request.getReviewedAt()).isNull();
        assertThat(request.getDocuments()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should allow status transition from PENDING to APPROVED")
    void shouldAllowStatusTransitionToApproved() {
        KycRequest request = KycRequest.builder()
                .userId(1002L)
                .status(KycStatus.PENDING)
                .requestType(RequestType.NEW_KYC)
                .submittedAt(LocalDateTime.now())
                .build();

        request.setStatus(KycStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());

        assertThat(request.getStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(request.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should allow status transition from PENDING to REJECTED")
    void shouldAllowStatusTransitionToRejected() {
        KycRequest request = KycRequest.builder()
                .userId(1003L)
                .status(KycStatus.PENDING)
                .requestType(RequestType.NEW_KYC)
                .submittedAt(LocalDateTime.now())
                .build();

        request.setStatus(KycStatus.REJECTED);
        request.setReviewRemarks("Documents are not clear.");

        assertThat(request.getStatus()).isEqualTo(KycStatus.REJECTED);
        assertThat(request.getReviewRemarks()).isEqualTo("Documents are not clear.");
    }

    @Test
    @DisplayName("Should default documents list to empty on builder")
    void shouldDefaultDocumentsListToEmpty() {
        KycRequest request = KycRequest.builder()
                .userId(1004L)
                .status(KycStatus.PENDING)
                .requestType(RequestType.NEW_KYC)
                .submittedAt(LocalDateTime.now())
                .build();

        assertThat(request.getDocuments()).isNotNull();
        assertThat(request.getDocuments()).isEmpty();
    }

    @Test
    @DisplayName("onPrePersist should set submittedAt and status when null")
    void onPrePersistShouldSetDefaults() {
        KycRequest request = new KycRequest();
        request.setUserId(1005L);
        request.setRequestType(RequestType.NEW_KYC);

        request.onPrePersist();  // manually trigger lifecycle hook

        assertThat(request.getStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(request.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("onPrePersist should NOT overwrite already set submittedAt")
    void onPrePersistShouldNotOverwriteExistingTimestamp() {
        LocalDateTime customTime = LocalDateTime.of(2024, 1, 15, 10, 0);

        KycRequest request = new KycRequest();
        request.setUserId(1006L);
        request.setRequestType(RequestType.NEW_KYC);
        request.setSubmittedAt(customTime);

        request.onPrePersist();

        assertThat(request.getSubmittedAt()).isEqualTo(customTime);
    }
}
