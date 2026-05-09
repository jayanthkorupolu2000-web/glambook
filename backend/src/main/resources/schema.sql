CREATE TABLE IF NOT EXISTS admin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS salon_owner (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    salon_name VARCHAR(150) NOT NULL,
    city VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS professional (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    name              VARCHAR(100) NOT NULL,
    email             VARCHAR(100) UNIQUE NOT NULL,
    password          VARCHAR(255) NOT NULL,
    city              VARCHAR(50) NOT NULL,
    specialization    VARCHAR(255),
    experience_years  INT DEFAULT 0,
    salon_owner_id    BIGINT NOT NULL,
    status            ENUM('PENDING','ACTIVE','SUSPENDED') NOT NULL DEFAULT 'PENDING',
    approved_by       BIGINT NULL,
    approved_at       DATETIME NULL,
    profile_photo_url VARCHAR(500) NULL,
    certifications    TEXT NULL,
    training_details  TEXT NULL,
    service_areas     TEXT NULL,
    travel_radius_km  INT NULL DEFAULT 0,
    response_time_hrs INT NULL DEFAULT 24,
    bio               TEXT NULL,
    instagram_handle  VARCHAR(100) NULL,
    is_available_home  TINYINT(1) NOT NULL DEFAULT 0,
    is_available_salon TINYINT(1) NOT NULL DEFAULT 1,
    FOREIGN KEY (salon_owner_id) REFERENCES salon_owner(id),
    FOREIGN KEY (approved_by) REFERENCES salon_owner(id)
);

CREATE TABLE IF NOT EXISTS customer (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    name             VARCHAR(100) NOT NULL,
    email            VARCHAR(100) UNIQUE NOT NULL,
    password         VARCHAR(255) NOT NULL,
    phone            VARCHAR(20),
    city             VARCHAR(50),
    profile_picture  VARCHAR(255),
    status           ENUM('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    cancel_count     INT NOT NULL DEFAULT 0,
    date_of_birth    DATE NULL,
    profile_photo_url VARCHAR(500) NULL,
    emergency_contact VARCHAR(255) NULL,
    medical_notes    TEXT NULL,
    referral_code    VARCHAR(20) UNIQUE NULL,
    referred_by      VARCHAR(20) NULL,
    preferences      TEXT NULL,
    reminder_opt_in  TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS services (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    category      VARCHAR(100) NULL,
    gender        ENUM('MEN','WOMEN','KIDS') NOT NULL DEFAULT 'WOMEN',
    price         DECIMAL(10,2) NOT NULL,
    duration_mins INT NOT NULL DEFAULT 30,
    target_group  ENUM('MEN','WOMEN','KIDS') NOT NULL DEFAULT 'WOMEN',
    is_active     TINYINT(1) NOT NULL DEFAULT 1,
    discount_pct  DECIMAL(5,2) NULL DEFAULT 0,
    professional_id BIGINT NULL
);

CREATE TABLE IF NOT EXISTS resources (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id     BIGINT NOT NULL,
    type         ENUM('ROOM','EQUIPMENT') NOT NULL,
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR(500),
    is_available TINYINT(1) NOT NULL DEFAULT 1,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES salon_owner(id)
);

CREATE TABLE IF NOT EXISTS resource_availability (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    avail_date  DATE NOT NULL,
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    is_booked   TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (resource_id) REFERENCES resources(id),
    CONSTRAINT uq_resource_slot UNIQUE (resource_id, avail_date, start_time)
);

CREATE TABLE IF NOT EXISTS group_bookings (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    organizer_id      BIGINT NOT NULL,
    salon_owner_id    BIGINT NOT NULL,
    title             VARCHAR(255) NOT NULL,
    event_date        DATE NOT NULL,
    participant_count INT NOT NULL DEFAULT 1,
    discount_pct      DECIMAL(5,2) NOT NULL DEFAULT 0,
    payment_mode      ENUM('INDIVIDUAL','GROUP') NOT NULL DEFAULT 'INDIVIDUAL',
    status            ENUM('DRAFT','CONFIRMED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'DRAFT',
    notes             TEXT NULL,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organizer_id)   REFERENCES customer(id),
    FOREIGN KEY (salon_owner_id) REFERENCES salon_owner(id)
);

CREATE TABLE IF NOT EXISTS appointments (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id           BIGINT NOT NULL,
    professional_id       BIGINT NULL,
    service_id            BIGINT NOT NULL,
    date_time             DATETIME NOT NULL,
    status                ENUM('PENDING','CONFIRMED','COMPLETED','CANCELLED') DEFAULT 'PENDING',
    cancelled_at          DATETIME NULL,
    resource_id           BIGINT NULL,
    reminder_sent_at      DATETIME NULL,
    home_address          TEXT NULL,
    home_access_notes     TEXT NULL,
    travel_fee            DECIMAL(8,2) NULL DEFAULT 0,
    group_booking_id      BIGINT NULL,
    rebooked_from_id      BIGINT NULL,
    reminder_count        INT NOT NULL DEFAULT 0,
    last_reminder_sent_at DATETIME NULL,
    FOREIGN KEY (customer_id)      REFERENCES customer(id),
    FOREIGN KEY (professional_id)  REFERENCES professional(id),
    FOREIGN KEY (service_id)       REFERENCES services(id),
    FOREIGN KEY (resource_id)      REFERENCES resources(id)
);

CREATE TABLE IF NOT EXISTS group_booking_participants (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_booking_id BIGINT NOT NULL,
    customer_id      BIGINT NULL,
    guest_name       VARCHAR(255) NULL,
    service_id       BIGINT NOT NULL,
    appointment_id   BIGINT NULL,
    individual_notes TEXT NULL,
    FOREIGN KEY (group_booking_id) REFERENCES group_bookings(id),
    FOREIGN KEY (customer_id)      REFERENCES customer(id),
    FOREIGN KEY (service_id)       REFERENCES services(id),
    FOREIGN KEY (appointment_id)   REFERENCES appointments(id)
);

CREATE TABLE IF NOT EXISTS payments (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    method         ENUM('CASH','CARD','UPI','WALLET','BANK_TRANSFER') NOT NULL DEFAULT 'UPI',
    status         ENUM('PENDING','PAID','REFUNDED') DEFAULT 'PENDING',
    paid_at        DATETIME,
    payment_type   ENUM('DEPOSIT','FULL','REFUND') NOT NULL DEFAULT 'FULL',
    transaction_id VARCHAR(255) NULL,
    receipt_url    VARCHAR(500) NULL,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);

CREATE TABLE IF NOT EXISTS reviews (
    id                        BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id               BIGINT NOT NULL,
    professional_id           BIGINT NOT NULL,
    appointment_id            BIGINT NULL,
    rating                    TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment                   TEXT,
    photos                    JSON NULL,
    status                    VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at                DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    professional_response     TEXT NULL,
    professional_response_at  DATETIME NULL,
    is_flagged                TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (customer_id)     REFERENCES customer(id),
    FOREIGN KEY (professional_id) REFERENCES professional(id)
);

CREATE TABLE IF NOT EXISTS complaints (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id        BIGINT NOT NULL,
    professional_id    BIGINT NOT NULL,
    description        TEXT NOT NULL,
    feedback           ENUM('POOR','AVERAGE','GOOD','BETTER') NOT NULL,
    rating             INT NOT NULL,
    status             ENUM('OPEN','FORWARDED','RESOLVED') NOT NULL DEFAULT 'OPEN',
    resolution_notes   TEXT,
    owner_action_notes TEXT NULL,
    owner_action_at    DATETIME NULL,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id)     REFERENCES customer(id),
    FOREIGN KEY (professional_id) REFERENCES professional(id)
);

CREATE TABLE IF NOT EXISTS policy (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    content      TEXT NOT NULL,
    published_by BIGINT NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (published_by) REFERENCES admin(id)
);

CREATE TABLE IF NOT EXISTS promotions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id     BIGINT NOT NULL,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    discount_pct DECIMAL(5,2) NOT NULL,
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL,
    is_active    TINYINT(1) NOT NULL DEFAULT 1,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES salon_owner(id)
);

CREATE TABLE IF NOT EXISTS loyalty (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id    BIGINT NOT NULL UNIQUE,
    owner_id       BIGINT NOT NULL,
    points         INT NOT NULL DEFAULT 0,
    tier           VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    total_earned   INT NOT NULL DEFAULT 0,
    total_redeemed INT NOT NULL DEFAULT 0,
    FOREIGN KEY (customer_id) REFERENCES customer(id),
    FOREIGN KEY (owner_id)    REFERENCES salon_owner(id)
);

CREATE TABLE IF NOT EXISTS loyalty_redemptions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    loyalty_id      BIGINT NOT NULL,
    customer_id     BIGINT NOT NULL,
    points_redeemed INT NOT NULL,
    discount_amount DECIMAL(8,2) NOT NULL,
    appointment_id  BIGINT NULL,
    redeemed_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (loyalty_id)     REFERENCES loyalty(id),
    FOREIGN KEY (customer_id)    REFERENCES customer(id),
    FOREIGN KEY (appointment_id) REFERENCES appointments(id)
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

CREATE TABLE IF NOT EXISTS salon_policy (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id   BIGINT NOT NULL,
    title      VARCHAR(255) NOT NULL,
    content    TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES salon_owner(id)
);

CREATE TABLE IF NOT EXISTS owner_notifications (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id     BIGINT NOT NULL,
    type         VARCHAR(50) NOT NULL,
    reference_id BIGINT,
    message      VARCHAR(500) NOT NULL,
    is_read      TINYINT(1) NOT NULL DEFAULT 0,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES salon_owner(id)
);

CREATE TABLE IF NOT EXISTS portfolio (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    professional_id  BIGINT NOT NULL,
    service_id       BIGINT NULL,
    media_type       ENUM('BEFORE_AFTER_PHOTO','VIDEO_CLIP','SINGLE_PHOTO') NOT NULL,
    before_photo_url VARCHAR(500) NULL,
    after_photo_url  VARCHAR(500) NULL,
    photo_url        VARCHAR(500) NULL,
    video_url        VARCHAR(500) NULL,
    video_thumbnail  VARCHAR(500) NULL,
    service_tag      VARCHAR(255) NULL,
    caption          VARCHAR(500) NULL,
    testimonial      TEXT NULL,
    is_featured      TINYINT(1) NOT NULL DEFAULT 0,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professional(id),
    FOREIGN KEY (service_id)      REFERENCES services(id)
);

CREATE TABLE IF NOT EXISTS service_before_after (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_id       BIGINT NOT NULL,
    professional_id  BIGINT NOT NULL,
    before_photo_url VARCHAR(500) NOT NULL,
    after_photo_url  VARCHAR(500) NOT NULL,
    caption          VARCHAR(500) NULL,
    uploaded_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (service_id)      REFERENCES services(id),
    FOREIGN KEY (professional_id) REFERENCES professional(id)
);

CREATE TABLE IF NOT EXISTS communications (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    professional_id  BIGINT NOT NULL,
    customer_id      BIGINT NOT NULL,
    appointment_id   BIGINT NULL,
    message          TEXT NOT NULL,
    type             ENUM('REMINDER','AFTERCARE','FOLLOWUP','GENERAL') NOT NULL,
    is_read          TINYINT(1) NOT NULL DEFAULT 0,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professional(id),
    FOREIGN KEY (customer_id)     REFERENCES customer(id),
    FOREIGN KEY (appointment_id)  REFERENCES appointments(id)
);

CREATE TABLE IF NOT EXISTS professional_notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    professional_id BIGINT NOT NULL,
    type            VARCHAR(50) NOT NULL,
    reference_id    BIGINT NULL,
    message         VARCHAR(500) NOT NULL,
    is_read         TINYINT(1) NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professional(id)
);

CREATE TABLE IF NOT EXISTS beauty_profile (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id            BIGINT NOT NULL UNIQUE,
    skin_type              ENUM('OILY','DRY','COMBINATION','SENSITIVE','NORMAL') NULL,
    hair_type              ENUM('STRAIGHT','WAVY','CURLY','COILY') NULL,
    hair_condition         ENUM('HEALTHY','DAMAGED','COLOURED','CHEMICALLY_TREATED') NULL,
    allergies              TEXT NULL,
    preferred_services     TEXT NULL,
    beauty_goals           TEXT NULL,
    consultation_photo_url VARCHAR(500) NULL,
    updated_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE TABLE IF NOT EXISTS products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    brand           VARCHAR(255) NOT NULL,
    category        VARCHAR(100) NOT NULL,
    description     TEXT NULL,
    ingredients     TEXT NULL,
    usage_tips      TEXT NULL,
    price           DECIMAL(10,2) NOT NULL,
    stock           INT NOT NULL DEFAULT 0,
    image_url       VARCHAR(500) NULL,
    recommended_for TEXT NULL,
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_recommendations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    professional_id BIGINT NOT NULL,
    customer_id     BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    appointment_id  BIGINT NULL,
    note            TEXT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professional(id),
    FOREIGN KEY (customer_id)     REFERENCES customer(id),
    FOREIGN KEY (product_id)      REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id      BIGINT NOT NULL,
    total_amount     DECIMAL(10,2) NOT NULL,
    payment_method   ENUM('CARD','UPI','WALLET','BANK_TRANSFER') NOT NULL,
    payment_status   ENUM('PENDING','PAID','FAILED') NOT NULL DEFAULT 'PENDING',
    delivery_status  ENUM('PROCESSING','SHIPPED','DELIVERED','CANCELLED') NOT NULL DEFAULT 'PROCESSING',
    delivery_address TEXT NOT NULL,
    estimated_delivery DATE NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id)   REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS consultations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    professional_id BIGINT NULL,
    appointment_id  BIGINT NULL,
    type        ENUM('VIRTUAL','IN_PERSON','GENERAL') NOT NULL DEFAULT 'GENERAL',
    topic       VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    status      ENUM('PENDING','RESPONDED','CLOSED') NOT NULL DEFAULT 'PENDING',
    question    TEXT NULL,
    notes       TEXT NULL,
    photo_url   VARCHAR(500) NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id)     REFERENCES customer(id),
    FOREIGN KEY (professional_id) REFERENCES professional(id),
    FOREIGN KEY (appointment_id)  REFERENCES appointments(id)
);

CREATE TABLE IF NOT EXISTS customer_notifications (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id  BIGINT NOT NULL,
    type         VARCHAR(50) NOT NULL,
    reference_id BIGINT NULL,
    message      VARCHAR(500) NOT NULL,
    is_read      TINYINT(1) NOT NULL DEFAULT 0,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE TABLE IF NOT EXISTS professional_availability (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    professional_id BIGINT NOT NULL,
    avail_date      DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    is_booked       TINYINT(1) NOT NULL DEFAULT 0,
    slot_type       VARCHAR(20) NOT NULL DEFAULT 'WORKING',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professional(id)
);

CREATE TABLE IF NOT EXISTS admin_notifications (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    message      TEXT NOT NULL,
    reference_id BIGINT NULL,
    is_read      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
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
