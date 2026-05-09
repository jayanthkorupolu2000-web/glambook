package com.salon.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs one-time DDL fixes on startup that Hibernate ddl-auto=update cannot handle
 * (e.g. expanding MySQL ENUM columns to VARCHAR so new enum values are accepted).
 * Each statement is wrapped in its own try/catch so a single failure doesn't
 * prevent the application from starting.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        // Fix 1: customer_notifications.type — change from ENUM to VARCHAR(50)
        // so any CustomerNotificationType value (including PAYMENT_REMINDER) is accepted.
        runSilently(
            "ALTER TABLE customer_notifications MODIFY COLUMN type VARCHAR(50) NOT NULL",
            "customer_notifications.type → VARCHAR(50)"
        );

        // Fix 2: loyalty.tier — change from ENUM to VARCHAR(20)
        // so PLATINUM (added later) is accepted without a schema change.
        runSilently(
            "ALTER TABLE loyalty MODIFY COLUMN tier VARCHAR(20) NOT NULL DEFAULT 'BRONZE'",
            "loyalty.tier → VARCHAR(20)"
        );

        // Fix 3: owner_notifications.type — change from ENUM to VARCHAR(50)
        runSilently(
            "ALTER TABLE owner_notifications MODIFY COLUMN type VARCHAR(50) NOT NULL",
            "owner_notifications.type → VARCHAR(50)"
        );

        // Fix 4: professional_notifications.type — change from ENUM to VARCHAR(50)
        runSilently(
            "ALTER TABLE professional_notifications MODIFY COLUMN type VARCHAR(50) NOT NULL",
            "professional_notifications.type → VARCHAR(50)"
        );

        // Fix 5: services.gender — add column if missing (fresh installs from old schema)
        runSilently(
            "ALTER TABLE services ADD COLUMN gender ENUM('MEN','WOMEN','KIDS') NOT NULL DEFAULT 'WOMEN'",
            "services.gender column"
        );

        // Fix 6: services.professional_id — add column if missing
        runSilently(
            "ALTER TABLE services ADD COLUMN professional_id BIGINT NULL",
            "services.professional_id column"
        );

        // Fix 7: services.is_active — add column if missing
        runSilently(
            "ALTER TABLE services ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1",
            "services.is_active column"
        );

        // Fix 8: appointments.reminder_count — add column if missing
        runSilently(
            "ALTER TABLE appointments ADD COLUMN reminder_count INT NOT NULL DEFAULT 0",
            "appointments.reminder_count column"
        );

        // Fix 9: appointments.last_reminder_sent_at — add column if missing
        runSilently(
            "ALTER TABLE appointments ADD COLUMN last_reminder_sent_at DATETIME NULL",
            "appointments.last_reminder_sent_at column"
        );

        // Fix 9b: professional_availability.slot_status — add column if missing
        runSilently(
            "ALTER TABLE professional_availability ADD COLUMN slot_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'",
            "professional_availability.slot_status column"
        );

        // Fix 9e: portfolio.file_path and portfolio.tags — metadata-only columns
        runSilently("ALTER TABLE portfolio ADD COLUMN file_path VARCHAR(500) NULL", "portfolio.file_path");
        runSilently("ALTER TABLE portfolio ADD COLUMN tags VARCHAR(255) NULL", "portfolio.tags");

        // Fix 9c: beauty_profile.hair_texture — add column if missing
        runSilently(
            "ALTER TABLE beauty_profile ADD COLUMN hair_texture VARCHAR(50) NULL",
            "beauty_profile.hair_texture column"
        );

        // Fix 9d: beauty_profile.notes — add column if missing
        runSilently(
            "ALTER TABLE beauty_profile ADD COLUMN notes TEXT NULL",
            "beauty_profile.notes column"
        );

        // Fix 10: reviews.appointment_id — add column if missing
        runSilently(
            "ALTER TABLE reviews ADD COLUMN appointment_id BIGINT NULL",
            "reviews.appointment_id column"
        );

        // Fix 11: reviews.photos — add JSON column if missing
        runSilently(
            "ALTER TABLE reviews ADD COLUMN photos JSON NULL",
            "reviews.photos column"
        );

        // Fix 12: reviews.status — add column if missing
        runSilently(
            "ALTER TABLE reviews ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'",
            "reviews.status column"
        );

        // Fix 13: consultations — add missing columns
        runSilently("ALTER TABLE consultations ADD COLUMN topic VARCHAR(20) NOT NULL DEFAULT 'GENERAL'", "consultations.topic");
        runSilently("ALTER TABLE consultations ADD COLUMN question TEXT NULL", "consultations.question");
        runSilently("ALTER TABLE consultations ADD COLUMN notes TEXT NULL", "consultations.notes");
        runSilently("ALTER TABLE consultations ADD COLUMN photo_url VARCHAR(500) NULL", "consultations.photo_url");
        runSilently("ALTER TABLE consultations ADD COLUMN updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP", "consultations.updated_at");

        // Fix 14: favorite tables
        runSilently("""
            CREATE TABLE IF NOT EXISTS favorite_products (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                customer_id BIGINT NOT NULL,
                product_id BIGINT NOT NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uq_fav_product (customer_id, product_id),
                FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
                FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
            )""", "favorite_products table");

        runSilently("""
            CREATE TABLE IF NOT EXISTS favorite_services (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                customer_id BIGINT NOT NULL,
                service_id BIGINT NOT NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uq_fav_service (customer_id, service_id),
                FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
                FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE
            )""", "favorite_services table");

        // Fix 15: loyalty_transactions table
        runSilently("""
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
            )""", "loyalty_transactions table");

        // Fix 16: admin_notifications table
        runSilently("""
            CREATE TABLE IF NOT EXISTS admin_notifications (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                message TEXT NOT NULL,
                reference_id BIGINT NULL,
                is_read BOOLEAN NOT NULL DEFAULT FALSE,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            )""", "admin_notifications table");
    }

    private void runSilently(String sql, String description) {
        try {
            jdbc.execute(sql);
            log.info("DatabaseMigrationRunner: applied — {}", description);
        } catch (Exception e) {
            // Column may already be VARCHAR — safe to ignore
            log.debug("DatabaseMigrationRunner: skipped ({}): {}", description, e.getMessage());
        }
    }
}
