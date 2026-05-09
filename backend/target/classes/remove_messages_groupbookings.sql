-- =====================================================
-- Migration: Remove Messages and Group Bookings Features
-- =====================================================

-- Drop foreign key constraint from appointments table
ALTER TABLE appointments DROP FOREIGN KEY IF EXISTS FK5iltr7k9pows18hk8nc101vc1;

-- Remove group_booking_id column from appointments
ALTER TABLE appointments DROP COLUMN IF EXISTS group_booking_id;

-- Drop group_booking_participants table
DROP TABLE IF EXISTS group_booking_participants;

-- Drop group_bookings table
DROP TABLE IF EXISTS group_bookings;

-- Drop communications table
DROP TABLE IF EXISTS communications;

-- Migration completed
