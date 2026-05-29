package com.cognizant.digitalwalletsystem.dto;

import com.cognizant.digitalwalletsystem.entity.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycDocumentRequest {

    @NotNull(message = "Document type is required (ID_PROOF or ADDRESS_PROOF)")
    private DocumentType documentType;

    /*
     * documentNumber:
     *   For ID_PROOF  → enter Aadhaar number (12 digits) OR PAN number (10 chars, 4+ alphabets)
     *   For ADDRESS_PROOF → not required, can be left blank
     *
     * Regex validation is done in KycServiceImpl.validateDocuments() so we can
     * give a clear, specific error message based on the document type.
     */
    @NotBlank(message = "Doc number is mandatory")
    private String documentNumber;

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File reference is required")
    private String fileReference;
}
