-- OpenShelves Schema (v1.1)

-- -------------------------------------------------------
-- Clean Schema
-- -------------------------------------------------------
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;


-- -------------------------------------------------------
-- author_roles: lookup table for author roles
-- -------------------------------------------------------
CREATE TABLE author_roles (
    -- key
    code                VARCHAR(20)     NOT NULL,

    -- descriptor
    label               VARCHAR(50)     NOT NULL,
    description         TEXT,

    -- constraints
    CONSTRAINT author_roles_pk       PRIMARY KEY (code),
    CONSTRAINT author_roles_label_uk UNIQUE (label)
);

INSERT INTO author_roles (code, label, description) VALUES
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
-- books: the catalog record
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
-- authors: people who wrote the books
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
-- book_authors: many-to-many between books and authors
-- -------------------------------------------------------
CREATE TABLE book_authors (
    -- identity
    id                  BIGINT          NOT NULL GENERATED ALWAYS AS IDENTITY,

    -- relationships
    book_id             BIGINT          NOT NULL,
    author_id           BIGINT          NOT NULL,

    -- additional metadata
    role                VARCHAR(20)     NOT NULL DEFAULT 'AUTHOR',
    sort_order          SMALLINT        NOT NULL DEFAULT 1,

    -- constraints
    CONSTRAINT book_authors_pk          PRIMARY KEY (id),
    CONSTRAINT book_authors_uk          UNIQUE (book_id, author_id, role),
    CONSTRAINT book_authors_book_fk
        FOREIGN KEY (book_id)           REFERENCES books           (id) ON DELETE CASCADE,
    CONSTRAINT book_authors_author_fk
        FOREIGN KEY (author_id)         REFERENCES authors         (id) ON DELETE RESTRICT,
    CONSTRAINT book_authors_role_fk
        FOREIGN KEY (role)              REFERENCES author_roles (code),
    CONSTRAINT book_authors_sort_order_chk
        CHECK (sort_order >= 1)
);

CREATE INDEX book_authors_book_idx      ON book_authors (book_id);
CREATE INDEX book_authors_author_idx    ON book_authors (author_id);
CREATE INDEX book_authors_role_idx      ON book_authors (role);
