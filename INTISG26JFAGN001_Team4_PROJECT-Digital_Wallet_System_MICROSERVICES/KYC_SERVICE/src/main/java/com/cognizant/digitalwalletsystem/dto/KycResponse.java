package com.cognizant.digitalwalletsystem.dto;

import com.cognizant.digitalwalletsystem.entity.enums.KycStatus;
import com.cognizant.digitalwalletsystem.entity.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
//This DTO returns the KYC Response after submission of KYC
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycResponse {

    private Long id;
    private Long userId;
    private KycStatus status;
    private RequestType requestType;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewRemarks;
    private List<KycDocumentResponse> documents;
}
