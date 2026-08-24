-- #27: an event id was a source prefix plus six base-36 characters. That is
-- 2.2 billion values per source, which sounds ample and is not: by the birthday
-- bound a collision becomes likely around 55 000 events for one source. On
-- collision the earlier event was overwritten with no error raised.
--
-- Ids are now <source>-<uuid>, generated as UUID version 7 by the application.

ALTER TABLE events ALTER COLUMN id TYPE VARCHAR(100);

ALTER TABLE events DROP CONSTRAINT check_event_id_format;

-- Rows written before this migration carry the old format and would violate the
-- new constraint. They keep their prefix and take a fresh UUID; nothing
-- references an event id, so rewriting it breaks no relation.
--
-- gen_random_uuid() is version 4, not 7, so these ids are not time-ordered.
-- That costs nothing: queries order by the timestamp column and its index, never
-- by id. Ordering is a property version 7 adds for free, not one anything relies
-- on.
UPDATE events
SET id = split_part(id, '-', 1) || '-' || gen_random_uuid()
WHERE id !~ '^[a-z]+-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

ALTER TABLE events ADD CONSTRAINT check_event_id_format
    CHECK (id ~ '^[a-z]+-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$');