package com.tughi.aggregator.data.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.transaction
import com.tughi.aggregator.data.Database

object F000T020 : Database.Migration {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.transaction {
            database.execSQL("""
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
                )
            """)

            database.execSQL("""
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
                )
            """)

            database.execSQL("CREATE UNIQUE INDEX entry_index__feed_id__uid ON entry (feed_id, uid)")

            database.execSQL("""
                CREATE TABLE tag (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    editable INTEGER NOT NULL DEFAULT 1
                )
            """)

            database.execSQL("""
                INSERT INTO tag (id, name, editable) VALUES
                    (0, 'All', 0),
                    (1, 'Starred', 0)
            """)

            database.execSQL("""
                CREATE TABLE feed_tag (
                    feed_id INTEGER NOT NULL,
                    tag_id INTEGER NOT NULL,
                    tag_time INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (feed_id) REFERENCES feed (id) ON DELETE CASCADE,
                    FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE,
                    UNIQUE (feed_id, tag_id)
                )
            """)

            database.execSQL("CREATE UNIQUE INDEX feed_tag_index__feed_id__tag_id ON feed_tag (feed_id, tag_id)")

            database.execSQL("""
                CREATE TABLE entry_tag (
                    entry_id INTEGER NOT NULL,
                    tag_id INTEGER NOT NULL,
                    tag_time INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (entry_id) REFERENCES entry (id) ON DELETE CASCADE,
                    FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE,
                    UNIQUE (entry_id, tag_id)
                )
            """)

            database.execSQL("CREATE UNIQUE INDEX entry_tag_index__entry_id__tag_id ON entry_tag (entry_id, tag_id)")

            database.execSQL("""
                CREATE TABLE my_feed_tag (
                    tag_id INTEGER NOT NULL,
                    type INTEGER NOT NULL,
                    FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE,
                    UNIQUE (tag_id, type)
                )
            """)

            database.execSQL("CREATE UNIQUE INDEX my_feed_tag_index__tag_id__type ON my_feed_tag (tag_id, type)")

            database.execSQL("CREATE VIRTUAL TABLE entry_fts USING fts3(tags)")

            database.execSQL("""
                CREATE TRIGGER entry_fts__after_insert__entry AFTER INSERT ON entry
                    BEGIN
                        INSERT INTO entry_fts (docid, tags)
                            SELECT NEW.id, (SELECT group_concat(s.tag_id) FROM (SELECT 0 AS tag_id UNION SELECT ft.tag_id FROM feed_tag ft WHERE ft.feed_id = NEW.feed_id) AS s);
                    END
            """)

            database.execSQL("""
                CREATE TRIGGER entry_fts__after_delete__entry AFTER DELETE ON entry
                    BEGIN
                        DELETE FROM entry_fts WHERE docid = OLD.id;
                    END
            """)

            database.execSQL("""
                CREATE TRIGGER entry_fts__after_insert__entry_tag AFTER INSERT ON entry_tag
                    BEGIN
                        DELETE FROM entry_fts WHERE docid = NEW.entry_id;
                        INSERT INTO entry_fts (docid, tags)
                            SELECT e.id, (SELECT group_concat(s.tag_id) FROM (SELECT 0 AS tag_id UNION SELECT et.tag_id FROM entry_tag et WHERE et.entry_id = e.id UNION SELECT ft.tag_id FROM feed_tag ft WHERE ft.feed_id = e.feed_id) AS s) FROM entry e WHERE e.id = NEW.entry_id;
                    END
            """)

            database.execSQL("""
                CREATE TRIGGER entry_fts__after_delete__entry_tag AFTER DELETE ON entry_tag
                    BEGIN
                        DELETE FROM entry_fts WHERE docid = OLD.entry_id;
                        INSERT INTO entry_fts (docid, tags)
                            SELECT e.id, (SELECT group_concat(s.tag_id) FROM (SELECT 0 AS tag_id UNION SELECT et.tag_id FROM entry_tag et WHERE et.entry_id = e.id UNION SELECT ft.tag_id FROM feed_tag ft WHERE ft.feed_id = e.feed_id) AS s) FROM entry e WHERE e.id = OLD.entry_id;
                    END
            """)

            database.execSQL("""
                CREATE TRIGGER entry_fts__after_insert__feed_tag AFTER INSERT ON feed_tag
                    BEGIN
                        DELETE FROM entry_fts WHERE docid IN (SELECT id FROM entry WHERE feed_id = NEW.feed_id);
                        INSERT INTO entry_fts (docid, tags)
                            SELECT e.id, (SELECT group_concat(s.tag_id) FROM (SELECT 0 AS tag_id UNION SELECT et.tag_id FROM entry_tag et WHERE et.entry_id = e.id UNION SELECT ft.tag_id FROM feed_tag ft WHERE ft.feed_id = e.feed_id) AS s) FROM entry e WHERE e.feed_id = NEW.feed_id;
                    END
            """)

            database.execSQL("""
                CREATE TRIGGER entry_fts__after_delete__feed_tag AFTER DELETE ON feed_tag
                    BEGIN
                        DELETE FROM entry_fts WHERE docid IN (SELECT id FROM entry WHERE feed_id = OLD.feed_id);
                        INSERT INTO entry_fts (docid, tags)
                            SELECT e.id, (SELECT group_concat(s.tag_id) FROM (SELECT 0 AS tag_id UNION SELECT et.tag_id FROM entry_tag et WHERE et.entry_id = e.id UNION SELECT ft.tag_id FROM feed_tag ft WHERE ft.feed_id = e.feed_id) AS s) FROM entry e WHERE e.feed_id = OLD.feed_id;
                    END
            """)
        }
    }
}
