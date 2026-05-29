package com.cognizant.digitalwalletsystem.dto;

import com.cognizant.digitalwalletsystem.entity.enums.KycVerificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * KYC Submit Request DTO — JSON metadata part of the multipart submission.
 *
 * The user picks ONE verification method:
 * <ul>
 *   <li>PAN_BASED — upload PAN document</li>
 *   <li>AADHAAR_BASED — upload Aadhaar document</li>
 * </ul>
 *
 * The actual document file is sent as a separate {@code file} part of the multipart request.
 * The controller binds this DTO via {@code @RequestPart("data")} and the file via
 * {@code @RequestPart("file")}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycSubmitRequest {

    @NotNull(message = "Verification type is required (PAN_BASED or AADHAAR_BASED)")
    private KycVerificationType verificationType;

    @NotBlank(message = "Verified name is required")
    private String verifiedName;

    @NotNull(message = "Date of birth is required")
    private LocalDate verifiedDob;

    /**
     * Document number based on verification type:
     * <ul>
     *   <li>PAN_BASED — 10 alphanumeric characters with at least 4 alphabets (e.g. "ABCDE1234F")</li>
     *   <li>AADHAAR_BASED — exactly 12 digits (e.g. "123456789012")</li>
     * </ul>
     */
    @NotBlank(message = "Document number is required")
    private String documentNumber;
}