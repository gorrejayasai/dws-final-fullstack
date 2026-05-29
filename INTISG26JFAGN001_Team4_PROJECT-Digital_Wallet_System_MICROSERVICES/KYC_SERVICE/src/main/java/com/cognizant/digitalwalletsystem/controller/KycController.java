package com.cognizant.digitalwalletsystem.controller;

import com.cognizant.digitalwalletsystem.dto.AdminReviewRequest;
import com.cognizant.digitalwalletsystem.dto.KycResponse;
import com.cognizant.digitalwalletsystem.dto.KycSubmitRequest;
import com.cognizant.digitalwalletsystem.dto.KycUpdateRequest;
import com.cognizant.digitalwalletsystem.entity.KycDocument;
import com.cognizant.digitalwalletsystem.service.FileStorageService;
import com.cognizant.digitalwalletsystem.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC Service", description = "APIs for KYC submission, admin review and document updates")
public class KycController {

    private final KycService kycService;
    private final FileStorageService fileStorageService;

    // ─────────────────────────────── USER ENDPOINTS ───────────────────────────────

    /**
     * Submit a new KYC request.
     * <p>
     * Sent as {@code multipart/form-data} with two parts:
     * <ul>
     *   <li>{@code data} — JSON body conforming to {@link KycSubmitRequest}</li>
     *   <li>{@code file} — the user's PAN or Aadhaar document (image or PDF)</li>
     * </ul>
     */
    @Operation(summary = "Submit KYC (User)",
            description = "Multipart submission with `data` (JSON) and `file` (the document). "
                    + "ID type must be PAN_BASED or AADHAAR_BASED.")
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KycResponse> submitKyc(
            @Valid @RequestPart("data") KycSubmitRequest request,
            @RequestPart("file") MultipartFile file,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Id") Long userId) {

        KycResponse response = kycService.submitKyc(request, file, role, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an in-progress KYC submission.
     * <p>
     * Same multipart shape as submit. Allowed only while the KYC is PENDING or REJECTED.
     * The previously stored document file is replaced on disk and a new path is persisted in DB.
     */
    @Operation(summary = "Update KYC documents (User)",
            description = "Multipart update with `data` and `file`. Allowed only while KYC status is PENDING or REJECTED.")
    @PutMapping(value = "/user/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KycResponse> updateKycByUser(
            @Valid @RequestPart("data") KycUpdateRequest request,
            @RequestPart("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        KycResponse response = kycService.updateKyc(userId, role, request, file);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────── COMMON ENDPOINTS ───────────────────────────────

    @Operation(summary = "View KYC by userId",
            description = "Returns the full KYC record. ADMIN can view any user; USER can only view their own.")
    @GetMapping("/{userId}")
    public ResponseEntity<KycResponse> getKycByUserId(@PathVariable Long userId,
                                                      @RequestHeader("X-User-Role") String role,
                                                      @RequestHeader("X-User-Id") Long requesterUserId) {
        return ResponseEntity.ok(kycService.getKycByUserId(userId, role, requesterUserId));
    }

    /**
     * Stream a stored KYC document back to the caller for inline viewing / download.
     * <p>
     * ADMIN can view any document; USER can only view documents belonging to their own KYC.
     * The frontend (or admin panel) uses the {@code documentUrl} from {@link com.cognizant.digitalwalletsystem.dto.KycDocumentResponse}
     * to embed the document inline.
     */
    @Operation(summary = "View / download a KYC document",
            description = "Streams the stored document file. ADMIN can access any; USER only their own.")
    @GetMapping("/document/{documentId}")
    public ResponseEntity<Resource> viewDocument(@PathVariable Long documentId,
                                                 @RequestHeader("X-User-Role") String role,
                                                 @RequestHeader("X-User-Id") Long requesterUserId) {
        KycDocument doc = kycService.getDocumentForView(documentId, role, requesterUserId);
        Resource resource = fileStorageService.loadFile(doc.getFileReference());

        String mimeType = doc.getDocumentMimeType() != null
                ? doc.getDocumentMimeType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }

    // ─────────────────────────────── ADMIN ENDPOINTS ───────────────────────────────

    @Operation(summary = "View all KYC submissions (Admin)",
            description = "Returns all KYC records — PENDING, APPROVED and REJECTED.")
    @GetMapping("/admin/all")
    public ResponseEntity<List<KycResponse>> getAllKyc(@RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(kycService.getAllKycRequests(role));
    }

    @Operation(summary = "Approve KYC (Admin)",
            description = "Admin approves a PENDING KYC after document verification. Remarks are optional.")
    @PutMapping("/admin/approve/{userId}")
    public ResponseEntity<KycResponse> approveKyc(
            @PathVariable Long userId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody AdminReviewRequest request) {
        KycResponse response = kycService.approveKyc(userId, request.getRemarks(), role);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reject KYC (Admin)",
            description = "Admin rejects a PENDING KYC. Remarks are mandatory so the user knows what to fix.")
    @PutMapping("/admin/reject/{userId}")
    public ResponseEntity<KycResponse> rejectKyc(
            @PathVariable Long userId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody AdminReviewRequest request) {
        KycResponse response = kycService.rejectKyc(userId, request.getRemarks(), role);
        return ResponseEntity.ok(response);
    }
}