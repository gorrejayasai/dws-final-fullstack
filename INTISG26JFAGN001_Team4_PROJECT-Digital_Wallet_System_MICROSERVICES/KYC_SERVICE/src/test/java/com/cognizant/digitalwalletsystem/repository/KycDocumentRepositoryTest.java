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
@DisplayName("KycDocumentRepository Tests")
class KycDocumentRepositoryTest {

    @Autowired
    private KycDocumentRepository kycDocumentRepository;

    @Autowired
    private KycRequestRepository kycRequestRepository;

    private KycRequest savedRequest;
    private KycDocument idProofDoc;
    private KycDocument addressProofDoc;

    @BeforeEach
    void setUp() {
        kycDocumentRepository.deleteAll();
        kycRequestRepository.deleteAll();

        savedRequest = kycRequestRepository.save(KycRequest.builder()
                .userId(4001L)
                .status(KycStatus.PENDING)
                .requestType(RequestType.NEW_KYC)
                .submittedAt(LocalDateTime.now())
                .build());

        idProofDoc = kycDocumentRepository.save(KycDocument.builder()
                .kycRequest(savedRequest)
                .documentType(DocumentType.ID_PROOF)
                .verificationType(KycVerificationType.AADHAAR_BASED)
                .verifiedName("Test User")
                .verifiedDob(LocalDate.of(1995, 1, 1))
                .documentNumber("123456789012")
                .fileName("aadhaar.jpg")
                .fileReference("user_4001/aadhaar.jpg")
                .uploadedAt(LocalDateTime.now())
                .build());

        addressProofDoc = kycDocumentRepository.save(KycDocument.builder()
                .kycRequest(savedRequest)
                .documentType(DocumentType.ADDRESS_PROOF)
                .verificationType(KycVerificationType.AADHAAR_BASED)
                .verifiedName("Test User")
                .verifiedDob(LocalDate.of(1995, 1, 1))
                .documentNumber("999988887777")
                .fileName("bill.pdf")
                .fileReference("user_4001/bill.pdf")
                .uploadedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("Should find all documents for a given kycRequestId")
    void shouldFindAllByKycRequestId() {
        List<KycDocument> docs = kycDocumentRepository.findAllByKycRequestId(savedRequest.getId());
        assertThat(docs).hasSize(2);
    }

    @Test
    @DisplayName("Should find document by kycRequestId and documentType")
    void shouldFindByKycRequestIdAndDocumentType() {
        Optional<KycDocument> found = kycDocumentRepository
                .findByKycRequestIdAndDocumentType(savedRequest.getId(), DocumentType.ID_PROOF);

        assertThat(found).isPresent();
        assertThat(found.get().getFileName()).isEqualTo("aadhaar.jpg");
    }

    @Test
    @DisplayName("Should return empty when documentType not found for request")
    void shouldReturnEmptyForMissingDocumentType() {
        // Remove address proof and query it
        kycDocumentRepository.delete(addressProofDoc);

        Optional<KycDocument> found = kycDocumentRepository
                .findByKycRequestIdAndDocumentType(savedRequest.getId(), DocumentType.ADDRESS_PROOF);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("countByKycRequestId should return correct count")
    void shouldCountDocumentsByKycRequestId() {
        int count = kycDocumentRepository.countByKycRequestId(savedRequest.getId());
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("deleteAllByKycRequestId should remove all documents")
    void shouldDeleteAllDocumentsByKycRequestId() {
        kycDocumentRepository.deleteAllByKycRequestId(savedRequest.getId());
        List<KycDocument> remaining = kycDocumentRepository.findAllByKycRequestId(savedRequest.getId());
        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for unknown kycRequestId")
    void shouldReturnEmptyForUnknownKycRequestId() {
        List<KycDocument> docs = kycDocumentRepository.findAllByKycRequestId(9999L);
        assertThat(docs).isEmpty();
    }
}
