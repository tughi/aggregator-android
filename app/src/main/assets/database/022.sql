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

PRAGMA user_version = 23;

--
