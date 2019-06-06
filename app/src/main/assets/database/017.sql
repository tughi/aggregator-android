--

CREATE TABLE my_feed_tag (
    tag_id INTEGER NOT NULL,
    type INTEGER NOT NULL,
    FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE,
    UNIQUE (tag_id, type)
);

--

CREATE UNIQUE INDEX my_feed_tag_index__tag_id__type ON my_feed_tag (tag_id, type);

--

DELETE FROM tag;

--

INSERT INTO tag (id, name, editable) VALUES
    (0, 'All', 0),
    (1, 'Starred', 0);

--

CREATE VIRTUAL TABLE entry_fts USING fts3(tags);

--

INSERT INTO entry_fts (docid, tags)
    SELECT id, '0' FROM entry;

--

CREATE TRIGGER entry_fts__after_insert__entry AFTER INSERT ON entry
    BEGIN
        INSERT INTO entry_fts (docid, tags)
            SELECT NEW.id, (SELECT group_concat(s.tag_id) FROM (SELECT 0 AS tag_id UNION SELECT ft.tag_id FROM feed_tag ft WHERE ft.feed_id = NEW.feed_id) AS s);
    END;

--

CREATE TRIGGER entry_fts__after_delete__entry AFTER DELETE ON entry
    BEGIN
        DELETE FROM entry_fts WHERE docid = OLD.id;
    END;

--

CREATE TRIGGER entry_fts__after_insert__entry_tag AFTER INSERT ON entry_tag
    BEGIN
        DELETE FROM entry_fts WHERE docid = NEW.entry_id;
        INSERT INTO entry_fts (docid, tags)
            SELECT e.id, (SELECT group_concat(s.tag_id) FROM (SELECT 0 AS tag_id UNION SELECT et.tag_id FROM entry_tag et WHERE et.entry_id = e.id UNION SELECT ft.tag_id FROM feed_tag ft WHERE ft.feed_id = e.feed_id) AS s) FROM entry e WHERE e.id = NEW.entry_id;
    END;

--

CREATE TRIGGER entry_fts__after_delete__entry_tag AFTER DELETE ON entry_tag
    BEGIN
        DELETE FROM entry_fts WHERE docid = OLD.entry_id;
        INSERT INTO entry_fts (docid, tags)
            SELECT e.id, (SELECT group_concat(s.tag_id) FROM (SELECT 0 AS tag_id UNION SELECT et.tag_id FROM entry_tag et WHERE et.entry_id = e.id UNION SELECT ft.tag_id FROM feed_tag ft WHERE ft.feed_id = e.feed_id) AS s) FROM entry e WHERE e.id = OLD.entry_id;
    END;

--

CREATE TRIGGER entry_fts__after_insert__feed_tag AFTER INSERT ON feed_tag
    BEGIN
        DELETE FROM entry_fts WHERE docid IN (SELECT id FROM entry WHERE feed_id = NEW.feed_id);
        INSERT INTO entry_fts (docid, tags)
            SELECT e.id, (SELECT group_concat(s.tag_id) FROM (SELECT 0 AS tag_id UNION SELECT et.tag_id FROM entry_tag et WHERE et.entry_id = e.id UNION SELECT ft.tag_id FROM feed_tag ft WHERE ft.feed_id = e.feed_id) AS s) FROM entry e WHERE e.feed_id = NEW.feed_id;
    END;

--

CREATE TRIGGER entry_fts__after_delete__feed_tag AFTER DELETE ON feed_tag
    BEGIN
        DELETE FROM entry_fts WHERE docid IN (SELECT id FROM entry WHERE feed_id = OLD.feed_id);
        INSERT INTO entry_fts (docid, tags)
            SELECT e.id, (SELECT group_concat(s.tag_id) FROM (SELECT 0 AS tag_id UNION SELECT et.tag_id FROM entry_tag et WHERE et.entry_id = e.id UNION SELECT ft.tag_id FROM feed_tag ft WHERE ft.feed_id = e.feed_id) AS s) FROM entry e WHERE e.feed_id = OLD.feed_id;
    END;

--

PRAGMA user_version = 20;

--
