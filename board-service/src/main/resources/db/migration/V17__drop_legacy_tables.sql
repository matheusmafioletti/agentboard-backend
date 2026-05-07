-- IMPORTANT: Run ONLY after all WorkItem-based tests pass and legacy entity classes are removed.
-- Drops legacy feature/user_story/task tables that have been superseded by work_item.

DROP TABLE IF EXISTS task CASCADE;
DROP TABLE IF EXISTS user_story CASCADE;
DROP TABLE IF EXISTS feature CASCADE;
