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
    last_update_time INTEGER NOT NULL,
    last_update_error TEXT,
    next_update_retry INTEGER NOT NULL,
    next_update_time INTEGER NOT NULL,
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
    read_time INTEGER NOT NULL,
    pinned_time INTEGER NOT NULL,
    FOREIGN KEY (feed_id) REFERENCES feeds (id) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE UNIQUE INDEX index_entries_feed_id_uid ON entries (feed_id, uid);
