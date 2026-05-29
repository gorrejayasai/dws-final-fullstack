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
 * KYC Update Request DTO — JSON metadata part of the multipart update.
 *
 * Mirrors {@link KycSubmitRequest} since the new flow stores a single document
 * per KYC. The replacement file is sent as the {@code file} part of the multipart request.
 *
 * Allowed only while the KYC is in PENDING or REJECTED state.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycUpdateRequest {

    @NotNull(message = "Verification type is required (PAN_BASED or AADHAAR_BASED)")
    private KycVerificationType verificationType;

    @NotBlank(message = "Verified name is required")
    private String verifiedName;

    @NotNull(message = "Date of birth is required")
    private LocalDate verifiedDob;

    @NotBlank(message = "Document number is required")
    private String documentNumber;
}