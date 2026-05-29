package com.cognizant.digitalwalletsystem.entity;

import com.cognizant.digitalwalletsystem.entity.enums.DocumentType;
import com.cognizant.digitalwalletsystem.entity.enums.KycStatus;
import com.cognizant.digitalwalletsystem.entity.enums.RequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KycDocument Entity Tests")
class KycDocumentEntityTest {

    private KycRequest parentRequest;

    @BeforeEach
    void setUp() {
        parentRequest = KycRequest.builder()
                .userId(2001L)
                .status(KycStatus.PENDING)
                .requestType(RequestType.NEW_KYC)
                .submittedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create KycDocument with all fields via builder")
    void shouldCreateDocumentWithBuilder() {
        LocalDateTime uploadTime = LocalDateTime.now();

        KycDocument doc = KycDocument.builder()
                .kycRequest(parentRequest)
                .documentType(DocumentType.ID_PROOF)
                .fileName("aadhaar_front.jpg")
                .fileReference("uploads/2001/aadhaar_front.jpg")
                .uploadedAt(uploadTime)
                .build();

        assertThat(doc.getDocumentType()).isEqualTo(DocumentType.ID_PROOF);
        assertThat(doc.getFileName()).isEqualTo("aadhaar_front.jpg");
        assertThat(doc.getFileReference()).isEqualTo("uploads/2001/aadhaar_front.jpg");
        assertThat(doc.getUploadedAt()).isEqualTo(uploadTime);
        assertThat(doc.getKycRequest()).isEqualTo(parentRequest);
    }

    @Test
    @DisplayName("Should create ADDRESS_PROOF document")
    void shouldCreateAddressProofDocument() {
        KycDocument doc = KycDocument.builder()
                .kycRequest(parentRequest)
                .documentType(DocumentType.ADDRESS_PROOF)
                .fileName("utility_bill.pdf")
                .fileReference("uploads/2001/utility_bill.pdf")
                .uploadedAt(LocalDateTime.now())
                .build();

        assertThat(doc.getDocumentType()).isEqualTo(DocumentType.ADDRESS_PROOF);
        assertThat(doc.getFileName()).isEqualTo("utility_bill.pdf");
    }

    @Test
    @DisplayName("onPrePersist should set uploadedAt when null")
    void onPrePersistShouldSetUploadedAt() {
        KycDocument doc = new KycDocument();
        doc.setKycRequest(parentRequest);
        doc.setDocumentType(DocumentType.ID_PROOF);
        doc.setFileName("pan_card.jpg");
        doc.setFileReference("uploads/2001/pan_card.jpg");

        doc.onPrePersist();

        assertThat(doc.getUploadedAt()).isNotNull();
    }

    @Test
    @DisplayName("onPrePersist should NOT overwrite existing uploadedAt")
    void onPrePersistShouldNotOverwriteExistingUploadedAt() {
        LocalDateTime fixedTime = LocalDateTime.of(2024, 6, 1, 9, 30);

        KycDocument doc = KycDocument.builder()
                .kycRequest(parentRequest)
                .documentType(DocumentType.ID_PROOF)
                .fileName("pan_card.jpg")
                .fileReference("uploads/2001/pan_card.jpg")
                .uploadedAt(fixedTime)
                .build();

        doc.onPrePersist();

        assertThat(doc.getUploadedAt()).isEqualTo(fixedTime);
    }

    @Test
    @DisplayName("DocumentType enum should expose AADHAAR and PAN (current flow) and the legacy values")
    void documentTypeEnumShouldExposeAllValues() {
        assertThat(DocumentType.values()).containsExactlyInAnyOrder(
                DocumentType.ID_PROOF,
                DocumentType.ADDRESS_PROOF
        );
    }
}
