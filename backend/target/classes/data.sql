-- Seed Admin (only if not exists)
INSERT INTO admin (username, password)
SELECT 'admin', 'admin123'
WHERE NOT EXISTS (SELECT 1 FROM admin WHERE username = 'admin');

-- Seed Salon Owners (only if not exists)
INSERT INTO salon_owner (name, salon_name, city, email, password, phone)
SELECT 'Ravi Kumar', 'Ravi Salon', 'Visakhapatnam', 'ravi@salon.com', 'owner123', '9000000001'
WHERE NOT EXISTS (SELECT 1 FROM salon_owner WHERE email = 'ravi@salon.com');

INSERT INTO salon_owner (name, salon_name, city, email, password, phone)
SELECT 'Priya Reddy', 'Priya Salon', 'Vijayawada', 'priya@salon.com', 'owner123', '9000000002'
WHERE NOT EXISTS (SELECT 1 FROM salon_owner WHERE email = 'priya@salon.com');

INSERT INTO salon_owner (name, salon_name, city, email, password, phone)
SELECT 'Suresh Rao', 'Suresh Salon', 'Hyderabad', 'suresh@salon.com', 'owner123', '9000000003'
WHERE NOT EXISTS (SELECT 1 FROM salon_owner WHERE email = 'suresh@salon.com');

INSERT INTO salon_owner (name, salon_name, city, email, password, phone)
SELECT 'Anita Sharma', 'Anita Salon', 'Ananthapur', 'anita@salon.com', 'owner123', '9000000004'
WHERE NOT EXISTS (SELECT 1 FROM salon_owner WHERE email = 'anita@salon.com');

INSERT INTO salon_owner (name, salon_name, city, email, password, phone)
SELECT 'Kiran Babu', 'Kiran Salon', 'Khammam', 'kiran@salon.com', 'owner123', '9000000005'
WHERE NOT EXISTS (SELECT 1 FROM salon_owner WHERE email = 'kiran@salon.com');

-- Seed Services (only if table is empty)
INSERT INTO services (name, category, gender, price, duration_mins)
SELECT * FROM (
  SELECT 'Haircut' AS name, 'Hair' AS category, 'MEN' AS gender, 150.00 AS price, 30 AS duration_mins
  UNION ALL SELECT 'Hair Color', 'Hair', 'MEN', 500.00, 60
  UNION ALL SELECT 'Beard Trim', 'Beard', 'MEN', 100.00, 20
  UNION ALL SELECT 'Beard Style', 'Beard', 'MEN', 200.00, 30
  UNION ALL SELECT 'Face Cleanup', 'Skin', 'MEN', 300.00, 45
  UNION ALL SELECT 'Men Package', 'Packages', 'MEN', 700.00, 90
  UNION ALL SELECT 'Haircut', 'Hair', 'WOMEN', 250.00, 45
  UNION ALL SELECT 'Hair Color', 'Hair', 'WOMEN', 800.00, 90
  UNION ALL SELECT 'Facial', 'Skin', 'WOMEN', 500.00, 60
  UNION ALL SELECT 'Manicure', 'Nails', 'WOMEN', 300.00, 45
  UNION ALL SELECT 'Pedicure', 'Nails', 'WOMEN', 350.00, 45
  UNION ALL SELECT 'Bridal Makeup', 'Makeup', 'WOMEN', 3000.00, 120
  UNION ALL SELECT 'Body Massage', 'Body', 'WOMEN', 1000.00, 60
  UNION ALL SELECT 'Kids Haircut', 'Hair', 'KIDS', 100.00, 20
  UNION ALL SELECT 'Kids Grooming', 'Grooming', 'KIDS', 150.00, 30
  UNION ALL SELECT 'Special Styling', 'Special', 'KIDS', 200.00, 30
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM services LIMIT 1);

-- Auto-fix: ensure all services have is_active set (runs on every startup)
UPDATE services SET is_active = 1 WHERE is_active IS NULL;
UPDATE services SET gender = 'WOMEN' WHERE gender IS NULL;
