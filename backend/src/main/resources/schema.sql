-- =======================================================
-- OpenShelves Schema (v1.1)
-- =======================================================


-- -------------------------------------------------------
-- Clean Schema
-- -------------------------------------------------------
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;


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
-- User Credential Types: lookup table
-- -------------------------------------------------------
CREATE TABLE lu_user_credential_types (
    -- key
    code                VARCHAR(100)     NOT NULL,

    -- descriptor
    label               VARCHAR(100)     NOT NULL,
    description         TEXT,

    -- constraints
    CONSTRAINT lu_user_credential_types_pk       PRIMARY KEY (code),
    CONSTRAINT lu_user_credential_types_label_uk UNIQUE (label)
);

INSERT INTO lu_user_credential_types (code, label, description) VALUES
    ('PASSWORD',   'Password',   'Standard Id-Password Login');


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


-- -------------------------------------------------------
-- User Credentials: how a user proves who they are
-- -------------------------------------------------------
CREATE TABLE user_credentials (
    -- identity
    id                      BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,

    -- primary relationship
    user_id                 BIGINT          NOT NULL,
    type                    VARCHAR(100)    NOT NULL,

    -- metadata
    metadata                JSONB,

    -- secrets
    password_hash           TEXT,
    provider_key            TEXT,

    -- other details
    failed_attempts         INT             NOT NULL DEFAULT 0,
    locked_until            TIMESTAMPTZ,
    last_used_at            TIMESTAMPTZ,
    password_changed_at     TIMESTAMPTZ,

    -- audit
    created_at              TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ       NOT NULL DEFAULT now(),

    -- constraints
    CONSTRAINT user_credentials_pk      PRIMARY KEY (id),
    CONSTRAINT user_credentials_user_fk
        FOREIGN KEY (user_id)           REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT user_credentials_type_fk
        FOREIGN KEY (type)              REFERENCES lu_user_credential_types (code) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT user_credentials_password_chk
        CHECK (type != 'PASSWORD' OR password_hash IS NOT NULL)
);

CREATE INDEX user_credentials_user_idx      ON user_credentials (user_id);
CREATE UNIQUE INDEX user_credentials_password_idx ON user_credentials (user_id) WHERE type = 'PASSWORD';


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

    -- timeouts
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
