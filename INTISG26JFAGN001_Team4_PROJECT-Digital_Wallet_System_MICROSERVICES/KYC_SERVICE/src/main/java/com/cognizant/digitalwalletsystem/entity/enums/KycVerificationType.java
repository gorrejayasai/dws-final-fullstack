package com.cognizant.digitalwalletsystem.entity.enums;

/**
 * KYC Verification Type - User must choose one of two verification methods
 *
 * PAN_BASED: KYC verification using PAN (Permanent Account Number)
 * AADHAAR_BASED: KYC verification using Aadhaar (12-digit ID number)
 *
 * Each user submission requires exactly ONE verification type with corresponding document upload
 */
public enum KycVerificationType {
    PAN_BASED,      // Verification using PAN document
    AADHAAR_BASED   // Verification using Aadhaar document
}