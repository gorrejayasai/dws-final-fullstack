package com.cognizant.digitalwalletsystem.entity;

import com.cognizant.digitalwalletsystem.entity.enums.DocumentType;
import com.cognizant.digitalwalletsystem.entity.enums.KycVerificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kyc_request_id", nullable = false)
    private KycRequest kycRequest;

    /**
     * KYC Verification Type - Either PAN_BASED or AADHAAR_BASED
     * This indicates which type of KYC verification the user chose
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 20)
    private KycVerificationType verificationType;

    /**
     * User's full name as provided during KYC submission
     * Used for admin review and verification
     */
    @Column(name = "verified_name", nullable = false, length = 100)
    private String verifiedName;

    /**
     * User's date of birth as provided during KYC submission
     * Used for admin review and verification
     */
    @Column(name = "verified_dob", nullable = false)
    private LocalDate verifiedDob;

    /**
     * Document Number (PAN or Aadhaar based on verification type)
     * PAN: 10 alphanumeric characters
     * Aadhaar: 12 digits
     */
    @Column(name = "document_number", nullable = false, length = 20, unique = true)
    private String documentNumber;

    // Legacy field - kept for backward compatibility
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = true, length = 20)
    private DocumentType documentType;

    /**
     * Original file name as uploaded by the user (e.g. "aadhaar_front.jpg")
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /**
     * Relative path under {@code app.upload.dir} where the document is stored on the local filesystem.
     * Example: {@code user_42/9f3c-...-aadhaar_front.jpg}
     * The admin viewer endpoint resolves this against {@code app.upload.dir} to stream the file back.
     */
    @Column(name = "file_reference", nullable = false)
    private String fileReference;

    /**
     * MIME type of the document (e.g., "image/jpeg", "application/pdf").
     * Captured from the uploaded {@code MultipartFile} so the view endpoint can return the
     * correct Content-Type for inline display in the admin panel.
     */
    @Column(name = "document_mime_type", length = 100)
    private String documentMimeType;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    public void onPrePersist() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }
}
