-- ============================================================
-- KYC Service — MySQL Schema
-- Database : kyc_db
-- Run this manually once, or let Hibernate ddl-auto handle it.
-- ============================================================

CREATE DATABASE IF NOT EXISTS kyc_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE kyc_db;

-- ============================================================
-- Table : kyc_requests
-- ============================================================
CREATE TABLE IF NOT EXISTS kyc_requests (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',   -- PENDING | APPROVED | REJECTED
    request_type    VARCHAR(30)     NOT NULL,                      -- NEW_KYC | UPDATE_KYC | RE_KYC
    submitted_at    DATETIME        NOT NULL,
    reviewed_at     DATETIME        NULL,
    review_remarks  VARCHAR(500)    NULL,

    CONSTRAINT pk_kyc_requests      PRIMARY KEY (id),
    CONSTRAINT uq_kyc_user_id       UNIQUE (user_id),             -- one KYC per user
    CONSTRAINT chk_kyc_status       CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT chk_request_type     CHECK (request_type IN ('NEW_KYC','UPDATE_KYC','RE_KYC'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Table : kyc_documents
-- ============================================================
CREATE TABLE IF NOT EXISTS kyc_documents (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    kyc_request_id  BIGINT          NOT NULL,
    document_type   VARCHAR(20)     NOT NULL,                      -- ID_PROOF | ADDRESS_PROOF
    file_name       VARCHAR(255)    NOT NULL,
    file_reference  VARCHAR(500)    NOT NULL,
    uploaded_at     DATETIME        NOT NULL,

    CONSTRAINT pk_kyc_documents         PRIMARY KEY (id),
    CONSTRAINT fk_doc_kyc_request       FOREIGN KEY (kyc_request_id)
                                            REFERENCES kyc_requests(id)
                                            ON DELETE CASCADE,
    CONSTRAINT chk_document_type        CHECK (document_type IN ('ID_PROOF','ADDRESS_PROOF')),
    -- one document type per KYC request (e.g. only one ID_PROOF per request)
    CONSTRAINT uq_doc_type_per_request  UNIQUE (kyc_request_id, document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Indexes for common query patterns
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_kyc_status       ON kyc_requests (status);
CREATE INDEX IF NOT EXISTS idx_kyc_submitted_at ON kyc_requests (submitted_at);
CREATE INDEX IF NOT EXISTS idx_doc_request_id   ON kyc_documents (kyc_request_id);
