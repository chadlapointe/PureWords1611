DB_PATH="/data/data/com.purewords1611.android/databases/pure_words_study.db"
sqlite3 $DB_PATH <<EOF
PRAGMA foreign_keys=OFF;

-- Fix verses index
DROP INDEX IF EXISTS idx_verses_book_ch_vs;
CREATE UNIQUE INDEX IF NOT EXISTS index_verses_book_chapter_verse ON verses(book, chapter, verse);

-- Recreate marginal_notes
DROP TABLE IF EXISTS marginal_notes;
CREATE TABLE marginal_notes (
    id INTEGER NOT NULL,
    verseId INTEGER NOT NULL,
    noteType TEXT NOT NULL,
    note TEXT NOT NULL,
    anchorToken TEXT,
    sourceId TEXT NOT NULL,
    sourceLocator TEXT NOT NULL,
    checksumSha256 TEXT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY(verseId) REFERENCES verses(id) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX index_marginal_notes_verseId ON marginal_notes (verseId);

-- Recreate bookmarks
DROP TABLE IF EXISTS bookmarks;
CREATE TABLE bookmarks (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    verseId INTEGER NOT NULL,
    createdAtEpochMillis INTEGER NOT NULL
);

-- Recreate highlights
DROP TABLE IF EXISTS highlights;
CREATE TABLE highlights (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    verseId INTEGER NOT NULL,
    colorName TEXT NOT NULL,
    createdAtEpochMillis INTEGER NOT NULL
);

-- Recreate personal_notes
DROP TABLE IF EXISTS personal_notes;
CREATE TABLE personal_notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    verseId INTEGER NOT NULL,
    note TEXT NOT NULL,
    updatedAtEpochMillis INTEGER NOT NULL
);

-- Recreate explanations
DROP TABLE IF EXISTS explanations;
CREATE TABLE explanations (
    id TEXT NOT NULL,
    verseId INTEGER NOT NULL,
    level TEXT NOT NULL,
    contentMarkdown TEXT NOT NULL,
    sourceId TEXT NOT NULL,
    checksumSha256 TEXT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY(verseId) REFERENCES verses(id) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX index_explanations_verseId ON explanations (verseId);
CREATE INDEX index_explanations_level ON explanations (level);

-- Recreate reading_preferences
DROP TABLE IF EXISTS reading_preferences;
CREATE TABLE reading_preferences (
    id INTEGER NOT NULL,
    explanationLevel TEXT NOT NULL,
    contentVersion INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY(id)
);
INSERT OR IGNORE INTO reading_preferences (id, explanationLevel, contentVersion) VALUES (1, 'BASIC', 0);

-- Recreate verses_fts with correct quoting
DROP TABLE IF EXISTS verses_fts;
CREATE VIRTUAL TABLE verses_fts USING fts4(originalText, modernizedText, comparativeText, content=\`verses\`);
INSERT INTO verses_fts(verses_fts) VALUES('rebuild');

PRAGMA user_version = 13;
EOF
