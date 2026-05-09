USE glambook;

-- ================= CUSTOMER =================
ALTER TABLE customer ADD COLUMN status ENUM('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE customer ADD COLUMN cancel_count INT NOT NULL DEFAULT 0;
ALTER TABLE customer ADD COLUMN date_of_birth DATE NULL;
ALTER TABLE customer ADD COLUMN profile_photo_url VARCHAR(500) NULL;
ALTER TABLE customer ADD COLUMN emergency_contact VARCHAR(255) NULL;
ALTER TABLE customer ADD COLUMN medical_notes TEXT NULL;
ALTER TABLE customer ADD COLUMN referral_code VARCHAR(20) UNIQUE NULL;
ALTER TABLE customer ADD COLUMN referred_by VARCHAR(20) NULL;
ALTER TABLE customer ADD COLUMN preferences TEXT NULL;
ALTER TABLE customer ADD COLUMN reminder_opt_in TINYINT(1) DEFAULT 1;

-- ================= PROFESSIONAL =================
ALTER TABLE professional ADD COLUMN approved_by BIGINT NULL;
ALTER TABLE professional ADD COLUMN approved_at DATETIME NULL;
ALTER TABLE professional MODIFY status ENUM('PENDING','ACTIVE','SUSPENDED') NOT NULL DEFAULT 'PENDING';
ALTER TABLE professional ADD CONSTRAINT fk_prof_approved_by FOREIGN KEY (approved_by) REFERENCES salon_owner(id);
ALTER TABLE professional ADD COLUMN profile_photo_url VARCHAR(500) NULL;
ALTER TABLE professional ADD COLUMN certifications TEXT NULL;
ALTER TABLE professional ADD COLUMN training_details TEXT NULL;
ALTER TABLE professional ADD COLUMN service_areas TEXT NULL;
ALTER TABLE professional ADD COLUMN travel_radius_km INT DEFAULT 0;
ALTER TABLE professional ADD COLUMN response_time_hrs INT DEFAULT 24;
ALTER TABLE professional ADD COLUMN bio TEXT NULL;
ALTER TABLE professional ADD COLUMN instagram_handle VARCHAR(100) NULL;
ALTER TABLE professional ADD COLUMN is_available_home TINYINT(1) DEFAULT 0;
ALTER TABLE professional ADD COLUMN is_available_salon TINYINT(1) DEFAULT 1;

-- ================= SERVICES =================
ALTER TABLE services ADD COLUMN gender ENUM('MEN','WOMEN','KIDS') NULL;
ALTER TABLE services ADD COLUMN category VARCHAR(100) NULL;
ALTER TABLE services ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1;
ALTER TABLE services ADD COLUMN discount_pct DECIMAL(5,2) DEFAULT 0;
ALTER TABLE services ADD COLUMN professional_id BIGINT NULL;
ALTER TABLE services ADD CONSTRAINT fk_services_professional FOREIGN KEY (professional_id) REFERENCES professional(id);

-- ================= APPOINTMENTS =================
ALTER TABLE appointments ADD COLUMN reminder_sent_at DATETIME NULL;
ALTER TABLE appointments ADD COLUMN cancelled_at DATETIME NULL;
ALTER TABLE appointments ADD COLUMN home_address TEXT NULL;
ALTER TABLE appointments ADD COLUMN home_access_notes TEXT NULL;
ALTER TABLE appointments ADD COLUMN travel_fee DECIMAL(8,2) DEFAULT 0;
ALTER TABLE appointments ADD COLUMN rebooked_from_id BIGINT NULL;
ALTER TABLE appointments ADD COLUMN group_booking_id BIGINT NULL;
ALTER TABLE appointments ADD CONSTRAINT fk_appt_rebook FOREIGN KEY (rebooked_from_id) REFERENCES appointments(id);

-- ================= PAYMENTS =================
ALTER TABLE payments ADD COLUMN payment_type ENUM('DEPOSIT','FULL','REFUND') DEFAULT 'FULL';
ALTER TABLE payments ADD COLUMN transaction_id VARCHAR(255) NULL;
ALTER TABLE payments ADD COLUMN receipt_url VARCHAR(500) NULL;

-- ================= REVIEWS =================
ALTER TABLE reviews ADD COLUMN professional_response TEXT NULL;
ALTER TABLE reviews ADD COLUMN professional_response_at DATETIME NULL;
ALTER TABLE reviews ADD COLUMN quality_rating INT NULL;
ALTER TABLE reviews ADD COLUMN timeliness_rating INT NULL;
ALTER TABLE reviews ADD COLUMN professionalism_rating INT NULL;
ALTER TABLE reviews ADD COLUMN review_photo_url VARCHAR(500) NULL;
ALTER TABLE reviews ADD COLUMN is_flagged TINYINT(1) DEFAULT 0;

-- ================= LOYALTY =================
ALTER TABLE loyalty ADD COLUMN total_earned INT DEFAULT 0;
ALTER TABLE loyalty ADD COLUMN total_redeemed INT DEFAULT 0;

-- ================= CONSULTATIONS (fix enums) =================
ALTER TABLE consultations
  MODIFY COLUMN type ENUM('VIRTUAL','IN_PERSON','GENERAL') NOT NULL DEFAULT 'GENERAL',
  MODIFY COLUMN status ENUM('PENDING','RESPONDED','CLOSED') NOT NULL DEFAULT 'PENDING';
ALTER TABLE consultations ADD COLUMN topic ENUM('HAIR','SKIN','MAKEUP','GENERAL') NOT NULL DEFAULT 'GENERAL';
ALTER TABLE consultations ADD COLUMN question TEXT NULL;
ALTER TABLE consultations ADD COLUMN notes TEXT NULL;
ALTER TABLE consultations ADD COLUMN updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP;

-- ================= NEW TABLES =================
CREATE TABLE IF NOT EXISTS professional_availability (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    professional_id BIGINT NOT NULL,
    avail_date      DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    is_booked       TINYINT(1) NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professional(id)
);

CREATE TABLE IF NOT EXISTS group_bookings (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id    BIGINT NOT NULL,
    salon_owner_id BIGINT NULL,
    scheduled_at   DATETIME NOT NULL,
    discount_pct   DECIMAL(5,2) NOT NULL DEFAULT 0,
    status         ENUM('PENDING','CONFIRMED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    notes          TEXT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id)    REFERENCES customer(id),
    FOREIGN KEY (salon_owner_id) REFERENCES salon_owner(id)
);
-- Add columns (without IF NOT EXISTS for older MySQL)
ALTER TABLE appointments ADD COLUMN group_booking_id BIGINT NULL;
ALTER TABLE services ADD COLUMN professional_id BIGINT NULL;
ALTER TABLE services ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1;

----
USE glambook;
UPDATE services SET is_active = 1 WHERE is_active IS NULL;
UPDATE services SET gender = 'WOMEN' WHERE gender IS NULL;

-- ================= PAYMENT & RATING ENFORCEMENT =================

-- Reviews: add appointment FK
ALTER TABLE reviews ADD COLUMN appointment_id BIGINT NULL;
ALTER TABLE reviews ADD CONSTRAINT fk_review_appointment
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL;

-- Appointments: reminder tracking
ALTER TABLE appointments ADD COLUMN reminder_count INT NOT NULL DEFAULT 0;
ALTER TABLE appointments ADD COLUMN last_reminder_sent_at DATETIME NULL;

-- Admin notifications table
CREATE TABLE IF NOT EXISTS admin_notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    message         TEXT NOT NULL,
    reference_id    BIGINT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- Backfill existing appointments with default reminder_count
UPDATE appointments SET reminder_count = 0 WHERE reminder_count IS NULL;

-- Professional availability: add slot_type column
ALTER TABLE professional_availability ADD COLUMN IF NOT EXISTS slot_type VARCHAR(20) NOT NULL DEFAULT 'WORKING';

-- Reviews: add photos, status, updated_at
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS photos JSON NULL;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP;

-- Consultations: add photo support
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500) NULL;

-- ================= CUSTOMER NOTIFICATIONS — add missing enum values =================
ALTER TABLE customer_notifications
  MODIFY COLUMN type ENUM(
    'BOOKING_CONFIRMED','BOOKING_CANCELLED',
    'PAYMENT_SUCCESS','PAYMENT_REFUNDED',
    'REVIEW_RESPONSE','COMMUNICATION_RECEIVED',
    'LOYALTY_POINTS_EARNED','POLICY_UPDATED',
    'PROMOTION_AVAILABLE','CONSULTATION_CONFIRMED',
    'PAYMENT_REMINDER'
  ) NOT NULL;

-- ================= LOYALTY TIER — add PLATINUM =================
ALTER TABLE loyalty
  MODIFY COLUMN tier ENUM('BRONZE','SILVER','GOLD','PLATINUM') NOT NULL DEFAULT 'BRONZE';

-- ================= LOYALTY TRANSACTIONS =================
CREATE TABLE IF NOT EXISTS loyalty_transactions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id    BIGINT NOT NULL,
    type           VARCHAR(10) NOT NULL,   -- EARN or REDEEM
    points         INT NOT NULL,
    description    TEXT NULL,
    appointment_id BIGINT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id)    REFERENCES customer(id) ON DELETE CASCADE,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
);

-- ================= FAVORITES =================
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

-- ================= SLOT STATUS (Daily Schedule Validation) =================
ALTER TABLE professional_availability
  ADD COLUMN IF NOT EXISTS slot_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';

-- ================= PORTFOLIO — metadata columns =================
ALTER TABLE portfolio ADD COLUMN IF NOT EXISTS file_path VARCHAR(500) NULL;
ALTER TABLE portfolio ADD COLUMN IF NOT EXISTS tags VARCHAR(255) NULL;
