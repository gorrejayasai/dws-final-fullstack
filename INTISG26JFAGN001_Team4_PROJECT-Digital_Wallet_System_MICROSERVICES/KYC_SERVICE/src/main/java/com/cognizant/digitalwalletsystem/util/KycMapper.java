package com.cognizant.digitalwalletsystem.util;

import com.cognizant.digitalwalletsystem.dto.KycDocumentResponse;
import com.cognizant.digitalwalletsystem.dto.KycResponse;
import com.cognizant.digitalwalletsystem.entity.KycDocument;
import com.cognizant.digitalwalletsystem.entity.KycRequest;

import java.util.List;
import java.util.stream.Collectors;

public final class KycMapper {

    private KycMapper() {}

    public static KycResponse toKycResponse(KycRequest entity) {
        List<KycDocumentResponse> docResponses = entity.getDocuments().stream()
                .map(KycMapper::toDocumentResponse)
                .collect(Collectors.toList());

        return KycResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .status(entity.getStatus())
                .requestType(entity.getRequestType())
                .submittedAt(entity.getSubmittedAt())
                .reviewedAt(entity.getReviewedAt())
                .reviewRemarks(entity.getReviewRemarks())
                .documents(docResponses)
                .build();
    }

    /**
     * Maps a {@link KycDocument} to its response DTO.
     * <p>
     * {@code documentUrl} is the API path the frontend hits to view/download the
     * stored document — see {@code GET /kyc/document/{documentId}} in the controller.
     */
    public static KycDocumentResponse toDocumentResponse(KycDocument doc) {
        return KycDocumentResponse.builder()
                .id(doc.getId())
                .verificationType(doc.getVerificationType())
                .verifiedName(doc.getVerifiedName())
                .verifiedDob(doc.getVerifiedDob())
                .documentNumber(doc.getDocumentNumber())
                .fileName(doc.getFileName())
                .fileReference(doc.getFileReference())
                .documentMimeType(doc.getDocumentMimeType())
                .documentUrl(doc.getId() != null ? "/kyc/document/" + doc.getId() : null)
                .uploadedAt(doc.getUploadedAt())
                .documentType(doc.getDocumentType())
                .build();
    }
}