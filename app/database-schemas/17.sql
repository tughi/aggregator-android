CREATE TABLE feed (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    link TEXT,
    language TEXT,
    custom_title TEXT,
    favicon_url TEXT,
    favicon_content BLOB,
    update_mode TEXT NOT NULL,
    last_update_time INTEGER NOT NULL DEFAULT 0,
    last_update_error TEXT,
    next_update_retry INTEGER NOT NULL DEFAULT 0,
    next_update_time INTEGER NOT NULL DEFAULT 0,
    http_etag TEXT,
    http_last_modified TEXT
);

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

CREATE UNIQUE INDEX entry_index__feed_id__uid ON entry (feed_id, uid);

CREATE TABLE tag (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL
);

CREATE TABLE feed_tag (
    feed_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    FOREIGN KEY (feed_id) REFERENCES feed (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
);

CREATE TABLE entry_tag (
    entry_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    FOREIGN KEY (entry_id) REFERENCES entry (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
);

