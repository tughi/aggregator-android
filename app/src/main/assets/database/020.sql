--

DROP TRIGGER entry_fts__after_insert__entry_tag;

--

CREATE TRIGGER entry_fts__after_insert__entry_tag AFTER INSERT ON entry_tag
    BEGIN
        UPDATE entry_fts SET tags = tags || ',' || NEW.tag_id WHERE docid = NEW.entry_id;
    END;

--

DROP TRIGGER entry_fts__after_insert__feed_tag;

--

CREATE TRIGGER entry_fts__after_insert__feed_tag AFTER INSERT ON feed_tag
    BEGIN
        UPDATE entry_fts SET tags = tags || ',' || NEW.tag_id WHERE docid IN (SELECT e.id FROM entry e WHERE e.feed_id = NEW.feed_id);
    END;

--

PRAGMA user_version = 21;

--