PRAGMA writable_schema = 1;
UPDATE sqlite_master SET sql = REPLACE(sql, '''verses''', '`verses`') WHERE name = 'verses_fts';
UPDATE sqlite_master SET name = 'index_verses_book_chapter_verse', sql = REPLACE(sql, 'idx_verses_book_ch_vs', 'index_verses_book_chapter_verse') WHERE name = 'idx_verses_book_ch_vs';
PRAGMA writable_schema = 0;
