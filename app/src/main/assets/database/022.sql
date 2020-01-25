--

DROP TRIGGER entry_fts__after_insert__feed_tag;

--

DROP TRIGGER entry_fts__after_delete__feed_tag;

--

DROP TRIGGER entry_fts__after_insert__entry_tag;

--

DROP TRIGGER entry_fts__after_delete__entry_tag;

--

DROP TRIGGER entry_fts__after_insert__entry;

--

DROP INDEX entry_tag_index__entry_id__tag_id;

--

ALTER TABLE entry_tag RENAME TO entry_tag_old;

--

INSERT INTO tag (name) SELECT t.name FROM tag t WHERE t.id = 2;

--

UPDATE entry_tag_old SET tag_id = (SELECT MAX(t.id) FROM tag t) WHERE tag_id = 2;

--

UPDATE feed_tag SET tag_id = (SELECT MAX(t.id) FROM tag t) WHERE tag_id = 2;

--

INSERT OR REPLACE INTO tag (id, name, editable) VALUES (2, 'Important', 0);

--

CREATE TABLE entry_tag_rule (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    feed_id INTEGER,
    tag_id INTEGER NOT NULL,
    condition TEXT NOT NULL,
    FOREIGN KEY (feed_id) REFERENCES feed (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
);

--

CREATE TABLE entry_tag (
    entry_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    tag_time INTEGER NOT NULL DEFAULT 0,
    entry_tag_rule_id INTEGER,
    FOREIGN KEY (entry_id) REFERENCES entry (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE,
    FOREIGN KEY (entry_tag_rule_id) REFERENCES entry_tag_rule (id) ON DELETE CASCADE,
    UNIQUE (entry_id, tag_id, entry_tag_rule_id)
);

--

INSERT INTO entry_tag (entry_id, tag_id, tag_time)
    SELECT entry_id, tag_id, tag_time FROM entry_tag_old;

--

CREATE UNIQUE INDEX entry_tag_index__entry_id__tag_id__entry_tag_rule_id ON entry_tag (entry_id, tag_id, entry_tag_rule_id);

--

DROP TABLE entry_tag_old;

--

INSERT INTO entry_tag_rule (feed_id, tag_id, condition)
    SELECT feed_id, tag_id, '' FROM feed_tag;

--

DROP TABLE feed_tag;

--

CREATE TRIGGER entry_fts__after_insert__entry AFTER INSERT ON entry
    BEGIN
        INSERT INTO entry_fts (docid, tags) VALUES (NEW.id, '0');
    END;

--

CREATE TRIGGER entry_fts__after_insert__entry_tag AFTER INSERT ON entry_tag
    BEGIN
        UPDATE entry_fts SET tags = tags || ',' || NEW.tag_id WHERE docid = NEW.entry_id;
    END;

--

CREATE TRIGGER entry_fts__after_delete__entry_tag AFTER DELETE ON entry_tag
    BEGIN
        DELETE FROM entry_fts WHERE docid = OLD.entry_id;
        INSERT INTO entry_fts (docid, tags)
            SELECT e.id, (SELECT group_concat(s.tag_id) FROM (SELECT 0 AS tag_id UNION SELECT et.tag_id FROM entry_tag et WHERE et.entry_id = e.id) AS s) FROM entry e WHERE e.id = OLD.entry_id;
    END;

--

INSERT INTO entry_tag (entry_id, tag_id, tag_time, entry_tag_rule_id) SELECT e.id, etr.tag_id, strftime('%s', 'now') * 1000, etr.id FROM entry_tag_rule etr LEFT JOIN entry e ON etr.feed_id = e.feed_id;

--

PRAGMA user_version = 23;

--
