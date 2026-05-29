package com.cognizant.digitalwalletsystem.service;

import com.cognizant.digitalwalletsystem.dto.*;
import com.cognizant.digitalwalletsystem.entity.KycDocument;
import com.cognizant.digitalwalletsystem.entity.KycRequest;
import com.cognizant.digitalwalletsystem.entity.enums.DocumentType;
import com.cognizant.digitalwalletsystem.entity.enums.KycStatus;
import com.cognizant.digitalwalletsystem.entity.enums.KycVerificationType;
import com.cognizant.digitalwalletsystem.entity.enums.UserType;
import com.cognizant.digitalwalletsystem.entity.enums.RequestType;
import com.cognizant.digitalwalletsystem.exception.InvalidDocumentException;
import com.cognizant.digitalwalletsystem.exception.KycAlreadyExistsException;
import com.cognizant.digitalwalletsystem.exception.KycNotFoundException;
import com.cognizant.digitalwalletsystem.exception.DeniedAccessException;
import com.cognizant.digitalwalletsystem.exception.KycStatusException;
import com.cognizant.digitalwalletsystem.repository.KycDocumentRepository;
import com.cognizant.digitalwalletsystem.repository.KycRequestRepository;
import com.cognizant.digitalwalletsystem.util.KycMapper;
import com.cognizant.digitalwalletsystem.util.NotificationPayloadBuilder;
import com.cognizant.digitalwalletsystem.util.NotificationServiceClient;
import com.cognizant.digitalwalletsystem.util.UserServiceClient;
import com.cognizant.digitalwalletsystem.util.WalletServiceClient;
import com.cognizant.digitalwalletsystem.entity.enums.KycNotificationEvent;
import com.cognizant.digitalwalletsystem.entity.enums.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycServiceImpl implements KycService {

    private final KycRequestRepository kycRequestRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final UserServiceClient userServiceClient;
    private final WalletServiceClient walletServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final FileStorageService fileStorageService;

    // Aadhaar: exactly 12 digits
    private static final String AADHAAR_REGEX = "[0-9]{12}";

    // PAN: exactly 10 alphanumeric (letters A-Z, digits 0-9). The 4-alphabet rule is checked separately.
    private static final String PAN_REGEX = "[A-Za-z0-9]{10}";

    // 1. SUBMIT KYC REQUEST (USER ONLY)
    @Override
    @Transactional
    public KycResponse submitKyc(KycSubmitRequest request, MultipartFile file, String role, Long userId) {

        if (!role.equals(UserType.USER.name())) {
            throw new DeniedAccessException("Access denied. User role required to submit KYC requests.");
        }

        if (kycRequestRepository.existsByUserId(userId)) {
            throw new KycAlreadyExistsException(userId);
        }

        validateNewKycRequest(request, file);
        checkDuplicateDocumentNumber(request.getDocumentNumber(), request.getVerificationType());

        // Persist the file to the local filesystem; only the relative path is stored in DB.
        String storedPath = fileStorageService.saveFile(file, userId);

        KycRequest kycRequest = new KycRequest();
        kycRequest.setUserId(userId);
        kycRequest.setStatus(KycStatus.PENDING);
        kycRequest.setRequestType(RequestType.NEW_KYC);
        kycRequest.setSubmittedAt(LocalDateTime.now());
        kycRequest = kycRequestRepository.save(kycRequest);

        saveKycDocument(kycRequest, request, file, storedPath);

        KycResponse response = KycMapper.toKycResponse(kycRequest);

        // Notification — fire and forget; do not block submission on failure.
        try {
            String payload = NotificationPayloadBuilder.buildKycSubmittedPayload(request.getVerifiedName());
            SendNotificationRequest notificationRequest = new SendNotificationRequest(
                    userId,
                    NotificationChannel.EMAIL,
                    KycNotificationEvent.KYC_SUBMITTED.getTemplateCode(),
                    payload
            );
            notificationServiceClient.sendAsync(notificationRequest);
            log.info("KYC submission notification sent to user: {} with verification type: {}",
                    userId, request.getVerificationType());
        } catch (Exception e) {
            log.error("Failed to send KYC submission notification for user {}: {}", userId, e.getMessage(), e);
        }

        return response;
    }

    // 2. VIEW KYC DETAILS BY USER ID
    @Override
    @Transactional(readOnly = true)
    public KycResponse getKycByUserId(Long targetUserId, String role, Long requesterUserId) {
        KycRequest kycRequest = kycRequestRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new KycNotFoundException(targetUserId));

        if (UserType.USER.name().equals(role) && !requesterUserId.equals(targetUserId)) {
            throw new DeniedAccessException("Access denied. Users can only view their own KYC records.");
        }

        return KycMapper.toKycResponse(kycRequest);
    }

    // 3. VIEW ALL KYC REQUESTS (Admin only)
    @Override
    @Transactional(readOnly = true)
    public List<KycResponse> getAllKycRequests(String role) {
        if (!role.equals(UserType.ADMIN.name())) {
            throw new DeniedAccessException("Access denied. Admin role required to view all KYC requests.");
        }
        List<KycRequest> allRequests = kycRequestRepository.findAll();
        List<KycResponse> responses = new ArrayList<>();
        for (KycRequest req : allRequests) {
            responses.add(KycMapper.toKycResponse(req));
        }
        return responses;
    }

    // 4. APPROVE KYC (Admin only)
    @Override
    @Transactional
    public KycResponse approveKyc(Long userId, String remarks, String role) {
        if (!role.equals(UserType.ADMIN.name())) {
            throw new DeniedAccessException("Access denied. Admin role required to approve KYC requests.");
        }

        KycRequest kycRequest = kycRequestRepository.findByUserId(userId)
                .orElseThrow(() -> new KycNotFoundException(userId));

        if (kycRequest.getStatus() != KycStatus.PENDING) {
            throw new KycStatusException(
                    "Only PENDING KYC requests can be approved. Current status: " + kycRequest.getStatus());
        }

        kycRequest.setStatus(KycStatus.APPROVED);
        kycRequest.setReviewedAt(LocalDateTime.now());
        kycRequest.setReviewRemarks(remarks);
        kycRequest = kycRequestRepository.save(kycRequest);

        try {
            String payload = NotificationPayloadBuilder.buildKycApprovedPayload("User " + userId, remarks);
            SendNotificationRequest notificationRequest = new SendNotificationRequest(
                    userId,
                    NotificationChannel.EMAIL,
                    KycNotificationEvent.KYC_APPROVED.getTemplateCode(),
                    payload
            );
            notificationServiceClient.sendAsync(notificationRequest);
            log.info("KYC approval notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send KYC approval notification for user {}: {}", userId, e.getMessage(), e);
        }

        createWallet(userId, "INR");
        return KycMapper.toKycResponse(kycRequest);
    }

    // 5. REJECT KYC (Admin only)
    @Override
    @Transactional
    public KycResponse rejectKyc(Long userId, String remarks, String role) {
        if (!role.equals(UserType.ADMIN.name())) {
            throw new DeniedAccessException("Access denied. Admin role required to reject KYC requests.");
        }

        KycRequest kycRequest = kycRequestRepository.findByUserId(userId)
                .orElseThrow(() -> new KycNotFoundException(userId));

        if (kycRequest.getStatus() != KycStatus.PENDING) {
            throw new KycStatusException(
                    "Only PENDING KYC requests can be rejected. Current status: " + kycRequest.getStatus());
        }

        if (remarks == null || remarks.trim().isEmpty()) {
            throw new KycStatusException("Rejection remarks are required. Please provide a reason.");
        }

        kycRequest.setStatus(KycStatus.REJECTED);
        kycRequest.setReviewedAt(LocalDateTime.now());
        kycRequest.setReviewRemarks(remarks);
        kycRequest = kycRequestRepository.save(kycRequest);
        KycResponse response = KycMapper.toKycResponse(kycRequest);

        try {
            String payload = NotificationPayloadBuilder.buildKycRejectedPayload("User " + userId, remarks);
            SendNotificationRequest notificationRequest = new SendNotificationRequest(
                    userId,
                    NotificationChannel.EMAIL,
                    KycNotificationEvent.KYC_REJECTED.getTemplateCode(),
                    payload
            );
            notificationServiceClient.sendAsync(notificationRequest);
            log.info("KYC rejection notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send KYC rejection notification for user {}: {}", userId, e.getMessage(), e);
        }

        return response;
    }

    // 6. UPDATE KYC (User only)
    @Override
    @Transactional
    public KycResponse updateKyc(Long userId, String role, KycUpdateRequest request, MultipartFile file) {
        if (!role.equals(UserType.USER.name())) {
            throw new DeniedAccessException("Access denied. User role required to update KYC requests.");
        }

        KycRequest kycRequest = kycRequestRepository.findByUserId(userId)
                .orElseThrow(() -> new KycNotFoundException(userId));

        if (kycRequest.getStatus() == KycStatus.APPROVED) {
            throw new KycStatusException(
                    "You can only update your KYC while it is PENDING or REJECTED. Current status: "
                            + kycRequest.getStatus());
        }

        KycStatus previousStatus = kycRequest.getStatus();

        validateUpdateRequest(request, file);

        // If the document number changed, ensure it's not already registered by another user.
        boolean documentNumberChanged = kycRequest.getDocuments().stream()
                .map(KycDocument::getDocumentNumber)
                .noneMatch(existing -> existing != null && existing.equals(request.getDocumentNumber()));
        if (documentNumberChanged) {
            checkDuplicateDocumentNumber(request.getDocumentNumber(), request.getVerificationType());
        }

        // Capture old file paths so we can clean them up after the DB commit succeeds.
        List<String> oldFilePaths = new ArrayList<>();
        for (KycDocument existing : kycRequest.getDocuments()) {
            if (existing.getFileReference() != null) {
                oldFilePaths.add(existing.getFileReference());
            }
        }

        // Persist the new file BEFORE wiping the old DB rows — that way a storage failure
        // doesn't leave the KYC with no document attached.
        String storedPath = fileStorageService.saveFile(file, userId);

        kycRequest.getDocuments().clear();
        kycDocumentRepository.deleteAllByKycRequestId(kycRequest.getId());

        saveKycDocument(kycRequest,
                KycSubmitRequest.builder()
                        .verificationType(request.getVerificationType())
                        .verifiedName(request.getVerifiedName())
                        .verifiedDob(request.getVerifiedDob())
                        .documentNumber(request.getDocumentNumber())
                        .build(),
                file,
                storedPath);

        kycRequest.setStatus(KycStatus.PENDING);
        kycRequest.setRequestType(
                previousStatus == KycStatus.PENDING ? RequestType.UPDATE_KYC : RequestType.RE_KYC);

        kycRequest = kycRequestRepository.save(kycRequest);
        KycResponse response = KycMapper.toKycResponse(kycRequest);

        // Best-effort cleanup of orphaned files now that DB is consistent.
        for (String oldPath : oldFilePaths) {
            fileStorageService.deleteFile(oldPath);
        }

        try {
            String payload = NotificationPayloadBuilder.buildKycUpdatedPayload("User " + userId, previousStatus.name());
            SendNotificationRequest notificationRequest = new SendNotificationRequest(
                    userId,
                    NotificationChannel.EMAIL,
                    KycNotificationEvent.KYC_UPDATED.getTemplateCode(),
                    payload
            );
            notificationServiceClient.sendAsync(notificationRequest);
            log.info("KYC update notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send KYC update notification for user {}: {}", userId, e.getMessage(), e);
        }

        return response;
    }

    // 7. RETRIEVE DOCUMENT FOR VIEWING (controller streams the bytes)
    @Override
    @Transactional(readOnly = true)
    public KycDocument getDocumentForView(Long documentId, String role, Long requesterUserId) {
        KycDocument doc = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new InvalidDocumentException("Document not found: " + documentId));

        Long ownerUserId = doc.getKycRequest().getUserId();
        if (UserType.USER.name().equals(role) && !ownerUserId.equals(requesterUserId)) {
            throw new DeniedAccessException("Access denied. Users can only view their own KYC documents.");
        }
        return doc;
    }

    // ─────────────────────────────── HELPERS ───────────────────────────────

    private void createWallet(Long userId, String currency) {
        try {
            walletServiceClient.createWallet(new CreateWalletRequest(userId, currency));
        } catch (Exception e) {
            log.warn("Failed to create wallet for user: {}", userId);
        }
    }

    private void validateNewKycRequest(KycSubmitRequest request, MultipartFile file) {
        if (request.getVerificationType() == null) {
            throw new InvalidDocumentException(
                    "Verification type is required. Choose either PAN_BASED or AADHAAR_BASED.");
        }
        if (request.getVerifiedName() == null || request.getVerifiedName().trim().isEmpty()) {
            throw new InvalidDocumentException("Verified name is required.");
        }
        if (request.getVerifiedDob() == null) {
            throw new InvalidDocumentException("Date of birth is required.");
        }
        if (request.getVerifiedDob().isAfter(LocalDate.now())) {
            throw new InvalidDocumentException("Date of birth cannot be in the future.");
        }
        validateDocumentNumberForType(request.getDocumentNumber(), request.getVerificationType());
        validateUploadedFile(file);
    }

    private void validateUpdateRequest(KycUpdateRequest request, MultipartFile file) {
        if (request.getVerificationType() == null) {
            throw new InvalidDocumentException(
                    "Verification type is required. Choose either PAN_BASED or AADHAAR_BASED.");
        }
        if (request.getVerifiedName() == null || request.getVerifiedName().trim().isEmpty()) {
            throw new InvalidDocumentException("Verified name is required.");
        }
        if (request.getVerifiedDob() == null) {
            throw new InvalidDocumentException("Date of birth is required.");
        }
        if (request.getVerifiedDob().isAfter(LocalDate.now())) {
            throw new InvalidDocumentException("Date of birth cannot be in the future.");
        }
        validateDocumentNumberForType(request.getDocumentNumber(), request.getVerificationType());
        validateUploadedFile(file);
    }

    private void validateUploadedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("Document file is required.");
        }
        String contentType = file.getContentType();
        // Allow common image types and PDF — anything else is likely a wrong upload.
        if (contentType == null
                || !(contentType.startsWith("image/") || contentType.equals("application/pdf"))) {
            throw new InvalidDocumentException(
                    "Unsupported document type: " + contentType
                            + ". Allowed types are JPEG/PNG images or PDF.");
        }
    }

    private void validateDocumentNumberForType(String documentNumber, KycVerificationType type) {
        if (documentNumber == null || documentNumber.trim().isEmpty()) {
            throw new InvalidDocumentException("Document number is required.");
        }
        String number = documentNumber.trim();
        if (type == KycVerificationType.PAN_BASED) {
            validatePanNumber(number);
        } else if (type == KycVerificationType.AADHAAR_BASED) {
            validateAadhaarNumber(number);
        } else {
            throw new InvalidDocumentException("Invalid verification type: " + type);
        }
    }

    private void validateAadhaarNumber(String number) {
        if (!number.matches(AADHAAR_REGEX)) {
            throw new InvalidDocumentException(
                    "Invalid Aadhaar number. Must be exactly 12 digits. Provided: " + number);
        }
    }

    private void validatePanNumber(String number) {
        if (!number.matches(PAN_REGEX)) {
            throw new InvalidDocumentException(
                    "Invalid PAN format. Must be 10 alphanumeric characters. Provided: " + number);
        }
        int alphabetCount = 0;
        for (int i = 0; i < number.length(); i++) {
            if (Character.isLetter(number.charAt(i))) {
                alphabetCount++;
            }
        }
        if (alphabetCount < 4) {
            throw new InvalidDocumentException(
                    "Invalid PAN number. Must have at least 4 alphabets. Found: " + alphabetCount);
        }
    }

    private void checkDuplicateDocumentNumber(String documentNumber, KycVerificationType type) {
        if (kycDocumentRepository.existsByDocumentNumber(documentNumber)) {
            throw new InvalidDocumentException(
                    "This " + type.name() + " ('" + documentNumber + "') is already registered with another wallet. " +
                            "Each person can have only ONE wallet. For support, contact admin.");
        }
        log.debug("Document number {} is unique and available for registration", documentNumber);
    }

    private void saveKycDocument(KycRequest kycRequest,
                                 KycSubmitRequest request,
                                 MultipartFile file,
                                 String storedPath) {
        KycDocument doc = new KycDocument();
        doc.setKycRequest(kycRequest);
        doc.setVerificationType(request.getVerificationType());
        doc.setVerifiedName(request.getVerifiedName());
        doc.setVerifiedDob(request.getVerifiedDob());
        doc.setDocumentNumber(request.getDocumentNumber());
        // Mirror the verification type onto the legacy document_type column so it's
        // never NULL for new submissions (AADHAAR_BASED → AADHAAR, PAN_BASED → PAN).
        doc.setDocumentType(toLegacyDocumentType(request.getVerificationType()));
        doc.setFileName(file.getOriginalFilename());
        doc.setFileReference(storedPath);
        doc.setDocumentMimeType(file.getContentType());
        doc.setUploadedAt(LocalDateTime.now());

        KycDocument savedDoc = kycDocumentRepository.save(doc);
        kycRequest.getDocuments().add(savedDoc);
    }

    /**
     * Maps {@link KycVerificationType} → legacy {@link DocumentType} so the
     * {@code document_type} column carries the document identity (AADHAAR / PAN)
     * rather than being null.
     */
    private DocumentType toLegacyDocumentType(KycVerificationType verificationType) {
        return switch (verificationType) {
            case AADHAAR_BASED -> DocumentType.AADHAAR;
            case PAN_BASED     -> DocumentType.PAN;
        };
    }
}