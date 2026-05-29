-- ============================================================
-- DWS TEST SEED DATA
-- Run each block on its own database in this order:
--   1. auth_service_db_dev
--   2. kyc_db
--   3. wallet_db
--   4. transaction_db
--
-- All 10 users share password: Test@1234
-- Existing users: admin(1), sai(2), dinesh(3), test1(4)
-- New users will get IDs: alice=5, bob=6, charlie=7, diana=8,
--   evan=9, fiona=10, george=11, hannah=12, ivan=13, julia=14
-- ============================================================

-- ════════════════════════════════════════════════════════════
-- 1. DATABASE: auth_service_db_dev
-- ════════════════════════════════════════════════════════════
USE auth_service_db_dev;

-- Password for ALL users: Test@1234
-- BCrypt hash (strength 10): $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh52
INSERT INTO user (username, email, password, role, status, created_at, updated_at) VALUES
('alice',   'alice@test.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh52', 'USER', 'ACTIVE', NOW(), NOW()),
('bob',     'bob@test.com',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh52', 'USER', 'ACTIVE', NOW(), NOW()),
('charlie', 'charlie@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh52', 'USER', 'ACTIVE', NOW(), NOW()),
('diana',   'diana@test.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh52', 'USER', 'ACTIVE', NOW(), NOW()),
('evan',    'evan@test.com',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh52', 'USER', 'ACTIVE', NOW(), NOW()),
('fiona',   'fiona@test.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh52', 'USER', 'ACTIVE', NOW(), NOW()),
('george',  'george@test.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh52', 'USER', 'ACTIVE', NOW(), NOW()),
('hannah',  'hannah@test.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh52', 'USER', 'ACTIVE', NOW(), NOW()),
('ivan',    'ivan@test.com',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh52', 'USER', 'ACTIVE', NOW(), NOW()),
('julia',   'julia@test.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh52', 'USER', 'ACTIVE', NOW(), NOW());

-- Verify IDs
SELECT id, username FROM user WHERE username IN ('alice','bob','charlie','diana','evan','fiona','george','hannah','ivan','julia');


-- ════════════════════════════════════════════════════════════
-- 2. DATABASE: kyc_db
-- user_id: alice=5, bob=6, charlie=7, diana=8, evan=9,
--          fiona=10, george=11, hannah=12, ivan=13, julia=14
-- ════════════════════════════════════════════════════════════
USE kyc_db;

INSERT INTO kyc_request (user_id, status, remarks, submitted_at, reviewed_at, created_at, updated_at) VALUES
(5,  'APPROVED', 'All documents verified', NOW(), NOW(), NOW(), NOW()),
(6,  'APPROVED', 'All documents verified', NOW(), NOW(), NOW(), NOW()),
(7,  'APPROVED', 'All documents verified', NOW(), NOW(), NOW(), NOW()),
(8,  'APPROVED', 'All documents verified', NOW(), NOW(), NOW(), NOW()),
(9,  'APPROVED', 'All documents verified', NOW(), NOW(), NOW(), NOW()),
(10, 'APPROVED', 'All documents verified', NOW(), NOW(), NOW(), NOW()),
(11, 'APPROVED', 'All documents verified', NOW(), NOW(), NOW(), NOW()),
(12, 'APPROVED', 'All documents verified', NOW(), NOW(), NOW(), NOW()),
(13, 'APPROVED', 'All documents verified', NOW(), NOW(), NOW(), NOW()),
(14, 'APPROVED', 'All documents verified', NOW(), NOW(), NOW(), NOW());

-- Check what kyc_request IDs were assigned before inserting documents
-- Run: SELECT id, user_id FROM kyc_request ORDER BY id DESC LIMIT 10;
-- Then replace kyc_request_id values below if needed.
-- Assumption: new kyc_request IDs start from 1 (fresh kyc_db)
-- If kyc_db already has rows, adjust the ids accordingly.

INSERT INTO kyc_document (kyc_request_id, document_type, document_number, file_reference, created_at, updated_at) VALUES
-- alice (kyc_request id=1)
(1, 'AADHAAR', '123456789012', 'user_5/dummy_aadhaar.jpg',  NOW(), NOW()),
(1, 'PAN',     'ABCDE1234F',   'user_5/dummy_pan.jpg',     NOW(), NOW()),
-- bob (kyc_request id=2)
(2, 'AADHAAR', '234567890123', 'user_6/dummy_aadhaar.jpg',  NOW(), NOW()),
(2, 'PAN',     'BCDEF2345G',   'user_6/dummy_pan.jpg',     NOW(), NOW()),
-- charlie
(3, 'AADHAAR', '345678901234', 'user_7/dummy_aadhaar.jpg',  NOW(), NOW()),
(3, 'PAN',     'CDEFG3456H',   'user_7/dummy_pan.jpg',     NOW(), NOW()),
-- diana
(4, 'AADHAAR', '456789012345', 'user_8/dummy_aadhaar.jpg',  NOW(), NOW()),
(4, 'PAN',     'DEFGH4567I',   'user_8/dummy_pan.jpg',     NOW(), NOW()),
-- evan
(5, 'AADHAAR', '567890123456', 'user_9/dummy_aadhaar.jpg',  NOW(), NOW()),
(5, 'PAN',     'EFGHI5678J',   'user_9/dummy_pan.jpg',     NOW(), NOW()),
-- fiona
(6, 'AADHAAR', '678901234567', 'user_10/dummy_aadhaar.jpg', NOW(), NOW()),
(6, 'PAN',     'FGHIJ6789K',   'user_10/dummy_pan.jpg',    NOW(), NOW()),
-- george
(7, 'AADHAAR', '789012345678', 'user_11/dummy_aadhaar.jpg', NOW(), NOW()),
(7, 'PAN',     'GHIJK7890L',   'user_11/dummy_pan.jpg',    NOW(), NOW()),
-- hannah
(8, 'AADHAAR', '890123456789', 'user_12/dummy_aadhaar.jpg', NOW(), NOW()),
(8, 'PAN',     'HIJKL8901M',   'user_12/dummy_pan.jpg',    NOW(), NOW()),
-- ivan
(9, 'AADHAAR', '901234567890', 'user_13/dummy_aadhaar.jpg', NOW(), NOW()),
(9, 'PAN',     'IJKLM9012N',   'user_13/dummy_pan.jpg',    NOW(), NOW()),
-- julia
(10,'AADHAAR', '012345678901', 'user_14/dummy_aadhaar.jpg', NOW(), NOW()),
(10,'PAN',     'JKLMN0123O',   'user_14/dummy_pan.jpg',    NOW(), NOW());


-- ════════════════════════════════════════════════════════════
-- 3. DATABASE: wallet_db
-- user_id: alice=5, bob=6, charlie=7, diana=8, evan=9,
--          fiona=10, george=11, hannah=12, ivan=13, julia=14
-- wallet IDs will be: alice=1..10 (fresh db) — adjust if needed
-- ════════════════════════════════════════════════════════════
USE wallet_db;

INSERT INTO wallet (user_id, currency, available_balance, held_balance, status, created_at, updated_at) VALUES
(5,  'INR', 850000.0000, 0.0000, 'ACTIVE', NOW(), NOW()),  -- alice  → wallet id=1
(6,  'INR', 620000.0000, 0.0000, 'ACTIVE', NOW(), NOW()),  -- bob    → wallet id=2
(7,  'INR', 430000.0000, 0.0000, 'ACTIVE', NOW(), NOW()),  -- charlie→ wallet id=3
(8,  'INR', 990000.0000, 0.0000, 'ACTIVE', NOW(), NOW()),  -- diana  → wallet id=4
(9,  'INR', 175000.0000, 0.0000, 'ACTIVE', NOW(), NOW()),  -- evan   → wallet id=5
(10, 'INR', 310000.0000, 0.0000, 'ACTIVE', NOW(), NOW()),  -- fiona  → wallet id=6
(11, 'INR', 540000.0000, 0.0000, 'ACTIVE', NOW(), NOW()),  -- george → wallet id=7
(12, 'INR', 760000.0000, 0.0000, 'ACTIVE', NOW(), NOW()),  -- hannah → wallet id=8
(13, 'INR', 220000.0000, 0.0000, 'ACTIVE', NOW(), NOW()),  -- ivan   → wallet id=9
(14, 'INR', 480000.0000, 0.0000, 'ACTIVE', NOW(), NOW());  -- julia  → wallet id=10


-- ════════════════════════════════════════════════════════════
-- 4. DATABASE: transaction_db
-- user_id:   alice=5,  bob=6,  charlie=7, diana=8,  evan=9
--            fiona=10, george=11, hannah=12, ivan=13, julia=14
-- wallet_id: alice=1,  bob=2,  charlie=3, diana=4,  evan=5
--            fiona=6,  george=7,  hannah=8,  ivan=9,  julia=10
-- ════════════════════════════════════════════════════════════
USE transaction_db;

-- ─── TOPUP transactions ───────────────────────────────────
INSERT INTO transactions (transaction_id, username, wallet_id, target_wallet_id, target_username, user_id, target_user_id, type, amount, currency, status, idempotency_key, metadata, created_at) VALUES
('txn-alice-top01',   'alice',   1, NULL, NULL, 5,  NULL, 'TOPUP', 50000.0000, 'INR', 'COMPLETED', 'idem-alice-top01',   NULL, NOW()),
('txn-bob-top01',     'bob',     2, NULL, NULL, 6,  NULL, 'TOPUP', 40000.0000, 'INR', 'COMPLETED', 'idem-bob-top01',     NULL, NOW()),
('txn-charlie-top01', 'charlie', 3, NULL, NULL, 7,  NULL, 'TOPUP', 30000.0000, 'INR', 'COMPLETED', 'idem-charlie-top01', NULL, NOW()),
('txn-diana-top01',   'diana',   4, NULL, NULL, 8,  NULL, 'TOPUP', 50000.0000, 'INR', 'COMPLETED', 'idem-diana-top01',   NULL, NOW()),
('txn-evan-top01',    'evan',    5, NULL, NULL, 9,  NULL, 'TOPUP', 25000.0000, 'INR', 'COMPLETED', 'idem-evan-top01',    NULL, NOW()),
('txn-fiona-top01',   'fiona',   6, NULL, NULL, 10, NULL, 'TOPUP', 35000.0000, 'INR', 'COMPLETED', 'idem-fiona-top01',   NULL, NOW()),
('txn-george-top01',  'george',  7, NULL, NULL, 11, NULL, 'TOPUP', 45000.0000, 'INR', 'COMPLETED', 'idem-george-top01',  NULL, NOW()),
('txn-hannah-top01',  'hannah',  8, NULL, NULL, 12, NULL, 'TOPUP', 50000.0000, 'INR', 'COMPLETED', 'idem-hannah-top01',  NULL, NOW()),
('txn-ivan-top01',    'ivan',    9, NULL, NULL, 13, NULL, 'TOPUP', 20000.0000, 'INR', 'COMPLETED', 'idem-ivan-top01',    NULL, NOW()),
('txn-julia-top01',   'julia',  10, NULL, NULL, 14, NULL, 'TOPUP', 30000.0000, 'INR', 'COMPLETED', 'idem-julia-top01',   NULL, NOW());

-- ─── TRANSFER transactions ────────────────────────────────
INSERT INTO transactions (transaction_id, username, wallet_id, target_wallet_id, target_username, user_id, target_user_id, type, amount, currency, status, idempotency_key, metadata, created_at) VALUES
('txn-alice-trf01',  'alice',   1, 2, 'bob',     5,  6,  'TRANSFER', 15000.0000, 'INR', 'COMPLETED', 'idem-alice-trf01',  NULL, NOW()),
('txn-bob-trf01',    'bob',     2, 3, 'charlie', 6,  7,  'TRANSFER', 10000.0000, 'INR', 'COMPLETED', 'idem-bob-trf01',    NULL, NOW()),
('txn-diana-trf01',  'diana',   4, 5, 'evan',    8,  9,  'TRANSFER', 20000.0000, 'INR', 'COMPLETED', 'idem-diana-trf01',  NULL, NOW()),
('txn-george-trf01', 'george',  7, 8, 'hannah',  11, 12, 'TRANSFER', 25000.0000, 'INR', 'COMPLETED', 'idem-george-trf01', NULL, NOW()),
('txn-julia-trf01',  'julia',  10, 1, 'alice',   14, 5,  'TRANSFER', 12000.0000, 'INR', 'COMPLETED', 'idem-julia-trf01',  NULL, NOW()),
('txn-fiona-trf01',  'fiona',   6, 7, 'george',  10, 11, 'TRANSFER',  8000.0000, 'INR', 'COMPLETED', 'idem-fiona-trf01',  NULL, NOW());

-- ─── WITHDRAW transactions ────────────────────────────────
INSERT INTO transactions (transaction_id, username, wallet_id, target_wallet_id, target_username, user_id, target_user_id, type, amount, currency, status, idempotency_key, metadata, created_at) VALUES
('txn-alice-wdr01',   'alice',   1, NULL, NULL, 5,  NULL, 'WITHDRAW', 10000.0000, 'INR', 'COMPLETED', 'idem-alice-wdr01',   NULL, NOW()),
('txn-charlie-wdr01', 'charlie', 3, NULL, NULL, 7,  NULL, 'WITHDRAW',  5000.0000, 'INR', 'COMPLETED', 'idem-charlie-wdr01', NULL, NOW()),
('txn-ivan-wdr01',    'ivan',    9, NULL, NULL, 13, NULL, 'WITHDRAW',  8000.0000, 'INR', 'COMPLETED', 'idem-ivan-wdr01',    NULL, NOW());

-- ─── Ledger entries ───────────────────────────────────────
-- TOPUP: 1 CREDIT each
INSERT INTO ledger_entries (transaction_id, wallet_id, entry_type, amount, created_at) VALUES
('txn-alice-top01',   1, 'CREDIT', 50000.0000, NOW()),
('txn-bob-top01',     2, 'CREDIT', 40000.0000, NOW()),
('txn-charlie-top01', 3, 'CREDIT', 30000.0000, NOW()),
('txn-diana-top01',   4, 'CREDIT', 50000.0000, NOW()),
('txn-evan-top01',    5, 'CREDIT', 25000.0000, NOW()),
('txn-fiona-top01',   6, 'CREDIT', 35000.0000, NOW()),
('txn-george-top01',  7, 'CREDIT', 45000.0000, NOW()),
('txn-hannah-top01',  8, 'CREDIT', 50000.0000, NOW()),
('txn-ivan-top01',    9, 'CREDIT', 20000.0000, NOW()),
('txn-julia-top01',  10, 'CREDIT', 30000.0000, NOW());

-- TRANSFER: DEBIT sender + CREDIT receiver
INSERT INTO ledger_entries (transaction_id, wallet_id, entry_type, amount, created_at) VALUES
('txn-alice-trf01',  1, 'DEBIT',  15000.0000, NOW()),
('txn-alice-trf01',  2, 'CREDIT', 15000.0000, NOW()),
('txn-bob-trf01',    2, 'DEBIT',  10000.0000, NOW()),
('txn-bob-trf01',    3, 'CREDIT', 10000.0000, NOW()),
('txn-diana-trf01',  4, 'DEBIT',  20000.0000, NOW()),
('txn-diana-trf01',  5, 'CREDIT', 20000.0000, NOW()),
('txn-george-trf01', 7, 'DEBIT',  25000.0000, NOW()),
('txn-george-trf01', 8, 'CREDIT', 25000.0000, NOW()),
('txn-julia-trf01', 10, 'DEBIT',  12000.0000, NOW()),
('txn-julia-trf01',  1, 'CREDIT', 12000.0000, NOW()),
('txn-fiona-trf01',  6, 'DEBIT',   8000.0000, NOW()),
('txn-fiona-trf01',  7, 'CREDIT',  8000.0000, NOW());

-- WITHDRAW: DEBIT each
INSERT INTO ledger_entries (transaction_id, wallet_id, entry_type, amount, created_at) VALUES
('txn-alice-wdr01',   1, 'DEBIT', 10000.0000, NOW()),
('txn-charlie-wdr01', 3, 'DEBIT',  5000.0000, NOW()),
('txn-ivan-wdr01',    9, 'DEBIT',  8000.0000, NOW());
