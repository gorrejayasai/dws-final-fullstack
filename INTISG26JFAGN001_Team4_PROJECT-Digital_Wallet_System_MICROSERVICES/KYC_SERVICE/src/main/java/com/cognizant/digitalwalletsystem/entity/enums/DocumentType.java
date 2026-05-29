package com.cognizant.digitalwalletsystem.entity.enums;

public enum DocumentType {

    // Current flow: each KYC submission carries exactly one identity document,
    // and the type below mirrors the chosen verification method
    // (see KycVerificationType). KycServiceImpl#saveKycDocument fills this in
    // automatically so the column is never NULL for fresh submissions.
    AADHAAR,        // populated when verificationType == AADHAAR_BASED
    PAN,            // populated when verificationType == PAN_BASED


    ID_PROOF,       // Aadhaar / PAN card (old multi-doc flow)
    ADDRESS_PROOF   // Utility bill, Passport, etc. (old multi-doc flow)
}
