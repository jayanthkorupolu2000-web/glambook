-- ============================================================
-- GlamBook — Remove Group Bookings & Communications
-- ============================================================
-- ⚠  RUN MANUALLY ONCE on a new system after the first backend
--    startup (Hibernate will have created the tables by then).
--
-- DO NOT add this to spring.sql.init — it uses DROP statements
-- that are destructive and not idempotent on MySQL 5.7.
--
-- How to run:
--   mysql -u root -p glambook < remove_messages_groupbookings.sql
-- ============================================================

USE glambook;

-- Step 1: Drop the FK from appointments before dropping the column
-- (Hibernate names the FK with a generated hash — find it first)
SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA   = 'glambook'
      AND TABLE_NAME     = 'appointments'
      AND COLUMN_NAME    = 'group_booking_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);
SET @drop_fk = IF(@fk_name IS NOT NULL,
    CONCAT('ALTER TABLE appointments DROP FOREIGN KEY ', @fk_name),
    'SELECT ''No FK to drop for group_booking_id'' AS info'
);
PREPARE stmt FROM @drop_fk; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Step 2: Drop group_booking_id column from appointments
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'glambook'
      AND TABLE_NAME   = 'appointments'
      AND COLUMN_NAME  = 'group_booking_id'
);
SET @sql = IF(@col_exists > 0,
    'ALTER TABLE appointments DROP COLUMN group_booking_id',
    'SELECT ''group_booking_id already removed'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Step 3: Drop group_booking_participants (depends on group_bookings)
SET @tbl_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = 'glambook' AND TABLE_NAME = 'group_booking_participants'
);
SET @sql = IF(@tbl_exists > 0,
    'DROP TABLE group_booking_participants',
    'SELECT ''group_booking_participants already removed'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Step 4: Drop group_bookings
SET @tbl_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = 'glambook' AND TABLE_NAME = 'group_bookings'
);
SET @sql = IF(@tbl_exists > 0,
    'DROP TABLE group_bookings',
    'SELECT ''group_bookings already removed'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Step 5: Drop communications
SET @tbl_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = 'glambook' AND TABLE_NAME = 'communications'
);
SET @sql = IF(@tbl_exists > 0,
    'DROP TABLE communications',
    'SELECT ''communications already removed'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'remove_messages_groupbookings.sql completed' AS result;
