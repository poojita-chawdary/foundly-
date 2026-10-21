-- V2: Seed initial administrator account.
-- Email: polurupoojithachowdary@gmail.com
-- Temporary password: Foundly2026 (BCrypt $2a$10)
-- Role: ADMIN, Membership: VERIFIED

INSERT INTO users (name, email, password_hash, role, campus_id, membership, suspended, created_at, updated_at) VALUES
('Poluru Poojita', 'polurupoojithachowdary@gmail.com', '$2a$10$REynuBVflnTRksNuvNL.LOTus1MFJUr4Q3fPv9ZV1HBxDxuzdhWxi', 'ADMIN', 1, 'VERIFIED', false, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
