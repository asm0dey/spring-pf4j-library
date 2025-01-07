CREATE TABLE IF NOT EXISTS author
(
    id          bigserial PRIMARY KEY,
    fb2id       text,
    first_name  text,
    middle_name text,
    last_name   text,
    nickname    text,
    added       timestamptz DEFAULT NOW() NOT NULL,
    full_name   text                      NOT NULL UNIQUE GENERATED ALWAYS AS (
        TRIM(
                CASE
                    WHEN last_name IS NOT NULL AND last_name <> '' THEN
                        last_name ||
                        CASE
                            WHEN (first_name IS NOT NULL AND first_name <> '') OR
                                 (middle_name IS NOT NULL AND middle_name <> '') THEN ', '
                            ELSE ''
                            END ||
                        TRIM(COALESCE(first_name, '') || ' ' || COALESCE(middle_name, '') ||
                             CASE
                                 WHEN nickname IS NOT NULL AND nickname <> '' THEN ' (' || nickname || ')'
                                 ELSE ''
                                 END)
                    ELSE
                        TRIM(COALESCE(first_name, '') || ' ' || COALESCE(middle_name, '') ||
                             CASE
                                 WHEN nickname IS NOT NULL AND nickname <> '' THEN nickname
                                 ELSE ''
                                 END)
                    END
        )
        ) STORED
);

CREATE TABLE IF NOT EXISTS book
(
    id              bigserial PRIMARY KEY,
    path            text                      NOT NULL,
    name            text                      NOT NULL,
    date            text,
    added           timestamptz DEFAULT NOW() NOT NULL,
    sequence        text,
    sequence_number int8,
    lang            text,
    zip_file        text
);

CREATE TABLE IF NOT EXISTS genre
(
    id   bigserial PRIMARY KEY,
    name text NOT NULL UNIQUE
);

CREATE TABLE book_author
(
    book_id   bigint
        REFERENCES book (id) ON DELETE CASCADE,
    author_id bigint
        REFERENCES author (id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, author_id)
);

CREATE TABLE book_genre
(
    book_id  bigint
        REFERENCES book (id) ON DELETE CASCADE,
    genre_id bigint
        REFERENCES genre (id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, genre_id)
);

-- Indices for author full_name filtering
CREATE INDEX IF NOT EXISTS idx_author_full_name_first_letter ON author (LOWER(SUBSTRING(full_name FROM 1 FOR 1)));
CREATE INDEX IF NOT EXISTS idx_author_full_name_first_two_letters ON author (LOWER(SUBSTRING(full_name FROM 1 FOR 2)));
CREATE INDEX IF NOT EXISTS idx_author_full_name_first_three_letters ON author (LOWER(SUBSTRING(full_name FROM 1 FOR 3)));
CREATE INDEX IF NOT EXISTS idx_author_full_name_first_four_letters ON author (LOWER(SUBSTRING(full_name FROM 1 FOR 4)));
CREATE INDEX IF NOT EXISTS idx_author_full_name_first_five_letters ON author (LOWER(SUBSTRING(full_name FROM 1 FOR 5)));

-- Indices for book sequence filtering
CREATE INDEX IF NOT EXISTS idx_book_sequence_first_letter ON book (LOWER(SUBSTRING(sequence FROM 1 FOR 1)));
CREATE INDEX IF NOT EXISTS idx_book_sequence_first_two_letters ON book (LOWER(SUBSTRING(sequence FROM 1 FOR 2)));
CREATE INDEX IF NOT EXISTS idx_book_sequence_first_three_letters ON book (LOWER(SUBSTRING(sequence FROM 1 FOR 3)));
CREATE INDEX IF NOT EXISTS idx_book_sequence_first_four_letters ON book (LOWER(SUBSTRING(sequence FROM 1 FOR 4)));
CREATE INDEX IF NOT EXISTS idx_book_sequence_first_five_letters ON book (LOWER(SUBSTRING(sequence FROM 1 FOR 5)));

CREATE OR REPLACE FUNCTION find_author_by_names(
    i_first_name text DEFAULT NULL,
    i_middle_name text DEFAULT NULL,
    i_last_name text DEFAULT NULL,
    i_nickname text DEFAULT NULL
)
    RETURNS TABLE
            (
                id bigint
            )
AS
$$
BEGIN
    RETURN QUERY
        SELECT author.id
        FROM author
        WHERE TRIM(
                      CASE
                          WHEN i_last_name IS NOT NULL AND i_last_name <> '' THEN
                              i_last_name ||
                              CASE
                                  WHEN (i_first_name IS NOT NULL AND i_first_name <> '') OR
                                       (i_middle_name IS NOT NULL AND i_middle_name <> '') THEN ', '
                                  ELSE ''
                                  END ||
                              TRIM(COALESCE(i_first_name, '') || ' ' || COALESCE(i_middle_name, '') ||
                                   CASE
                                       WHEN i_nickname IS NOT NULL AND i_nickname <> '' THEN ' (' || i_nickname || ')'
                                       ELSE ''
                                       END)
                          ELSE
                              TRIM(COALESCE(i_first_name, '') || ' ' || COALESCE(i_middle_name, '') ||
                                   CASE
                                       WHEN i_nickname IS NOT NULL AND i_nickname <> '' THEN i_nickname
                                       ELSE ''
                                       END)
                          END) = full_name;
END;
$$ LANGUAGE plpgsql;