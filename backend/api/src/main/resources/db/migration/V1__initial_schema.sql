-- 최초 스키마 (회원 + 계정)
-- db 수정 사항이 생기면 v2, v3, ... 등으로 파일을 만들어서 버전 관리함

CREATE TABLE users (
    id                 INTEGER       NOT NULL AUTO_INCREMENT,

    nickname           VARCHAR(20)  NOT NULL,
    apt_name           VARCHAR(100) NOT NULL,
    dong               VARCHAR(20)  DEFAULT NULL,
    ho                 VARCHAR(20)  DEFAULT NULL,

    role               ENUM('ADMIN', 'USER')       NOT NULL,
    status             ENUM('ACTIVE', 'WITHDRAWN') NOT NULL,

    withdrawn_at       DATETIME(6)  DEFAULT NULL,
    withdrawal_reason  VARCHAR(255) DEFAULT NULL,

    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_users_nickname UNIQUE (nickname)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE accounts (
    id                  INTEGER       NOT NULL AUTO_INCREMENT,
    user_id             INTEGER       NOT NULL,

    email               VARCHAR(100) NOT NULL,
    password            VARCHAR(83)  DEFAULT NULL,

    refresh_token_hash  VARCHAR(64)  DEFAULT NULL,
    expiry_date         DATETIME(6)  DEFAULT NULL,

    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_accounts_email UNIQUE (email),
    CONSTRAINT uk_accounts_user_id UNIQUE (user_id),
    CONSTRAINT uk_accounts_refresh_token_hash UNIQUE (refresh_token_hash),
    CONSTRAINT fk_accounts_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
