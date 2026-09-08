DROP INDEX idx_verses_book_ch_vs;
CREATE UNIQUE INDEX index_verses_book_chapter_verse ON verses(book, chapter, verse);
DROP TABLE verses_fts;
CREATE VIRTUAL TABLE verses_fts USING fts4(originalText, modernizedText, comparativeText, content=`verses`);
INSERT INTO verses_fts(verses_fts) VALUES('rebuild');
