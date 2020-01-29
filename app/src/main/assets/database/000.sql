--

CREATE TABLE feed (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    link TEXT,
    language TEXT,
    custom_title TEXT,
    favicon_url TEXT,
    favicon_content BLOB,
    cleanup_mode TEXT NOT NULL DEFAULT 'DEFAULT',
    update_mode TEXT NOT NULL,
    last_update_time INTEGER NOT NULL DEFAULT 0,
    last_update_error TEXT,
    next_update_retry INTEGER NOT NULL DEFAULT 0,
    next_update_time INTEGER NOT NULL DEFAULT 0,
    http_etag TEXT,
    http_last_modified TEXT
);

--

CREATE TABLE entry (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    feed_id INTEGER NOT NULL,
    uid TEXT NOT NULL,
    title TEXT,
    link TEXT,
    content TEXT,
    author TEXT,
    publish_time INTEGER,
    insert_time INTEGER NOT NULL,
    update_time INTEGER NOT NULL,
    read_time INTEGER NOT NULL DEFAULT 0,
    pinned_time INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (feed_id) REFERENCES feed (id) ON DELETE CASCADE
);

--

CREATE UNIQUE INDEX entry_index__feed_id__uid ON entry (feed_id, uid);

--

CREATE TABLE tag (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    editable INTEGER NOT NULL DEFAULT 1
);

--

INSERT INTO tag (id, name, editable) VALUES
    (0, 'All', 0),
    (1, 'Starred', 0),
    (2, 'Pinned', 0);

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

CREATE UNIQUE INDEX entry_tag_index__entry_id__tag_id__entry_tag_rule_id ON entry_tag (entry_id, tag_id, entry_tag_rule_id);

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

CREATE VIRTUAL TABLE entry_fts USING fts3(tags);

--

CREATE TRIGGER entry_fts__after_insert__entry AFTER INSERT ON entry
    BEGIN
        INSERT INTO entry_fts (docid, tags) VALUES (NEW.id, '0');
    END;

--

CREATE TRIGGER entry_fts__after_delete__entry AFTER DELETE ON entry
    BEGIN
        DELETE FROM entry_fts WHERE docid = OLD.id;
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
