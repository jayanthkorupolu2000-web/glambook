-- ============================================================
-- GlamBook — Fresh System Setup Script
-- Run this on a NEW system where glambook DB is empty.
-- Compatible with MySQL 5.7+ (no IF NOT EXISTS on ALTER TABLE).

-- Usage:
-- mysql -u root -p < fresh_setup.sql
-- OR paste into MySQL Workbench and execute.

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
SET @s = CONCAT('ALTER TABLE ', tbl, ' ADD COLUMN ', col, ' ', def);
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
SET @s = CONCAT('ALTER TABLE ', tbl, ' ADD CONSTRAINT ', cname, ' ', def);
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
END IF;
END //
DELIMITER ;

-- ── CUSTOMER ─────────────────────────────────────────────────────────────────
CALL add_col('customer', 'status', "ENUM('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE'");
CALL add_col('customer', 'cancel_count', 'INT NOT NULL DEFAULT 0');
CALL add_col('customer', 'date_of_birth', 'DATE NULL');
CALL add_col('customer', 'profile_photo_url','VARCHAR(500) NULL');
CALL add_col('customer', 'emergency_contact','VARCHAR(255) NULL');
CALL add_col('customer', 'medical_notes', 'TEXT NULL');
CALL add_col('customer', 'referral_code', 'VARCHAR(20) UNIQUE NULL');
CALL add_col('customer', 'referred_by', 'VARCHAR(20) NULL');
CALL add_col('customer', 'preferences', 'TEXT NULL');
CALL add_col('customer', 'reminder_opt_in', 'TINYINT(1) DEFAULT 1');

-- ── PROFESSIONAL ─────────────────────────────────────────────────────────────
CALL add_col('professional', 'approved_by', 'BIGINT NULL');
CALL add_col('professional', 'approved_at', 'DATETIME NULL');
CALL add_col('professional', 'profile_photo_url', 'VARCHAR(500) NULL');
CALL add_col('professional', 'certifications', 'TEXT NULL');
CALL add_col('professional', 'training_details', 'TEXT NULL');
CALL add_col('professional', 'service_areas', 'TEXT NULL');
CALL add_col('professional', 'travel_radius_km', 'INT DEFAULT 0');
CALL add_col('professional', 'response_time_hrs', 'INT DEFAULT 24');
CALL add_col('professional', 'bio', 'TEXT NULL');
CALL add_col('professional', 'instagram_handle', 'VARCHAR(100) NULL');
CALL add_col('professional', 'is_available_home', 'TINYINT(1) DEFAULT 0');
CALL add_col('professional', 'is_available_salon','TINYINT(1) DEFAULT 1');
CALL add_fk('professional', 'fk_prof_approved_by', 'FOREIGN KEY (approved_by) REFERENCES salon_owner(id)');

-- ── SERVICES ─────────────────────────────────────────────────────────────────
CALL add_col('services', 'gender', "ENUM('MEN','WOMEN','KIDS') NULL");
CALL add_col('services', 'category', 'VARCHAR(100) NULL');
CALL add_col('services', 'is_active', 'TINYINT(1) NOT NULL DEFAULT 1');
CALL add_col('services', 'discount_pct', 'DECIMAL(5,2) DEFAULT 0');
CALL add_col('services', 'professional_id','BIGINT NULL');
CALL add_fk('services', 'fk_services_professional', 'FOREIGN KEY (professional_id) REFERENCES professional(id)');

-- ── APPOINTMENTS ─────────────────────────────────────────────────────────────
CALL add_col('appointments', 'reminder_sent_at', 'DATETIME NULL');
CALL add_col('appointments', 'cancelled_at', 'DATETIME NULL');
CALL add_col('appointments', 'home_address', 'TEXT NULL');
CALL add_col('appointments', 'home_access_notes', 'TEXT NULL');
CALL add_col('appointments', 'travel_fee', 'DECIMAL(8,2) DEFAULT 0');
CALL add_col('appointments', 'rebooked_from_id', 'BIGINT NULL');
CALL add_col('appointments', 'group_booking_id', 'BIGINT NULL');
CALL add_col('appointments', 'reminder_count', 'INT NOT NULL DEFAULT 0');
CALL add_col('appointments', 'last_reminder_sent_at', 'DATETIME NULL');
CALL add_fk('appointments', 'fk_appt_rebook', 'FOREIGN KEY (rebooked_from_id) REFERENCES appointments(id)');

-- ── PAYMENTS ─────────────────────────────────────────────────────────────────
CALL add_col('payments', 'payment_type', "ENUM('DEPOSIT','FULL','REFUND') DEFAULT 'FULL'");
CALL add_col('payments', 'transaction_id','VARCHAR(255) NULL');
CALL add_col('payments', 'receipt_url', 'VARCHAR(500) NULL');

-- ── REVIEWS ──────────────────────────────────────────────────────────────────
CALL add_col('reviews', 'professional_response', 'TEXT NULL');
CALL add_col('reviews', 'professional_response_at', 'DATETIME NULL');
CALL add_col('reviews', 'quality_rating', 'INT NULL');
CALL add_col('reviews', 'timeliness_rating', 'INT NULL');
CALL add_col('reviews', 'professionalism_rating', 'INT NULL');
CALL add_col('reviews', 'review_photo_url', 'VARCHAR(500) NULL');
CALL add_col('reviews', 'is_flagged', 'TINYINT(1) DEFAULT 0');
CALL add_col('reviews', 'appointment_id', 'BIGINT NULL');
CALL add_col('reviews', 'photos', 'JSON NULL');
CALL add_col('reviews', 'status', "VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
CALL add_col('reviews', 'updated_at', 'DATETIME NULL ON UPDATE CURRENT_TIMESTAMP');
CALL add_fk('reviews', 'fk_review_appointment', 'FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL');

-- ── WALLET ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS wallet (  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NOT NULL UNIQUE,
  balance     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  updated_at  DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_wallet_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS wallet_transactions (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  type        VARCHAR(10)  NOT NULL COMMENT 'credit or debit',
  amount      DECIMAL(10,2) NOT NULL,
  source      VARCHAR(30)  NOT NULL COMMENT 'points_redemption or manual',
  description TEXT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_wallet_txn_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
);

-- ── LOYALTY ───────────────────────────────────────────────────────────────────
CALL add_col('loyalty', 'total_earned', 'INT DEFAULT 0');
CALL add_col('loyalty', 'total_redeemed', 'INT DEFAULT 0');

-- ── CONSULTATIONS ─────────────────────────────────────────────────────────────
CALL add_col('consultations', 'topic', "VARCHAR(20) NOT NULL DEFAULT 'GENERAL'");
CALL add_col('consultations', 'question', 'TEXT NULL');
CALL add_col('consultations', 'notes', 'TEXT NULL');
CALL add_col('consultations', 'updated_at', 'DATETIME NULL ON UPDATE CURRENT_TIMESTAMP');
CALL add_col('consultations', 'photo_url', 'VARCHAR(500) NULL');

-- ── PROFESSIONAL AVAILABILITY ─────────────────────────────────────────────────
CALL add_col('professional_availability', 'slot_type', "VARCHAR(20) NOT NULL DEFAULT 'WORKING'");
CALL add_col('professional_availability', 'slot_status', "VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'");

-- ── PORTFOLIO ─────────────────────────────────────────────────────────────────
CALL add_col('portfolio', 'file_path', 'VARCHAR(500) NULL');
CALL add_col('portfolio', 'tags', 'VARCHAR(255) NULL');

-- ── NOTIFICATION TYPE COLUMNS → VARCHAR (no enum size limit) ─────────────────
ALTER TABLE customer_notifications MODIFY COLUMN type VARCHAR(50) NOT NULL;
ALTER TABLE owner_notifications MODIFY COLUMN type VARCHAR(50) NOT NULL;
ALTER TABLE professional_notifications MODIFY COLUMN type VARCHAR(50) NOT NULL;
ALTER TABLE loyalty MODIFY COLUMN tier VARCHAR(20) NOT NULL DEFAULT 'BRONZE';

-- ── NEW TABLES ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS admin_notifications (
id BIGINT AUTO_INCREMENT PRIMARY KEY,
message TEXT NOT NULL,
reference_id BIGINT NULL,
is_read BOOLEAN NOT NULL DEFAULT FALSE,
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loyalty_transactions (
id BIGINT AUTO_INCREMENT PRIMARY KEY,
customer_id BIGINT NOT NULL,
type VARCHAR(10) NOT NULL,
points INT NOT NULL,
description TEXT NULL,
appointment_id BIGINT NULL,
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS favorite_products (
id BIGINT AUTO_INCREMENT PRIMARY KEY,
customer_id BIGINT NOT NULL,
product_id BIGINT NOT NULL,
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
UNIQUE KEY uq_fav_product (customer_id, product_id),
FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS favorite_services (
id BIGINT AUTO_INCREMENT PRIMARY KEY,
customer_id BIGINT NOT NULL,
service_id BIGINT NOT NULL,
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
UNIQUE KEY uq_fav_service (customer_id, service_id),
FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE
);

-- ── DATA FIXES ────────────────────────────────────────────────────────────────
UPDATE services SET is_active = 1 WHERE is_active IS NULL;
UPDATE services SET gender = 'WOMEN' WHERE gender IS NULL;
UPDATE appointments SET reminder_count = 0 WHERE reminder_count IS NULL;

-- ── PRODUCT REVIEWS ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_reviews (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id  BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  rating      INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  review_text TEXT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_product_review (customer_id, product_id),
  FOREIGN KEY (product_id)  REFERENCES products(id)  ON DELETE CASCADE,
  FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
);

-- ── PRODUCT ORDERS ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_orders (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id      BIGINT NOT NULL,
  product_id       BIGINT NOT NULL,
  quantity         INT NOT NULL DEFAULT 1,
  unit_price       DECIMAL(10,2) NOT NULL,
  total_price      DECIMAL(10,2) NOT NULL,
  status           ENUM('PLACED','CONFIRMED','SHIPPED','DELIVERED','CANCELLED') NOT NULL DEFAULT 'PLACED',
  tracking_number  VARCHAR(100) NULL,
  order_date       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  delivery_date    DATE NULL,
  FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id)  REFERENCES products(id)  ON DELETE CASCADE
);

-- ── PRODUCT SEED DATA ─────────────────────────────────────────────────────────
-- Ensure products table has required columns
CALL add_col('products', 'stock_quantity', 'INT NOT NULL DEFAULT 100');
CALL add_col('products', 'stock', 'INT NOT NULL DEFAULT 100');

INSERT IGNORE INTO products (name, brand, category, description, ingredients, usage_tips, price, stock, image_url, is_active)
VALUES
-- HAIRCARE
('Argan Oil Shampoo',    'L''Oreal',    'HAIRCARE',   'Nourishing shampoo with pure argan oil for silky smooth hair.',
 'Aqua, Sodium Laureth Sulfate, Argan Oil, Keratin, Vitamin E',
 'Apply to wet hair, lather and rinse. Use 2-3 times a week.',
 450.00, 150, NULL, 1),
('Keratin Conditioner',  'Tresemme',   'HAIRCARE',   'Deep conditioning treatment that repairs and strengthens hair.',
 'Aqua, Cetearyl Alcohol, Keratin Protein, Panthenol, Silk Amino Acids',
 'Apply after shampooing, leave for 3 minutes, rinse thoroughly.',
 380.00, 120, NULL, 1),
('Hair Growth Serum',    'Mamaearth',  'HAIRCARE',   'Clinically tested serum that promotes hair growth and reduces hair fall.',
 'Redensyl, Procapil, Biotin, Bhringraj Extract, Castor Oil',
 'Apply 10-15 drops to scalp, massage gently. Use daily for best results.',
 620.00, 80, NULL, 1),
-- SKINCARE
('Vitamin C Serum',      'Minimalist', 'SKINCARE',   '10% Vitamin C serum for brightening and anti-aging.',
 'Ascorbic Acid 10%, Hyaluronic Acid, Niacinamide, Ferulic Acid',
 'Apply 3-4 drops on cleansed face. Use morning and night.',
 890.00, 90, NULL, 1),
('Hyaluronic Moisturizer','Neutrogena','SKINCARE',   'Lightweight gel-cream moisturizer with hyaluronic acid for 24hr hydration.',
 'Hyaluronic Acid, Glycerin, Ceramides, Aloe Vera, Vitamin B5',
 'Apply on damp skin morning and evening for best absorption.',
 750.00, 110, NULL, 1),
('SPF 50 Sunscreen',     'Lakme',      'SKINCARE',   'Broad spectrum SPF 50 PA+++ sunscreen with matte finish.',
 'Zinc Oxide, Titanium Dioxide, Niacinamide, Vitamin C, Aloe Vera',
 'Apply generously 15 minutes before sun exposure. Reapply every 2 hours.',
 520.00, 200, NULL, 1),
-- MAKEUP
('HD Foundation',        'MAC',        'MAKEUP',     'Full coverage HD foundation for a flawless, long-lasting finish.',
 'Dimethicone, Cyclopentasiloxane, Titanium Dioxide, Iron Oxides',
 'Apply with brush or sponge. Build coverage as needed.',
 1200.00, 60, NULL, 1),
('Matte Lipstick Set',   'Maybelline', 'MAKEUP',     'Set of 5 long-lasting matte lipsticks in trending shades.',
 'Isododecane, Trimethylsiloxysilicate, Wax, Pigments',
 'Apply directly or with lip brush. Blot for longer wear.',
 680.00, 75, NULL, 1),
('Waterproof Mascara',   'Revlon',     'MAKEUP',     'Volumizing waterproof mascara for dramatic lashes all day.',
 'Beeswax, Carnauba Wax, Iron Oxides, Vitamin E',
 'Apply from root to tip with zigzag motion. Remove with oil-based remover.',
 450.00, 100, NULL, 1),
-- NAILCARE
('Gel Nail Polish Kit',  'OPI',        'NAILCARE',   'Professional gel nail polish kit with 6 trending shades and top coat.',
 'Butyl Acetate, Ethyl Acetate, Nitrocellulose, Adipic Acid',
 'Apply base coat, 2 coats of color, top coat. Cure under UV lamp.',
 950.00, 50, NULL, 1),
('Cuticle Oil',          'CND',        'NAILCARE',   'Nourishing cuticle oil with jojoba and vitamin E for healthy nails.',
 'Jojoba Oil, Vitamin E, Sweet Almond Oil, Lavender Essential Oil',
 'Apply 1-2 drops around cuticles and massage in. Use daily.',
 280.00, 180, NULL, 1),
('Nail Strengthener',    'Essie',      'NAILCARE',   'Fortifying nail treatment that prevents breakage and promotes growth.',
 'Butyl Acetate, Ethyl Acetate, Calcium, Hydrolyzed Wheat Protein',
 'Apply 2 coats on bare nails. Reapply every 2 days.',
 320.00, 140, NULL, 1),
-- FRAGRANCE
('Rose Mist',            'Forest Essentials','FRAGRANCE','Delicate rose water mist for face and body with natural rose extracts.',
 'Aqua, Rosa Damascena Flower Water, Glycerin, Aloe Vera',
 'Spritz on face and body throughout the day for a refreshing feel.',
 580.00, 90, NULL, 1),
('Jasmine Eau de Parfum','Chanel',     'FRAGRANCE',  'Luxurious jasmine-based eau de parfum with floral and woody notes.',
 'Alcohol Denat., Aqua, Parfum, Jasmine Absolute, Sandalwood',
 'Spray on pulse points — wrists, neck, behind ears. Do not rub.',
 1450.00, 40, NULL, 1),
-- TOOLS
('Jade Roller',          'Herbivore',  'TOOLS',      'Authentic jade facial roller for lymphatic drainage and de-puffing.',
 '100% Natural Jade Stone, Stainless Steel Handle',
 'Use on cleansed face with serum or oil. Roll upward and outward.',
 750.00, 70, NULL, 1);

-- ── Cleanup helpers ───────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS add_col;
DROP PROCEDURE IF EXISTS add_fk;

SELECT 'fresh_setup.sql completed successfully' AS result;