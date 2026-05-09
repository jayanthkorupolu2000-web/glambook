-- ============================================================
-- GlamBook — Fresh System Setup Script
-- Run this on a NEW system where glambook DB is empty.
-- Compatible with MySQL 5.7+ (no IF NOT EXISTS on ALTER TABLE).
--
-- Usage:
--   mysql -u root -p < fresh_setup.sql
-- OR paste into MySQL Workbench and execute.
--
-- This script is SAFE to run on an existing system too —
-- every statement is wrapped in a procedure that checks
-- information_schema before executing.
-- ============================================================

CREATE DATABASE IF NOT EXISTS glambook;
USE glambook;

-- ── Helper procedure: add column only if it doesn't exist ────────────────────
DROP PROCEDURE IF EXISTS add_col;
DELIMITER //
CREATE PROCEDURE add_col(IN tbl VARCHAR(64), IN col VARCHAR(64), IN def TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'glambook' AND TABLE_NAME = tbl AND COLUMN_NAME = col
  ) THEN
    SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', def);
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END //
DELIMITER ;

-- ── Helper procedure: add constraint only if it doesn't exist ────────────────
DROP PROCEDURE IF EXISTS add_fk;
DELIMITER //
CREATE PROCEDURE add_fk(IN tbl VARCHAR(64), IN cname VARCHAR(64), IN def TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = 'glambook' AND TABLE_NAME = tbl AND CONSTRAINT_NAME = cname
  ) THEN
    SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD CONSTRAINT `', cname, '` ', def);
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END //
DELIMITER ;

-- ── CUSTOMER ─────────────────────────────────────────────────────────────────
CALL add_col('customer', 'status',           "ENUM('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE'");
CALL add_col('customer', 'cancel_count',     'INT NOT NULL DEFAULT 0');
CALL add_col('customer', 'date_of_birth',    'DATE NULL');
CALL add_col('customer', 'profile_photo_url','VARCHAR(500) NULL');
CALL add_col('customer', 'emergency_contact','VARCHAR(255) NULL');
CALL add_col('customer', 'medical_notes',    'TEXT NULL');
CALL add_col('customer', 'referral_code',    'VARCHAR(20) UNIQUE NULL');
CALL add_col('customer', 'referred_by',      'VARCHAR(20) NULL');
CALL add_col('customer', 'preferences',      'TEXT NULL');
CALL add_col('customer', 'reminder_opt_in',  'TINYINT(1) DEFAULT 1');

-- ── PROFESSIONAL ─────────────────────────────────────────────────────────────
CALL add_col('professional', 'approved_by',       'BIGINT NULL');
CALL add_col('professional', 'approved_at',       'DATETIME NULL');
CALL add_col('professional', 'profile_photo_url', 'VARCHAR(500) NULL');
CALL add_col('professional', 'certifications',    'TEXT NULL');
CALL add_col('professional', 'training_details',  'TEXT NULL');
CALL add_col('professional', 'service_areas',     'TEXT NULL');
CALL add_col('professional', 'travel_radius_km',  'INT DEFAULT 0');
CALL add_col('professional', 'response_time_hrs', 'INT DEFAULT 24');
CALL add_col('professional', 'bio',               'TEXT NULL');
CALL add_col('professional', 'instagram_handle',  'VARCHAR(100) NULL');
CALL add_col('professional', 'is_available_home', 'TINYINT(1) DEFAULT 0');
CALL add_col('professional', 'is_available_salon','TINYINT(1) DEFAULT 1');
CALL add_fk('professional', 'fk_prof_approved_by', 'FOREIGN KEY (approved_by) REFERENCES salon_owner(id)');

-- ── SERVICES ─────────────────────────────────────────────────────────────────
CALL add_col('services', 'gender',         "ENUM('MEN','WOMEN','KIDS') NULL");
CALL add_col('services', 'category',       'VARCHAR(100) NULL');
CALL add_col('services', 'is_active',      'TINYINT(1) NOT NULL DEFAULT 1');
CALL add_col('services', 'discount_pct',   'DECIMAL(5,2) DEFAULT 0');
CALL add_col('services', 'professional_id','BIGINT NULL');
CALL add_fk('services', 'fk_services_professional', 'FOREIGN KEY (professional_id) REFERENCES professional(id)');

-- ── APPOINTMENTS ─────────────────────────────────────────────────────────────
CALL add_col('appointments', 'reminder_sent_at',      'DATETIME NULL');
CALL add_col('appointments', 'cancelled_at',          'DATETIME NULL');
CALL add_col('appointments', 'home_address',          'TEXT NULL');
CALL add_col('appointments', 'home_access_notes',     'TEXT NULL');
CALL add_col('appointments', 'travel_fee',            'DECIMAL(8,2) DEFAULT 0');
CALL add_col('appointments', 'rebooked_from_id',      'BIGINT NULL');
CALL add_col('appointments', 'group_booking_id',      'BIGINT NULL');
CALL add_col('appointments', 'reminder_count',        'INT NOT NULL DEFAULT 0');
CALL add_col('appointments', 'last_reminder_sent_at', 'DATETIME NULL');
CALL add_fk('appointments', 'fk_appt_rebook', 'FOREIGN KEY (rebooked_from_id) REFERENCES appointments(id)');

-- ── PAYMENTS ─────────────────────────────────────────────────────────────────
CALL add_col('payments', 'payment_type',  "ENUM('DEPOSIT','FULL','REFUND') DEFAULT 'FULL'");
CALL add_col('payments', 'transaction_id','VARCHAR(255) NULL');
CALL add_col('payments', 'receipt_url',   'VARCHAR(500) NULL');

-- ── REVIEWS ──────────────────────────────────────────────────────────────────
CALL add_col('reviews', 'professional_response',    'TEXT NULL');
CALL add_col('reviews', 'professional_response_at', 'DATETIME NULL');
CALL add_col('reviews', 'quality_rating',           'INT NULL');
CALL add_col('reviews', 'timeliness_rating',        'INT NULL');
CALL add_col('reviews', 'professionalism_rating',   'INT NULL');
CALL add_col('reviews', 'review_photo_url',         'VARCHAR(500) NULL');
CALL add_col('reviews', 'is_flagged',               'TINYINT(1) DEFAULT 0');
CALL add_col('reviews', 'appointment_id',           'BIGINT NULL');
CALL add_col('reviews', 'photos',                   'JSON NULL');
CALL add_col('reviews', 'status',                   "VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
CALL add_col('reviews', 'updated_at',               'DATETIME NULL ON UPDATE CURRENT_TIMESTAMP');
CALL add_fk('reviews', 'fk_review_appointment', 'FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL');

-- ── LOYALTY ───────────────────────────────────────────────────────────────────
CALL add_col('loyalty', 'total_earned',   'INT DEFAULT 0');
CALL add_col('loyalty', 'total_redeemed', 'INT DEFAULT 0');

-- ── CONSULTATIONS ─────────────────────────────────────────────────────────────
CALL add_col('consultations', 'topic',      "VARCHAR(20) NOT NULL DEFAULT 'GENERAL'");
CALL add_col('consultations', 'question',   'TEXT NULL');
CALL add_col('consultations', 'notes',      'TEXT NULL');
CALL add_col('consultations', 'updated_at', 'DATETIME NULL ON UPDATE CURRENT_TIMESTAMP');
CALL add_col('consultations', 'photo_url',  'VARCHAR(500) NULL');

-- ── PROFESSIONAL AVAILABILITY ─────────────────────────────────────────────────
CALL add_col('professional_availability', 'slot_type',   "VARCHAR(20) NOT NULL DEFAULT 'WORKING'");
CALL add_col('professional_availability', 'slot_status', "VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'");

-- ── PORTFOLIO ─────────────────────────────────────────────────────────────────
CALL add_col('portfolio', 'file_path', 'VARCHAR(500) NULL');
CALL add_col('portfolio', 'tags',      'VARCHAR(255) NULL');

-- ── NOTIFICATION TYPE COLUMNS → VARCHAR (no enum size limit) ─────────────────
ALTER TABLE customer_notifications      MODIFY COLUMN type VARCHAR(50) NOT NULL;
ALTER TABLE owner_notifications         MODIFY COLUMN type VARCHAR(50) NOT NULL;
ALTER TABLE professional_notifications  MODIFY COLUMN type VARCHAR(50) NOT NULL;
ALTER TABLE loyalty                     MODIFY COLUMN tier VARCHAR(20) NOT NULL DEFAULT 'BRONZE';

-- ── NEW TABLES ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS admin_notifications (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    message      TEXT NOT NULL,
    reference_id BIGINT NULL,
    is_read      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loyalty_transactions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id    BIGINT NOT NULL,
    type           VARCHAR(10) NOT NULL,
    points         INT NOT NULL,
    description    TEXT NULL,
    appointment_id BIGINT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id)    REFERENCES customer(id) ON DELETE CASCADE,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS favorite_products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_fav_product (customer_id, product_id),
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id)  REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS favorite_services (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    service_id  BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_fav_service (customer_id, service_id),
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
    FOREIGN KEY (service_id)  REFERENCES services(id) ON DELETE CASCADE
);

-- ── DATA FIXES ────────────────────────────────────────────────────────────────
UPDATE services SET is_active = 1 WHERE is_active IS NULL;
UPDATE services SET gender = 'WOMEN' WHERE gender IS NULL;
UPDATE appointments SET reminder_count = 0 WHERE reminder_count IS NULL;

-- ── Cleanup helpers ───────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS add_col;
DROP PROCEDURE IF EXISTS add_fk;

SELECT 'fresh_setup.sql completed successfully' AS result;
