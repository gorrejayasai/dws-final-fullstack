package com.cognizant.digitalwalletsystem.dto;

import com.cognizant.digitalwalletsystem.entity.enums.DocumentType;
import com.cognizant.digitalwalletsystem.entity.enums.KycVerificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * KYC Document Response DTO returned alongside a KYC record.
 *
 * Used both when:
 * <ul>
 *   <li>User views their own submitted KYC</li>
 *   <li>Admin reviews a KYC submission for approval/rejection</li>
 * </ul>
 *
 * The {@link #documentUrl} field is the path the frontend can hit (via the API gateway)
 * to view or download the stored document file.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocumentResponse {

    private Long id;

    private KycVerificationType verificationType;

    private String verifiedName;

    private LocalDate verifiedDob;

    private String documentNumber;

    /** Original file name as uploaded by the user (e.g. "aadhaar_front.jpg"). */
    private String fileName;

    /**
     * Relative storage path on the KYC service host (e.g. {@code user_42/uuid_aadhaar.jpg}).
     * Useful for support/debug; the frontend should rely on {@link #documentUrl} instead.
     */
    private String fileReference;

    /** MIME type of the stored document (e.g. {@code image/jpeg}, {@code application/pdf}). */
    private String documentMimeType;

    /**
     * API path the frontend can call to view/download the document inline.
     * Example: {@code /kyc/document/123}
     */
    private String documentUrl;

    private LocalDateTime uploadedAt;

    /** Legacy field, retained for backward compatibility with older clients. */
    private DocumentType documentType;
}