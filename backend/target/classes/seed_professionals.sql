-- ============================================================
-- GlamBook — Professional Seed Data  (v2)
-- Password: Prof@123  (8 chars, 1 uppercase, 1 digit, 1 special)
-- Specializations match the exact service names in the app.
--
-- Run ONCE after the app has started (salon_owner rows must exist).
-- Usage:
--   USE glambook;
--   SOURCE /path/to/seed_professionals.sql;
--
-- All INSERTs are idempotent (WHERE NOT EXISTS guard).
-- ============================================================

USE glambook;

-- ── Visakhapatnam — Ravi Salon ────────────────────────────────────────────────

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Arjun Reddy', 'arjun.reddy@glambook.com', 'Prof@123', 'Visakhapatnam',
       'Haircut', 5, o.id, 'ACTIVE',
       'Expert in modern haircuts and styling for men.', 1, 0
FROM salon_owner o WHERE o.email = 'ravi@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'arjun.reddy@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Divya Lakshmi', 'divya.lakshmi@glambook.com', 'Prof@123', 'Visakhapatnam',
       'Bridal Makeup', 8, o.id, 'ACTIVE',
       'Specialist in bridal and party makeup with 8 years experience.', 1, 1
FROM salon_owner o WHERE o.email = 'ravi@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'divya.lakshmi@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Karthik Varma', 'karthik.varma@glambook.com', 'Prof@123', 'Visakhapatnam',
       'Hair Color', 6, o.id, 'ACTIVE',
       'Creative colorist specializing in balayage and highlights.', 1, 0
FROM salon_owner o WHERE o.email = 'ravi@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'karthik.varma@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Swathi Nair', 'swathi.nair@glambook.com', 'Prof@123', 'Visakhapatnam',
       'Facial', 4, o.id, 'ACTIVE',
       'Certified skin care therapist offering facials and cleanups.', 1, 1
FROM salon_owner o WHERE o.email = 'ravi@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'swathi.nair@glambook.com');

-- ── Vijayawada — Priya Salon ──────────────────────────────────────────────────

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Ramesh Babu', 'ramesh.babu@glambook.com', 'Prof@123', 'Vijayawada',
       'Beard Trim', 3, o.id, 'ACTIVE',
       'Precision beard shaping and grooming specialist.', 1, 0
FROM salon_owner o WHERE o.email = 'priya@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'ramesh.babu@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Meena Kumari', 'meena.kumari@glambook.com', 'Prof@123', 'Vijayawada',
       'Manicure', 5, o.id, 'ACTIVE',
       'Nail art and manicure expert with a creative touch.', 1, 1
FROM salon_owner o WHERE o.email = 'priya@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'meena.kumari@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Venkat Rao', 'venkat.rao@glambook.com', 'Prof@123', 'Vijayawada',
       'Hair Color', 7, o.id, 'ACTIVE',
       'Award-winning colorist with expertise in all hair types.', 1, 0
FROM salon_owner o WHERE o.email = 'priya@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'venkat.rao@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Lakshmi Devi', 'lakshmi.devi@glambook.com', 'Prof@123', 'Vijayawada',
       'Pedicure', 6, o.id, 'ACTIVE',
       'Foot care and pedicure specialist with relaxing techniques.', 1, 1
FROM salon_owner o WHERE o.email = 'priya@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'lakshmi.devi@glambook.com');

-- ── Hyderabad — Suresh Salon ──────────────────────────────────────────────────

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Anil Kumar', 'anil.kumar@glambook.com', 'Prof@123', 'Hyderabad',
       'Hair Color', 9, o.id, 'ACTIVE',
       'Keratin and hair color expert with 9 years experience.', 1, 0
FROM salon_owner o WHERE o.email = 'suresh@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'anil.kumar@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Pooja Sharma', 'pooja.sharma@glambook.com', 'Prof@123', 'Hyderabad',
       'Bridal Makeup', 5, o.id, 'ACTIVE',
       'Makeup artist for bridal, parties and photoshoots.', 1, 1
FROM salon_owner o WHERE o.email = 'suresh@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'pooja.sharma@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Sunil Verma', 'sunil.verma@glambook.com', 'Prof@123', 'Hyderabad',
       'Face Cleanup', 4, o.id, 'ACTIVE',
       'Scalp treatment and face cleanup specialist.', 1, 0
FROM salon_owner o WHERE o.email = 'suresh@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'sunil.verma@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Kavitha Reddy', 'kavitha.reddy@glambook.com', 'Prof@123', 'Hyderabad',
       'Facial', 6, o.id, 'ACTIVE',
       'Advanced skin care and anti-aging facial treatments.', 1, 1
FROM salon_owner o WHERE o.email = 'suresh@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'kavitha.reddy@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Ravi Teja', 'ravi.teja@glambook.com', 'Prof@123', 'Hyderabad',
       'Beard Style', 3, o.id, 'ACTIVE',
       'Modern beard styling and grooming for men.', 1, 0
FROM salon_owner o WHERE o.email = 'suresh@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'ravi.teja@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Sravani Rao', 'sravani.rao@glambook.com', 'Prof@123', 'Hyderabad',
       'Men Package', 8, o.id, 'ACTIVE',
       'Complete men grooming packages — hair, beard and skin.', 1, 1
FROM salon_owner o WHERE o.email = 'suresh@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'sravani.rao@glambook.com');

-- ── Ananthapur — Anita Salon ──────────────────────────────────────────────────

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Prasad Naidu', 'prasad.naidu@glambook.com', 'Prof@123', 'Ananthapur',
       'Haircut', 4, o.id, 'ACTIVE',
       'Classic and modern haircuts for all ages.', 1, 0
FROM salon_owner o WHERE o.email = 'anita@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'prasad.naidu@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Usha Rani', 'usha.rani@glambook.com', 'Prof@123', 'Ananthapur',
       'Pedicure', 5, o.id, 'ACTIVE',
       'Foot care and pedicure specialist with relaxing techniques.', 1, 1
FROM salon_owner o WHERE o.email = 'anita@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'usha.rani@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Mahesh Goud', 'mahesh.goud@glambook.com', 'Prof@123', 'Ananthapur',
       'Beard Trim', 6, o.id, 'ACTIVE',
       'Complete beard trimming and grooming for men.', 1, 0
FROM salon_owner o WHERE o.email = 'anita@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'mahesh.goud@glambook.com');

-- ── Khammam — Kiran Salon ─────────────────────────────────────────────────────

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Deepika Rao', 'deepika.rao@glambook.com', 'Prof@123', 'Khammam',
       'Bridal Makeup', 10, o.id, 'ACTIVE',
       'Complete bridal makeover specialist with 10 years experience.', 1, 1
FROM salon_owner o WHERE o.email = 'kiran@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'deepika.rao@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Naresh Babu', 'naresh.babu@glambook.com', 'Prof@123', 'Khammam',
       'Hair Color', 5, o.id, 'ACTIVE',
       'Global and highlight coloring expert.', 1, 0
FROM salon_owner o WHERE o.email = 'kiran@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'naresh.babu@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Sirisha Devi', 'sirisha.devi@glambook.com', 'Prof@123', 'Khammam',
       'Manicure', 4, o.id, 'ACTIVE',
       'Creative nail art designs and manicure specialist.', 1, 1
FROM salon_owner o WHERE o.email = 'kiran@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'sirisha.devi@glambook.com');

INSERT INTO professional (name, email, password, city, specialization, experience_years, salon_owner_id, status, bio, is_available_salon, is_available_home)
SELECT 'Vijay Kumar', 'vijay.kumar@glambook.com', 'Prof@123', 'Khammam',
       'Haircut', 7, o.id, 'ACTIVE',
       'Precision haircuts and hair styling specialist.', 1, 0
FROM salon_owner o WHERE o.email = 'kiran@salon.com'
AND NOT EXISTS (SELECT 1 FROM professional WHERE email = 'vijay.kumar@glambook.com');

-- ── Summary ───────────────────────────────────────────────────────────────────
-- Total: 22 professionals across 5 cities
-- Password for ALL: Prof@123  (8 chars, uppercase P, digit 1, special @)
--
-- City          | Count | Professionals
-- Visakhapatnam |  4    | Arjun (Haircut), Divya (Bridal Makeup), Karthik (Hair Color), Swathi (Facial)
-- Vijayawada    |  4    | Ramesh (Beard Trim), Meena (Manicure), Venkat (Hair Color), Lakshmi (Pedicure)
-- Hyderabad     |  6    | Anil (Hair Color), Pooja (Bridal Makeup), Sunil (Face Cleanup),
--               |       | Kavitha (Facial), Ravi Teja (Beard Style), Sravani (Men Package)
-- Ananthapur    |  3    | Prasad (Haircut), Usha (Pedicure), Mahesh (Beard Trim)
-- Khammam       |  4    | Deepika (Bridal Makeup), Naresh (Hair Color), Sirisha (Manicure), Vijay (Haircut)
--
-- Specializations used (all match services in the app):
--   Haircut, Hair Color, Beard Trim, Beard Style, Face Cleanup,
--   Facial, Men Package, Manicure, Pedicure, Bridal Makeup
