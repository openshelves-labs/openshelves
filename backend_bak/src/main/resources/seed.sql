-- =======================================================
-- OpenShelves Seed Data
-- =======================================================
-- Sample data for local development. Run after schema.sql.
-- Never run against production.


-- -------------------------------------------------------
-- Sample User
-- -------------------------------------------------------
-- Sign in with `admin@openshelves.local` or `ADMIN001`, password `admin`.
-- The hash carries the `{bcrypt}` prefix the DelegatingPasswordEncoder needs.

WITH sample_user AS (
    INSERT INTO users (display_name, email, institutional_id, department, language, timezone)
    VALUES ('Admin', 'admin@openshelves.local', 'ADMIN001', 'Library Services', 'en', 'Asia/Kolkata')
    RETURNING id
)
INSERT INTO user_passwords (user_id, password_hash)
SELECT id, '{bcrypt}$2a$10$vVW7vinVLdMacWhhaTEFpuN8funosORsqrVD119id6KZWhHKRLOGK'
FROM sample_user;
