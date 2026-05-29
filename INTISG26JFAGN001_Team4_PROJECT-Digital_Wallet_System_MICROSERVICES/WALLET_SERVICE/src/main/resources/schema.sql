CREATE DATABASE IF NOT EXISTS wallet_db;
USE wallet_db;

CREATE TABLE IF NOT EXISTS wallets
(
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    user_id           BIGINT         NOT NULL,
    currency          VARCHAR(10)    NOT NULL DEFAULT 'INR',
    available_balance DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    held_balance      DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    status            VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at        DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)             DEFAULT NULL
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT uk_wallet_user_id
        UNIQUE (user_id),

    CONSTRAINT chk_wallet_status
        CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),

    CONSTRAINT chk_available_balance
        CHECK (available_balance >= 0),

    CONSTRAINT chk_held_balance
        CHECK (held_balance >= 0),

    INDEX idx_wallet_status (status)
)
AUTO_INCREMENT = 1000000000;