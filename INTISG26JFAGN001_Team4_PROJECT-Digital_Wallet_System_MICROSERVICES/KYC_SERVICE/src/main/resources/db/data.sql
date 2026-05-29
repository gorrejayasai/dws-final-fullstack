-- ============================================================
-- KYC Service — Sample / Seed Data  (DEV only)
-- Run after schema.sql
-- ============================================================

USE kyc_db;

-- Sample KYC request — PENDING
INSERT INTO kyc_requests (user_id, status, request_type, submitted_at)
VALUES (1001, 'PENDING', 'NEW_KYC', NOW())
ON DUPLICATE KEY UPDATE status = status;

-- Sample KYC request — APPROVED
INSERT INTO kyc_requests (user_id, status, request_type, submitted_at, reviewed_at, review_remarks)
VALUES (1002, 'APPROVED', 'NEW_KYC', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 'All documents verified successfully.')
ON DUPLICATE KEY UPDATE status = status;

-- Sample KYC request — REJECTED
INSERT INTO kyc_requests (user_id, status, request_type, submitted_at, reviewed_at, review_remarks)
VALUES (1003, 'REJECTED', 'NEW_KYC', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 'Aadhaar image is blurry. Please resubmit.')
ON DUPLICATE KEY UPDATE status = status;

-- Documents for user 1001 (PENDING)
INSERT INTO kyc_documents (kyc_request_id, document_type, file_name, file_reference, uploaded_at)
SELECT id, 'ID_PROOF', 'aadhaar_1001.jpg', 'uploads/1001/aadhaar_1001.jpg', NOW()
FROM kyc_requests WHERE user_id = 1001
ON DUPLICATE KEY UPDATE file_name = file_name;

-- Documents for user 1002 (APPROVED — 2 docs)
INSERT INTO kyc_documents (kyc_request_id, document_type, file_name, file_reference, uploaded_at)
SELECT id, 'ID_PROOF', 'pan_1002.jpg', 'uploads/1002/pan_1002.jpg', DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM kyc_requests WHERE user_id = 1002
ON DUPLICATE KEY UPDATE file_name = file_name;

INSERT INTO kyc_documents (kyc_request_id, document_type, file_name, file_reference, uploaded_at)
SELECT id, 'ADDRESS_PROOF', 'utility_bill_1002.pdf', 'uploads/1002/utility_bill_1002.pdf', DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM kyc_requests WHERE user_id = 1002
ON DUPLICATE KEY UPDATE file_name = file_name;

-- Documents for user 1003 (REJECTED)
INSERT INTO kyc_documents (kyc_request_id, document_type, file_name, file_reference, uploaded_at)
SELECT id, 'ID_PROOF', 'aadhaar_1003_blurry.jpg', 'uploads/1003/aadhaar_1003_blurry.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM kyc_requests WHERE user_id = 1003
ON DUPLICATE KEY UPDATE file_name = file_name;
