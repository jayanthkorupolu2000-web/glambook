-- ============================================================
-- GlamBook — Fresh System Setup / Incremental Migration
-- Executed automatically by Spring Boot on every startup.
-- Fully idempotent — safe to re-run on existing systems.
--
-- Uses INFORMATION_SCHEMA checks + PREPARE/EXECUTE instead of
-- stored procedures (DELIMITER does not work via JDBC).
-- ============================================================

USE glambook;

-- ============================================================
-- CUSTOMER — extra columns
-- ============================================================

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='customer' AND COLUMN_NAME='status');
SET @s = IF(@c=0, "ALTER TABLE customer ADD COLUMN status ENUM('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE'", 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='customer' AND COLUMN_NAME='cancel_count');
SET @s = IF(@c=0, 'ALTER TABLE customer ADD COLUMN cancel_count INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='customer' AND COLUMN_NAME='date_of_birth');
SET @s = IF(@c=0, 'ALTER TABLE customer ADD COLUMN date_of_birth DATE NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='customer' AND COLUMN_NAME='profile_photo_url');
SET @s = IF(@c=0, 'ALTER TABLE customer ADD COLUMN profile_photo_url VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='customer' AND COLUMN_NAME='emergency_contact');
SET @s = IF(@c=0, 'ALTER TABLE customer ADD COLUMN emergency_contact VARCHAR(255) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='customer' AND COLUMN_NAME='medical_notes');
SET @s = IF(@c=0, 'ALTER TABLE customer ADD COLUMN medical_notes TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='customer' AND COLUMN_NAME='referral_code');
SET @s = IF(@c=0, 'ALTER TABLE customer ADD COLUMN referral_code VARCHAR(20) UNIQUE NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='customer' AND COLUMN_NAME='referred_by');
SET @s = IF(@c=0, 'ALTER TABLE customer ADD COLUMN referred_by VARCHAR(20) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='customer' AND COLUMN_NAME='preferences');
SET @s = IF(@c=0, 'ALTER TABLE customer ADD COLUMN preferences TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='customer' AND COLUMN_NAME='reminder_opt_in');
SET @s = IF(@c=0, 'ALTER TABLE customer ADD COLUMN reminder_opt_in TINYINT(1) DEFAULT 1', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ============================================================
-- PROFESSIONAL — extra columns
-- ============================================================

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='approved_by');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN approved_by BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='approved_at');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN approved_at DATETIME NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='profile_photo_url');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN profile_photo_url VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='certifications');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN certifications MEDIUMTEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='training_details');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN training_details TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='service_areas');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN service_areas TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='travel_radius_km');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN travel_radius_km INT DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='response_time_hrs');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN response_time_hrs INT DEFAULT 24', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='bio');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN bio TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='instagram_handle');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN instagram_handle VARCHAR(100) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='is_available_home');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN is_available_home TINYINT(1) DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='is_available_salon');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN is_available_salon TINYINT(1) DEFAULT 1', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='suspension_reason');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN suspension_reason TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND COLUMN_NAME='suspended_until');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD COLUMN suspended_until DATETIME NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- FK: approved_by → salon_owner
SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional' AND CONSTRAINT_NAME='fk_prof_approved_by');
SET @s = IF(@c=0, 'ALTER TABLE professional ADD CONSTRAINT fk_prof_approved_by FOREIGN KEY (approved_by) REFERENCES salon_owner(id)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ============================================================
-- SERVICES — extra columns
-- ============================================================

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='services' AND COLUMN_NAME='gender');
SET @s = IF(@c=0, "ALTER TABLE services ADD COLUMN gender ENUM('MEN','WOMEN','KIDS') NULL", 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='services' AND COLUMN_NAME='category');
SET @s = IF(@c=0, 'ALTER TABLE services ADD COLUMN category VARCHAR(100) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='services' AND COLUMN_NAME='is_active');
SET @s = IF(@c=0, 'ALTER TABLE services ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='services' AND COLUMN_NAME='discount_pct');
SET @s = IF(@c=0, 'ALTER TABLE services ADD COLUMN discount_pct DECIMAL(5,2) DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='services' AND COLUMN_NAME='professional_id');
SET @s = IF(@c=0, 'ALTER TABLE services ADD COLUMN professional_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='services' AND CONSTRAINT_NAME='fk_services_professional');
SET @s = IF(@c=0, 'ALTER TABLE services ADD CONSTRAINT fk_services_professional FOREIGN KEY (professional_id) REFERENCES professional(id)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ============================================================
-- APPOINTMENTS — extra columns
-- ============================================================

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='appointments' AND COLUMN_NAME='reminder_sent_at');
SET @s = IF(@c=0, 'ALTER TABLE appointments ADD COLUMN reminder_sent_at DATETIME NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='appointments' AND COLUMN_NAME='cancelled_at');
SET @s = IF(@c=0, 'ALTER TABLE appointments ADD COLUMN cancelled_at DATETIME NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='appointments' AND COLUMN_NAME='home_address');
SET @s = IF(@c=0, 'ALTER TABLE appointments ADD COLUMN home_address TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='appointments' AND COLUMN_NAME='home_access_notes');
SET @s = IF(@c=0, 'ALTER TABLE appointments ADD COLUMN home_access_notes TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='appointments' AND COLUMN_NAME='travel_fee');
SET @s = IF(@c=0, 'ALTER TABLE appointments ADD COLUMN travel_fee DECIMAL(8,2) DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='appointments' AND COLUMN_NAME='rebooked_from_id');
SET @s = IF(@c=0, 'ALTER TABLE appointments ADD COLUMN rebooked_from_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='appointments' AND COLUMN_NAME='reminder_count');
SET @s = IF(@c=0, 'ALTER TABLE appointments ADD COLUMN reminder_count INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='appointments' AND COLUMN_NAME='last_reminder_sent_at');
SET @s = IF(@c=0, 'ALTER TABLE appointments ADD COLUMN last_reminder_sent_at DATETIME NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='appointments' AND CONSTRAINT_NAME='fk_appt_rebook');
SET @s = IF(@c=0, 'ALTER TABLE appointments ADD CONSTRAINT fk_appt_rebook FOREIGN KEY (rebooked_from_id) REFERENCES appointments(id)', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ============================================================
-- PAYMENTS — extra columns
-- ============================================================

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='payments' AND COLUMN_NAME='payment_type');
SET @s = IF(@c=0, "ALTER TABLE payments ADD COLUMN payment_type ENUM('DEPOSIT','FULL','REFUND') DEFAULT 'FULL'", 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='payments' AND COLUMN_NAME='transaction_id');
SET @s = IF(@c=0, 'ALTER TABLE payments ADD COLUMN transaction_id VARCHAR(255) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='payments' AND COLUMN_NAME='receipt_url');
SET @s = IF(@c=0, 'ALTER TABLE payments ADD COLUMN receipt_url VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ============================================================
-- REVIEWS — extra columns
-- ============================================================

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='reviews' AND COLUMN_NAME='professional_response');
SET @s = IF(@c=0, 'ALTER TABLE reviews ADD COLUMN professional_response TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='reviews' AND COLUMN_NAME='professional_response_at');
SET @s = IF(@c=0, 'ALTER TABLE reviews ADD COLUMN professional_response_at DATETIME NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='reviews' AND COLUMN_NAME='quality_rating');
SET @s = IF(@c=0, 'ALTER TABLE reviews ADD COLUMN quality_rating INT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='reviews' AND COLUMN_NAME='timeliness_rating');
SET @s = IF(@c=0, 'ALTER TABLE reviews ADD COLUMN timeliness_rating INT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='reviews' AND COLUMN_NAME='professionalism_rating');
SET @s = IF(@c=0, 'ALTER TABLE reviews ADD COLUMN professionalism_rating INT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='reviews' AND COLUMN_NAME='review_photo_url');
SET @s = IF(@c=0, 'ALTER TABLE reviews ADD COLUMN review_photo_url VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='reviews' AND COLUMN_NAME='is_flagged');
SET @s = IF(@c=0, 'ALTER TABLE reviews ADD COLUMN is_flagged TINYINT(1) DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='reviews' AND COLUMN_NAME='appointment_id');
SET @s = IF(@c=0, 'ALTER TABLE reviews ADD COLUMN appointment_id BIGINT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='reviews' AND COLUMN_NAME='photos');
SET @s = IF(@c=0, 'ALTER TABLE reviews ADD COLUMN photos JSON NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='reviews' AND COLUMN_NAME='status');
SET @s = IF(@c=0, "ALTER TABLE reviews ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'", 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='reviews' AND COLUMN_NAME='updated_at');
SET @s = IF(@c=0, 'ALTER TABLE reviews ADD COLUMN updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='reviews' AND CONSTRAINT_NAME='fk_review_appointment');
SET @s = IF(@c=0, 'ALTER TABLE reviews ADD CONSTRAINT fk_review_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ============================================================
-- LOYALTY — extra columns
-- ============================================================

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='loyalty' AND COLUMN_NAME='total_earned');
SET @s = IF(@c=0, 'ALTER TABLE loyalty ADD COLUMN total_earned INT DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='loyalty' AND COLUMN_NAME='total_redeemed');
SET @s = IF(@c=0, 'ALTER TABLE loyalty ADD COLUMN total_redeemed INT DEFAULT 0', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ============================================================
-- CONSULTATIONS — extra columns
-- ============================================================

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='consultations' AND COLUMN_NAME='topic');
SET @s = IF(@c=0, "ALTER TABLE consultations ADD COLUMN topic VARCHAR(20) NOT NULL DEFAULT 'GENERAL'", 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='consultations' AND COLUMN_NAME='question');
SET @s = IF(@c=0, 'ALTER TABLE consultations ADD COLUMN question TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='consultations' AND COLUMN_NAME='notes');
SET @s = IF(@c=0, 'ALTER TABLE consultations ADD COLUMN notes TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='consultations' AND COLUMN_NAME='updated_at');
SET @s = IF(@c=0, 'ALTER TABLE consultations ADD COLUMN updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='consultations' AND COLUMN_NAME='photo_url');
SET @s = IF(@c=0, 'ALTER TABLE consultations ADD COLUMN photo_url VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='consultations' AND COLUMN_NAME='professional_reply');
SET @s = IF(@c=0, 'ALTER TABLE consultations ADD COLUMN professional_reply TEXT NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='consultations' AND COLUMN_NAME='professional_replied_at');
SET @s = IF(@c=0, 'ALTER TABLE consultations ADD COLUMN professional_replied_at DATETIME NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ============================================================
-- PROFESSIONAL AVAILABILITY — extra columns
-- ============================================================

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional_availability' AND COLUMN_NAME='slot_type');
SET @s = IF(@c=0, "ALTER TABLE professional_availability ADD COLUMN slot_type VARCHAR(20) NOT NULL DEFAULT 'WORKING'", 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='professional_availability' AND COLUMN_NAME='slot_status');
SET @s = IF(@c=0, "ALTER TABLE professional_availability ADD COLUMN slot_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'", 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ============================================================
-- PORTFOLIO — extra columns
-- ============================================================

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='portfolio' AND COLUMN_NAME='file_path');
SET @s = IF(@c=0, 'ALTER TABLE portfolio ADD COLUMN file_path VARCHAR(500) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='glambook' AND TABLE_NAME='portfolio' AND COLUMN_NAME='tags');
SET @s = IF(@c=0, 'ALTER TABLE portfolio ADD COLUMN tags VARCHAR(255) NULL', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ============================================================
-- NOTIFICATION TYPE COLUMNS — widen to VARCHAR so new enum
-- values never cause a DB truncation error
-- ============================================================

ALTER TABLE customer_notifications      MODIFY COLUMN type VARCHAR(50) NOT NULL;
ALTER TABLE owner_notifications         MODIFY COLUMN type VARCHAR(50) NOT NULL;
ALTER TABLE professional_notifications  MODIFY COLUMN type VARCHAR(50) NOT NULL;

-- ============================================================
-- LOYALTY TIER — widen to VARCHAR (supports PLATINUM etc.)
-- ============================================================

ALTER TABLE loyalty MODIFY COLUMN tier VARCHAR(20) NOT NULL DEFAULT 'BRONZE';

-- ============================================================
-- PROFESSIONAL — widen certifications to MEDIUMTEXT
-- ============================================================

ALTER TABLE professional MODIFY COLUMN certifications MEDIUMTEXT NULL;

-- ============================================================
-- DATA FIXES
-- ============================================================

UPDATE services     SET is_active     = 1       WHERE is_active IS NULL;
UPDATE services     SET gender        = 'WOMEN'  WHERE gender IS NULL;
UPDATE appointments SET reminder_count = 0       WHERE reminder_count IS NULL;

-- ============================================================
-- SALON OWNER — unique salon names & official emails
-- ============================================================

UPDATE salon_owner SET salon_name='GlamVizag Studio',         email='info@glamvizag.com'          WHERE id=1 AND (salon_name != 'GlamVizag Studio'         OR email != 'info@glamvizag.com');
UPDATE salon_owner SET salon_name='Royale Cuts Vijayawada',   email='contact@royalecutsvjw.com'   WHERE id=2 AND (salon_name != 'Royale Cuts Vijayawada'   OR email != 'contact@royalecutsvjw.com');
UPDATE salon_owner SET salon_name='Prestige Salon Hyderabad', email='hello@prestigesalon.com'     WHERE id=3 AND (salon_name != 'Prestige Salon Hyderabad' OR email != 'hello@prestigesalon.com');
UPDATE salon_owner SET salon_name='Elegance Beauty Lounge',   email='bookings@elegancebeauty.com' WHERE id=4 AND (salon_name != 'Elegance Beauty Lounge'   OR email != 'bookings@elegancebeauty.com');
UPDATE salon_owner SET salon_name='Sparkle Salon Khammam',    email='info@sparklesalon.com'       WHERE id=5 AND (salon_name != 'Sparkle Salon Khammam'    OR email != 'info@sparklesalon.com');
