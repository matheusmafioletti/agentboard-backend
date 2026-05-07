-- Remove the board-seeding trigger function if it still exists
DROP FUNCTION IF EXISTS seed_board_columns() CASCADE;

-- Drop legacy tables: Column and Board are replaced by enum-based stages
DROP TABLE IF EXISTS column_def CASCADE;
DROP TABLE IF EXISTS board CASCADE;
