package com.cognizant.digitalwalletsystem.repository;

import com.cognizant.digitalwalletsystem.entity.KycDocument;
import com.cognizant.digitalwalletsystem.entity.enums.DocumentType;
//import com.cognizant.digitalwalletsystem.entity.enums.KycVerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findAllByKycRequestId(Long kycRequestId);

    Optional<KycDocument> findByKycRequestIdAndDocumentType(Long kycRequestId, DocumentType documentType);

    int countByKycRequestId(Long kycRequestId);

    /**
     * Check if a document number already exists in the system
     * Used to prevent duplicate document registrations across different users
     *
     * @param documentNumber the document number to check (PAN or Aadhaar)
     * @return true if document number exists, false otherwise
     */
    @Query("SELECT COUNT(d) > 0 FROM KycDocument d WHERE d.documentNumber = :documentNumber")
    boolean existsByDocumentNumber(@Param("documentNumber") String documentNumber);

    /**
     * BUG FIX: Spring Data JPA derived delete methods REQUIRE @Modifying + @Transactional.
     *
     * Without @Modifying  → InvalidDataAccessApiUsageException at runtime.
     * Without @Transactional → TransactionRequiredException (no active transaction).
     *
     * Also replaced the derived-delete style with an explicit JPQL DELETE query.
     * The derived style would first SELECT all matching rows then DELETE each one
     * individually (N+1 deletes). The JPQL DELETE executes a single SQL DELETE
     * statement — much more efficient when replacing all documents on a KYC update.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM KycDocument d WHERE d.kycRequest.id = :kycRequestId")
    void deleteAllByKycRequestId(@Param("kycRequestId") Long kycRequestId);
}
