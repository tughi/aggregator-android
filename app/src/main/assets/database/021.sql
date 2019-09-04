--

ALTER TABLE feed ADD COLUMN cleanup_mode TEXT NOT NULL DEFAULT 'DEFAULT';

--

PRAGMA user_version = 22;

--
