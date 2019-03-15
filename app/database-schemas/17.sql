CREATE TABLE feeds (
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

CREATE TABLE entries (
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
    FOREIGN KEY (feed_id) REFERENCES feeds (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX entries_index__feed_id__uid ON entries (feed_id, uid);
