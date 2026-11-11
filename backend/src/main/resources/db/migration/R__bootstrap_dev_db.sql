-- =======================================================
-- OpenShelves Schema (Development)
-- =======================================================


-- -------------------------------------------------------
-- Clean Schema (keeps Flyway's history table)
-- -------------------------------------------------------
DO $$
DECLARE
    r record;
BEGIN
    -- Tables (CASCADE also takes dependent views, FKs and owned sequences)
    FOR r IN
        SELECT c.relname FROM pg_class c
        WHERE c.relnamespace = 'public'::regnamespace
          AND c.relkind IN ('r', 'p')
          AND c.relname <> 'flyway_schema_history'
          AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = c.oid AND d.deptype = 'e')
        LOOP
            EXECUTE format('DROP TABLE IF EXISTS public.%I CASCADE', r.relname);
        END LOOP;

    -- Remaining views, materialized views, sequences
    FOR r IN
        SELECT c.relname, c.relkind FROM pg_class c
        WHERE c.relnamespace = 'public'::regnamespace
          AND c.relkind IN ('v', 'm', 'S')
          AND c.relname NOT LIKE 'flyway_schema_history%'
          AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = c.oid AND d.deptype = 'e')
        LOOP
            EXECUTE format('DROP %s IF EXISTS public.%I CASCADE',
                           CASE r.relkind WHEN 'v' THEN 'VIEW' WHEN 'm' THEN 'MATERIALIZED VIEW' ELSE 'SEQUENCE' END,
                           r.relname);
        END LOOP;

    -- Functions and procedures
    FOR r IN
        SELECT p.oid::regprocedure AS sig FROM pg_proc p
        WHERE p.pronamespace = 'public'::regnamespace
          AND p.prokind IN ('f', 'p')
          AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = p.oid AND d.deptype = 'e')
        LOOP
            EXECUTE format('DROP ROUTINE IF EXISTS %s CASCADE', r.sig);
        END LOOP;

    -- Enums, domains, standalone composite types
    FOR r IN
        SELECT t.typname FROM pg_type t
        WHERE t.typnamespace = 'public'::regnamespace
          AND (t.typtype IN ('e', 'd')
            OR (t.typtype = 'c' AND (SELECT relkind FROM pg_class WHERE oid = t.typrelid) = 'c'))
          AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = t.oid AND d.deptype = 'e')
        LOOP
            EXECUTE format('DROP TYPE IF EXISTS public.%I CASCADE', r.typname);
        END LOOP;
END $$;


-- -------------------------------------------------------
-- Author Roles: lookup table for author roles
-- -------------------------------------------------------
CREATE TABLE lu_author_roles (
    -- key
    code                VARCHAR(100)     NOT NULL,

    -- descriptor
    label               VARCHAR(100)     NOT NULL,
    description         TEXT,

    -- constraints
    CONSTRAINT lu_author_roles_pk       PRIMARY KEY (code),
    CONSTRAINT lu_author_roles_label_uk UNIQUE (label)
);

INSERT INTO lu_author_roles (code, label, description) VALUES
    ('AUTHOR',       'Author',       'Primary writer of the work'),
    ('CO_AUTHOR',    'Co-Author',    'Joint primary writer of the work'),
    ('EDITOR',       'Editor',       'Responsible for editing and curating the content'),
    ('TRANSLATOR',   'Translator',   'Translated the work into another language'),
    ('ILLUSTRATOR',  'Illustrator',  'Created illustrations or artwork for the work'),
    ('PHOTOGRAPHER', 'Photographer', 'Provided photographs for the work'),
    ('FOREWORD',     'Foreword',     'Wrote the foreword'),
    ('INTRODUCTION', 'Introduction', 'Wrote the introduction'),
    ('PREFACE',      'Preface',      'Wrote the preface'),
    ('AFTERWORD',    'Afterword',    'Wrote the afterword'),
    ('CONTRIBUTOR',  'Contributor',  'Made a general contribution to the work'),
    ('COMPILER',     'Compiler',     'Compiled or assembled the work'),
    ('NARRATOR',     'Narrator',     'Narrated the audiobook version of the work');


-- -------------------------------------------------------
-- Auth Provider Types: lookup table for SSO protocols
-- -------------------------------------------------------
CREATE TABLE lu_auth_provider_types (
    -- key
    code                VARCHAR(100)     NOT NULL,

    -- descriptor
    label               VARCHAR(100)     NOT NULL,
    description         TEXT,

    -- constraints
    CONSTRAINT lu_auth_provider_types_pk       PRIMARY KEY (code),
    CONSTRAINT lu_auth_provider_types_label_uk UNIQUE (label)
);

INSERT INTO lu_auth_provider_types (code, label, description) VALUES
    ('SAML',   'SAML',   'SAML 2.0 identity provider, e.g. Shibboleth'),
    ('OIDC',   'OIDC',   'OpenID Connect identity provider, e.g. Google');


-- -------------------------------------------------------
-- Books: the catalog record
-- -------------------------------------------------------
CREATE TABLE books (
    -- identity
    id                  BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,

    -- core metadata
    title               TEXT            NOT NULL,
    subtitle            TEXT,
    description         TEXT,
    language            VARCHAR(3),                            -- ISO language code
    page_count          INTEGER,

    -- classification
    dewey_decimal       VARCHAR(20),
    lc_classification   VARCHAR(50),

    -- publisher
    publisher           TEXT,
    publication_year    SMALLINT,

    -- series
    series_name         TEXT,
    series_number       INTEGER,

    -- identifiers
    isbn_10             VARCHAR(10),
    isbn_13             VARCHAR(13),
    asin                VARCHAR(10),
    olid                VARCHAR(15),

    -- media
    cover_image_url     TEXT,

    -- audit
    created_at          TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ       NOT NULL DEFAULT now(),

    -- constraints
    CONSTRAINT books_pk                 PRIMARY KEY (id),
    CONSTRAINT books_isbn_10_uk         UNIQUE (isbn_10),
    CONSTRAINT books_isbn_13_uk         UNIQUE (isbn_13),
    CONSTRAINT books_asin_uk            UNIQUE (asin),
    CONSTRAINT books_olid_uk            UNIQUE (olid),
    CONSTRAINT books_publication_year_chk
        CHECK (publication_year BETWEEN 1000 AND EXTRACT(YEAR FROM CURRENT_DATE) + 1)   -- Publishers often print the next year on books released late in the year
);


-- -------------------------------------------------------
-- Authors: people who wrote the books
-- -------------------------------------------------------
CREATE TABLE authors (
    -- identity
    id                  BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,

    -- core metadata
    name                TEXT            NOT NULL,
    bio                 TEXT,
    nationality         VARCHAR(100),

    -- dates
    birth_year          SMALLINT,
    death_year          SMALLINT,

    -- identifiers
    asin                VARCHAR(10),
    olid                VARCHAR(15),

    -- media
    profile_image_url   TEXT,

    -- audit
    created_at          TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ       NOT NULL DEFAULT now(),

    -- constraints
    CONSTRAINT authors_pk               PRIMARY KEY (id),
    CONSTRAINT authors_asin_uk          UNIQUE (asin),
    CONSTRAINT authors_olid_uk          UNIQUE (olid),
    CONSTRAINT authors_birth_year_chk
        CHECK (birth_year <= EXTRACT(YEAR FROM CURRENT_DATE)),
    CONSTRAINT authors_death_year_chk
        CHECK (death_year <= EXTRACT(YEAR FROM CURRENT_DATE)),
    CONSTRAINT authors_lifespan_chk
        CHECK (death_year IS NULL OR death_year >= birth_year)
);


-- -------------------------------------------------------
-- Book Authors: many-to-many between books and authors
-- -------------------------------------------------------
CREATE TABLE book_authors (
    -- identity
    id                  BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,

    -- relationships
    book_id             BIGINT          NOT NULL,
    author_id           BIGINT          NOT NULL,

    -- additional metadata
    role                VARCHAR(100)     NOT NULL DEFAULT 'AUTHOR',
    sort_order          SMALLINT        NOT NULL DEFAULT 1,

    -- constraints
    CONSTRAINT book_authors_pk          PRIMARY KEY (id),
    CONSTRAINT book_authors_uk          UNIQUE (book_id, author_id, role),
    CONSTRAINT book_authors_book_fk
        FOREIGN KEY (book_id)           REFERENCES books           (id) ON DELETE CASCADE,
    CONSTRAINT book_authors_author_fk
        FOREIGN KEY (author_id)         REFERENCES authors         (id) ON DELETE RESTRICT,
    CONSTRAINT book_authors_role_fk
        FOREIGN KEY (role)              REFERENCES lu_author_roles (code) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT book_authors_sort_order_chk
        CHECK (sort_order >= 1)
);

CREATE INDEX book_authors_book_role_idx    ON book_authors (book_id, role, sort_order);
CREATE INDEX book_authors_author_idx       ON book_authors (author_id);


-- -------------------------------------------------------
-- Users: core identity and profile
-- -------------------------------------------------------
CREATE TABLE users (
    -- identity
    id                  BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,

    -- core metadata
    display_name        TEXT            NOT NULL,
    bio                 TEXT,

    department          TEXT,
    program             TEXT,

    institutional_id    TEXT,

    -- contact
    email               TEXT,
    phone_number        TEXT,

    -- localization
    language            VARCHAR(3),                            -- ISO language code
    timezone            VARCHAR(50),

    -- status
    is_active           BOOLEAN         NOT NULL DEFAULT true,

    -- media
    avatar_image_url    TEXT,

    -- audit
    created_at          TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ       NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,

    -- constraints
    CONSTRAINT users_pk                 PRIMARY KEY (id)
);

CREATE UNIQUE INDEX users_email_idx     ON users (lower(email));
CREATE UNIQUE INDEX users_institutional_id_idx ON users (institutional_id);


-- -------------------------------------------------------
-- Auth Providers: SSO identity providers, managed by library admins
-- -------------------------------------------------------
CREATE TABLE auth_providers (
    -- identity
    id                      BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,

    -- core metadata
    key                     TEXT            NOT NULL,              -- URL label, e.g. /sso/{key}/start
    type                    VARCHAR(100)    NOT NULL,
    display_name            TEXT            NOT NULL,

    -- sign-in page
    enabled                 BOOLEAN         NOT NULL DEFAULT true,
    sort_order              INT             NOT NULL DEFAULT 0,

    -- provider identity
    issuer                  TEXT            NOT NULL,              -- OIDC iss / SAML entityID

    -- configuration
    settings                JSONB           NOT NULL DEFAULT '{}', -- client ID, IdP metadata, attribute mapping, SAML SP certificate
    secret                  TEXT,                                  -- encrypted: OIDC client secret or SAML SP private key; key comes from the deployment environment

    -- audit
    created_at              TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ       NOT NULL DEFAULT now(),

    -- constraints
    CONSTRAINT auth_providers_pk        PRIMARY KEY (id),
    CONSTRAINT auth_providers_key_uk    UNIQUE (key),
    CONSTRAINT auth_providers_issuer_uk UNIQUE (issuer),
    CONSTRAINT auth_providers_type_fk
        FOREIGN KEY (type)              REFERENCES lu_auth_provider_types (code) ON UPDATE CASCADE ON DELETE RESTRICT
);


-- -------------------------------------------------------
-- User Passwords: the password a user signs in with (at most one)
-- -------------------------------------------------------
CREATE TABLE user_passwords (
    -- identity
    id                      BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,

    -- primary relationship
    user_id                 BIGINT          NOT NULL,

    -- secret
    password_hash           TEXT            NOT NULL,
    password_changed_at     TIMESTAMPTZ,

    -- lockout
    failed_attempts         INT             NOT NULL DEFAULT 0,
    locked_until            TIMESTAMPTZ,

    -- usage
    last_used_at            TIMESTAMPTZ,

    -- audit
    created_at              TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ       NOT NULL DEFAULT now(),

    -- constraints
    CONSTRAINT user_passwords_pk        PRIMARY KEY (id),
    CONSTRAINT user_passwords_user_uk   UNIQUE (user_id),
    CONSTRAINT user_passwords_user_fk
        FOREIGN KEY (user_id)           REFERENCES users (id) ON DELETE CASCADE
);


-- -------------------------------------------------------
-- User Identities: SSO accounts a user has connected
-- -------------------------------------------------------
-- Matched to auth_providers by issuer, deliberately without a foreign key,
-- so deleting and re-adding a provider keeps its users linked.
CREATE TABLE user_identities (
    -- identity
    id                      BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,

    -- primary relationship
    user_id                 BIGINT          NOT NULL,

    -- identity at the provider
    issuer                  TEXT            NOT NULL,              -- OIDC iss / SAML entityID
    subject                 TEXT            NOT NULL,              -- OIDC sub / SAML NameID

    -- metadata
    metadata                JSONB,                                 -- display hints, e.g. email or name at the provider

    -- usage
    last_used_at            TIMESTAMPTZ,

    -- audit
    created_at              TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ       NOT NULL DEFAULT now(),

    -- constraints
    CONSTRAINT user_identities_pk       PRIMARY KEY (id),
    CONSTRAINT user_identities_uk       UNIQUE (issuer, subject),
    CONSTRAINT user_identities_user_fk
        FOREIGN KEY (user_id)           REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX user_identities_user_idx      ON user_identities (user_id);


-- -------------------------------------------------------
-- User Sessions: active logins
-- -------------------------------------------------------
CREATE TABLE user_sessions (
    -- identity
    id                      BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,

    -- primary relationship
    user_id                 BIGINT          NOT NULL,

    -- tokens (SHA-256 hashed)
    session_token_hash      TEXT            NOT NULL,

    -- metadata
    ip_address              TEXT,
    user_agent              TEXT,
    device_name             TEXT,
    device_location         TEXT,
    last_active_at          TIMESTAMPTZ       NOT NULL DEFAULT now(),

    -- how it was opened (snapshot, no FK)
    auth_provider           TEXT,                                  -- provider display name; null = password

    -- timeouts
    is_persistent           BOOLEAN           NOT NULL DEFAULT false,  -- "keep me signed in"
    expires_at              TIMESTAMPTZ       NOT NULL,
    revoked_at              TIMESTAMPTZ,

    -- audit
    created_at              TIMESTAMPTZ       NOT NULL DEFAULT now(),

    -- constraints
    CONSTRAINT user_sessions_pk      PRIMARY KEY (id),
    CONSTRAINT user_sessions_user_fk
        FOREIGN KEY (user_id)           REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX user_sessions_user_idx                ON user_sessions (user_id);
CREATE UNIQUE INDEX user_sessions_token_hash_idx   ON user_sessions (session_token_hash);


-- -------------------------------------------------------
-- SSO Codes: short-lived, single-use codes for SSO round-trips
-- -------------------------------------------------------
-- LOGIN: bridges the SSO callback and POST /sso/exchange.
-- LINK:  starts connecting a provider from settings.
-- Rows are deleted when used; expired ones are purged.
CREATE TABLE sso_codes (
    -- identity
    id                      BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,

    -- primary relationship
    user_id                 BIGINT          NOT NULL,
    provider_key            TEXT            NOT NULL,

    -- code (SHA-256 hashed)
    purpose                 VARCHAR(10)     NOT NULL,
    code_hash               TEXT            NOT NULL,

    -- LOGIN only
    is_persistent           BOOLEAN,
    code_challenge          TEXT,                                  -- PKCE S256: exchange must present the matching verifier

    -- timeouts
    expires_at              TIMESTAMPTZ       NOT NULL,

    -- audit
    created_at              TIMESTAMPTZ       NOT NULL DEFAULT now(),

    -- constraints
    CONSTRAINT sso_codes_pk          PRIMARY KEY (id),
    CONSTRAINT sso_codes_user_fk
        FOREIGN KEY (user_id)           REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT sso_codes_provider_fk
        FOREIGN KEY (provider_key)      REFERENCES auth_providers (key) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT sso_codes_purpose_chk
        CHECK (purpose IN ('LOGIN', 'LINK')),
    CONSTRAINT sso_codes_login_fields_chk
        CHECK ((purpose = 'LOGIN') = (is_persistent IS NOT NULL AND code_challenge IS NOT NULL))
);

CREATE UNIQUE INDEX sso_codes_code_hash_idx        ON sso_codes (code_hash);
CREATE INDEX sso_codes_expires_idx                 ON sso_codes (expires_at);


-- -------------------------------------------------------
-- Password Reset Tokens: emailed links to set a new password
-- -------------------------------------------------------
-- Covers forgot password and first-time setup (there's no sign-up).
-- Single use: deleted when used, and replaced by any newer request for the same user.
CREATE TABLE password_reset_tokens (
    -- identity
    id                      BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,

    -- primary relationship
    user_id                 BIGINT          NOT NULL,

    -- token (SHA-256 hashed)
    token_hash              TEXT            NOT NULL,

    -- timeouts
    expires_at              TIMESTAMPTZ       NOT NULL,

    -- audit
    created_at              TIMESTAMPTZ       NOT NULL DEFAULT now(),

    -- constraints
    CONSTRAINT password_reset_tokens_pk      PRIMARY KEY (id),
    CONSTRAINT password_reset_tokens_user_fk
        FOREIGN KEY (user_id)           REFERENCES users (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX password_reset_tokens_token_hash_idx   ON password_reset_tokens (token_hash);
CREATE INDEX password_reset_tokens_user_idx                ON password_reset_tokens (user_id);





-- =======================================================
-- Seed Data for Development
-- =======================================================

-- Sample data for local development. Never to be run against production.


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
