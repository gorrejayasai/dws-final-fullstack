package com.cognizant.digitalwalletsystem.service;

import com.cognizant.digitalwalletsystem.dto.KycResponse;
import com.cognizant.digitalwalletsystem.dto.KycSubmitRequest;
import com.cognizant.digitalwalletsystem.dto.KycUpdateRequest;
import com.cognizant.digitalwalletsystem.entity.KycDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KycService {

    // *** User operations *** //

    /** User submits a new KYC request with verification metadata plus the document file. */
    KycResponse submitKyc(KycSubmitRequest request, MultipartFile file, String role, Long userId);

    /** User updates their own document while status is PENDING or REJECTED. */
    KycResponse updateKyc(Long userId, String role, KycUpdateRequest request, MultipartFile file);

    // *** Common operation *** //

    KycResponse getKycByUserId(Long targetUserId, String role, Long requesterUserId);

    /**
     * Look up a stored document by its DB id and enforce access control:
     * USER can only access documents on their own KYC, ADMIN can access any.
     */
    KycDocument getDocumentForView(Long documentId, String role, Long requesterUserId);

    // *** Admin operations *** //

    List<KycResponse> getAllKycRequests(String role);

    KycResponse approveKyc(Long userId, String remarks, String role);

    KycResponse rejectKyc(Long userId, String remarks, String role);

}