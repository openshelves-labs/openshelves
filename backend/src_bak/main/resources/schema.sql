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
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now(),

    -- constraints
    CONSTRAINT books_pk                 PRIMARY KEY (id),
    CONSTRAINT books_isbn_10_uk         UNIQUE (isbn_10),
    CONSTRAINT books_isbn_13_uk         UNIQUE (isbn_13),
    CONSTRAINT books_asin_uk            UNIQUE (asin),
    CONSTRAINT books_olid_uk            UNIQUE (olid),
    CONSTRAINT books_publication_year_chk
        CHECK (publication_year BETWEEN 1000 AND EXTRACT(YEAR FROM CURRENT_DATE))
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
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now(),

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
-- Metadata Fields: lookup table for book/author fields
-- -------------------------------------------------------
CREATE TABLE lu_metadata_fields (
    -- key
    code                VARCHAR(100)     NOT NULL,

    -- descriptor
    label               VARCHAR(100)    NOT NULL,
    description         TEXT,

    -- constraints
    CONSTRAINT lu_metadata_fields_pk       PRIMARY KEY (code),
    CONSTRAINT lu_metadata_fields_label_uk UNIQUE (label)
);

INSERT INTO lu_metadata_fields (code, label, description) VALUES
    ('BOOK_TITLE',              'Book Title',           'The primary title of the book'),
    ('BOOK_SUBTITLE',           'Book Subtitle',        'The secondary title or subtitle of the book'),
    ('BOOK_DESCRIPTION',        'Book Description',     'A description or summary of the book content'),
    ('BOOK_LANGUAGE',           'Book Language',        'The language code of the book (e.g., en)'),
    ('BOOK_PAGE_COUNT',         'Book Page Count',      'The total number of pages in the book'),
    ('BOOK_DEWEY_DECIMAL',      'Dewey Decimal',        'The Dewey Decimal Classification code'),
    ('BOOK_LC_CLASSIFICATION',  'LC Classification',    'The Library of Congress classification code'),
    ('BOOK_PUBLISHER',          'Book Publisher',       'The name of the publishing entity'),
    ('BOOK_PUBLICATION_YEAR',   'Publication Year',     'The year the book was published'),
    ('BOOK_SERIES_NAME',        'Series Name',          'The name of the series the book belongs to'),
    ('BOOK_SERIES_NUMBER',      'Series Number',        'The position of the book within a series'),
    ('BOOK_ISBN10',             'ISBN-10',              'The 10-digit International Standard Book Number'),
    ('BOOK_ISBN13',             'ISBN-13',              'The 13-digit International Standard Book Number'),
    ('BOOK_ASIN',               'Amazon ASIN',          'Amazon Standard Identification Number for the book'),
    ('BOOK_OLID',               'Open Library ID',      'Open Library identifier for the book'),
    ('BOOK_COVER_IMAGE_URL',    'Cover Image URL',      'The URL of the book cover image'),
    ('BOOK_AUTHORS',            'Book Authors',         'The list of contributors associated with the book'),
    ('AUTHOR_NAME',             'Author Name',          'The full display name of the author'),
    ('AUTHOR_BIO',              'Author Biography',     'A short biography or description of the author'),
    ('AUTHOR_NATIONALITY',      'Author Nationality',   'The nationality of the author'),
    ('AUTHOR_BIRTH_YEAR',       'Birth Year',           'The year the author was born'),
    ('AUTHOR_DEATH_YEAR',       'Death Year',           'The year the author died'),
    ('AUTHOR_ASIN',             'Author ASIN',          'Amazon identifier for the author'),
    ('AUTHOR_OLID',             'Author OLID',          'Open Library identifier for the author'),
    ('AUTHOR_PROFILE_IMAGE_URL','Profile Image URL',    'The URL of the author profile image');


-- -------------------------------------------------------
-- Metadata Providers: lookup table for external sources
-- -------------------------------------------------------
CREATE TABLE lu_metadata_providers (
    -- key
    code                VARCHAR(100)     NOT NULL,

    -- descriptor
    label               VARCHAR(100)     NOT NULL,
    description         TEXT,

    -- constraints
    CONSTRAINT lu_metadata_providers_pk       PRIMARY KEY (code),
    CONSTRAINT lu_metadata_providers_label_uk UNIQUE (label)
);

INSERT INTO lu_metadata_providers (code, label, description) VALUES
    ('GOOGLE_BOOKS', 'Google Books', 'Google Books API'),
    ('OPEN_LIBRARY', 'Open Library', 'Open Library API'),
    ('AMAZON',       'Amazon',       'Amazon Books service'),
    ('GOODREADS',    'Goodreads',    'Goodreads social cataloging');


-- -------------------------------------------------------
-- Resolution Strategies: lookup table for conflict resolution
-- -------------------------------------------------------
CREATE TABLE lu_resolution_strategies (
    -- key
    code                VARCHAR(100)     NOT NULL,

    -- descriptor
    label               VARCHAR(100)     NOT NULL,
    description         TEXT,

    -- constraints
    CONSTRAINT lu_resolution_strategies_pk       PRIMARY KEY (code),
    CONSTRAINT lu_resolution_strategies_label_uk UNIQUE (label)
);

INSERT INTO lu_resolution_strategies (code, label, description) VALUES
    ('PRIORITY',             'Priority',             'Select the value from the provider with the highest defined priority'),
    ('FIRST_NON_NULL',       'First Non-Null',       'Select the first non-null value encountered among providers'),
    ('HIGHEST_QUALITY_TEXT', 'Highest Quality Text', 'Select the value with the highest quality text'),
    ('VOTING',               'Voting',               'Select the value most commonly supplied by all providers'),
    ('AI_SYNTHESIZE',        'AI Synthesize',        'Use an AI model to synthesize a consolidated value from all provider inputs'),
    ('MANUAL',               'Manual',               'Mark the field for manual resolution by a human moderator');


-- -------------------------------------------------------
-- Field Resolution Policies: rules for merging metadata
-- -------------------------------------------------------
CREATE TABLE field_resolution_policies (
    -- identity
    id                  BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,

    -- configuration
    field_key           VARCHAR(100)     NOT NULL,
    resolution_strategy VARCHAR(100)     NOT NULL,
    ranked_providers   VARCHAR(100)[],

    -- constraints
    CONSTRAINT field_resolution_policies_pk      PRIMARY KEY (id),
    CONSTRAINT field_resolution_policies_uk      UNIQUE (field_key),
    CONSTRAINT field_resolution_policies_field_fk
        FOREIGN KEY (field_key)           REFERENCES lu_metadata_fields (code) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT field_resolution_policies_strategy_fk
        FOREIGN KEY (resolution_strategy) REFERENCES lu_resolution_strategies (code) ON UPDATE CASCADE ON DELETE RESTRICT
);


-- -------------------------------------------------------
-- Integrity: Referential Integrity for ranked_providers Array
-- -------------------------------------------------------

-- 1. Validation Trigger: Ensures all providers in a policy exist in the lookup table
CREATE OR REPLACE FUNCTION check_ranked_providers_validity()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.ranked_providers IS NOT NULL AND array_length(NEW.ranked_providers, 1) > 0 THEN
        IF EXISTS (
            SELECT 1
            FROM unnest(NEW.ranked_providers) AS p_code
            WHERE p_code NOT IN (SELECT code FROM lu_metadata_providers)
        ) THEN
            RAISE EXCEPTION 'Invalid provider code in ranked_providers: some values do not exist in lu_metadata_providers';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_ranked_providers
BEFORE INSERT OR UPDATE ON field_resolution_policies
FOR EACH ROW
EXECUTE FUNCTION check_ranked_providers_validity();


-- 2. Cascade Trigger: Syncs changes from lu_metadata_providers to policies
CREATE OR REPLACE FUNCTION handle_metadata_provider_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.code <> NEW.code THEN
        -- Cascade code changes: replace old code with new code in all priority arrays
        UPDATE field_resolution_policies
        SET ranked_providers = array_replace(ranked_providers, OLD.code, NEW.code)
        WHERE OLD.code = ANY(ranked_providers);
    ELSIF TG_OP = 'DELETE' THEN
        -- Prevent "silent" deletion: raise an exception if the provider is still in use
        IF EXISTS (
            SELECT 1
            FROM field_resolution_policies
            WHERE OLD.code = ANY(ranked_providers)
        ) THEN
            RAISE EXCEPTION 'Cannot delete provider %: it is currently referenced in one or more field resolution policies. Remove it from the policies first.', OLD.code;
        END IF;
    END IF;
    -- Note: Since this is an AFTER trigger, we return NULL
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_handle_metadata_provider_changes
AFTER UPDATE OR DELETE ON lu_metadata_providers
FOR EACH ROW
EXECUTE FUNCTION handle_metadata_provider_changes();
