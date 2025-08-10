-- Add authentication fields to users table
ALTER TABLE users
    ADD COLUMN password VARCHAR(255) NOT NULL DEFAULT '$2a$10$default_hash_for_migration',
    ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'USER',
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN account_non_expired BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN account_non_locked BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN credentials_non_expired BOOLEAN NOT NULL DEFAULT true; 