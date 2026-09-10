CREATE TABLE member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(191) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_email UNIQUE (email),
    CONSTRAINT uk_member_provider UNIQUE (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
