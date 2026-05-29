package com.cognizant.digitalwalletsystem.config;

import com.cognizant.digitalwalletsystem.entity.KycDocument;
import com.cognizant.digitalwalletsystem.entity.KycRequest;
import com.cognizant.digitalwalletsystem.entity.enums.*;
import com.cognizant.digitalwalletsystem.repository.KycDocumentRepository;
import com.cognizant.digitalwalletsystem.repository.KycRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class TestKycSeeder implements CommandLineRunner {

    private final KycRequestRepository kycRequestRepository;
    private final KycDocumentRepository kycDocumentRepository;

    // username → { aadhaar, pan, dob }
    private static final List<Object[]> SEED_DATA = List.of(
            new Object[]{"alice",   "123456789012", "ABCDE1234F", LocalDate.of(1995, 3, 12)},
            new Object[]{"bob",     "234567890123", "BCDEF2345G", LocalDate.of(1993, 7, 25)},
            new Object[]{"charlie", "345678901234", "CDEFG3456H", LocalDate.of(1990, 11, 5)},
            new Object[]{"diana",   "456789012345", "DEFGH4567I", LocalDate.of(1997, 1, 18)},
            new Object[]{"evan",    "567890123456", "EFGHI5678J", LocalDate.of(1992, 6, 30)},
            new Object[]{"fiona",   "678901234567", "FGHIJ6789K", LocalDate.of(1996, 9, 14)},
            new Object[]{"george",  "789012345678", "GHIJK7890L", LocalDate.of(1988, 4, 22)},
            new Object[]{"hannah",  "890123456789", "HIJKL8901M", LocalDate.of(1994, 8, 3)},
            new Object[]{"ivan",    "901234567890", "IJKLM9012N", LocalDate.of(1991, 2, 17)},
            new Object[]{"julia",   "012345678901", "JKLMN0123O", LocalDate.of(1998, 12, 9)}
    );

    @Override
    public void run(String... args) {
        RestTemplate restTemplate = new RestTemplate();

        for (Object[] row : SEED_DATA) {
            String username = (String) row[0];
            String aadhaar  = (String) row[1];
            String pan      = (String) row[2];
            LocalDate dob   = (LocalDate) row[3];

            try {
                // Look up userId from UserService via internal endpoint
                String url = "http://localhost:8082/user/internal/by-username/" + username;
                Map<?, ?> response = restTemplate.getForObject(url, Map.class);
                if (response == null || response.get("userId") == null) {
                    log.warn("User not found in UserService: {}, skipping KYC seed", username);
                    continue;
                }
                Long userId = Long.valueOf(response.get("userId").toString());

                if (kycRequestRepository.existsByUserId(userId)) {
                    log.info("KYC already exists for user: {}, skipping", username);
                    continue;
                }

                // Create KYC request — APPROVED
                KycRequest kyc = KycRequest.builder()
                        .userId(userId)
                        .status(KycStatus.APPROVED)
                        .requestType(RequestType.NEW_KYC)
                        .submittedAt(LocalDateTime.now())
                        .reviewedAt(LocalDateTime.now())
                        .reviewRemarks("Auto-seeded for testing")
                        .build();
                kyc = kycRequestRepository.save(kyc);

                // Aadhaar document
                KycDocument aadhaarDoc = KycDocument.builder()
                        .kycRequest(kyc)
                        .verificationType(KycVerificationType.AADHAAR_BASED)
                        .verifiedName(username)
                        .verifiedDob(dob)
                        .documentNumber(aadhaar)
                        .documentType(DocumentType.ID_PROOF)
                        .fileName("aadhaar.jpg")
                        .fileReference("user_" + userId + "/dummy_aadhaar.jpg")
                        .documentMimeType("image/jpeg")
                        .uploadedAt(LocalDateTime.now())
                        .build();

                // PAN document
                KycDocument panDoc = KycDocument.builder()
                        .kycRequest(kyc)
                        .verificationType(KycVerificationType.PAN_BASED)
                        .verifiedName(username)
                        .verifiedDob(dob)
                        .documentNumber(pan)
                        .documentType(DocumentType.ID_PROOF)
                        .fileName("pan.jpg")
                        .fileReference("user_" + userId + "/dummy_pan.jpg")
                        .documentMimeType("image/jpeg")
                        .uploadedAt(LocalDateTime.now())
                        .build();

                kycDocumentRepository.save(aadhaarDoc);
                kycDocumentRepository.save(panDoc);

                log.info("KYC seeded (APPROVED) for user: {} (userId={})", username, userId);

            } catch (Exception e) {
                log.warn("Could not seed KYC for user {}: {}", username, e.getMessage());
            }
        }
    }
}
