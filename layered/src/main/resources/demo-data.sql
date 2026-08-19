-- =====================================================================
--  Sunrise Dental Clinic — demo data
--
--  Enough to log in as each of the four roles and walk the whole workflow.
--  Load it after schema.sql:
--      mysql -u root -p sunrise_dental < demo-data.sql
--
--  Every account's password is  Password123
--  The hashes below are PBKDF2 values produced by PasswordHasher; the
--  plaintext appears nowhere in the database.
-- =====================================================================

USE sunrise_dental;

-- --- Accounts -------------------------------------------------------
INSERT INTO user_account (uid, email, password_hash, display_name, role, active, created_at) VALUES
    ('u-admin', 'admin@sunrisedental.lk',      '120000:F5/mkW3ArrZkD4wL+n3IdQ==:e21fp1QzrNg01tsSh2aL7slJJ3vUCC97zPO+AgaHH3k=',  'Anoma Fernando', 'ADMIN',        TRUE, NOW()),
    ('u-recep', 'reception@sunrisedental.lk',  '120000:Ry7JwtDQ2FRQ1dH/DHG8Ng==:HLCxZ9k4liUSuXLzzP8Tjfc5Jg/mXOG0PDKcCAQC7aM=',  'Kumari Silva',   'RECEPTIONIST', TRUE, NOW()),
    ('u-dent1', 'silva@sunrisedental.lk',      '120000:6u8txaX78YBpXF35aTMIiw==:ib1+vyTMB1gtt6FsCouUn44yqyec9c+hXIKTWIlrSaI=',  'Dr. Ranil Silva','DENTIST',      TRUE, NOW()),
    ('u-dent2', 'jayasuriya@sunrisedental.lk', '120000:mbphz/mRW/Hv58sZ6yYDGA==:azdmsHf3mfsHp71PV8qloweZV4UbxBcLcV6mdAERZ0w=',  'Dr. Malini Jayasuriya', 'DENTIST', TRUE, NOW()),
    ('u-pat1',  'nimal@example.lk',            '120000:0jEt7Bx5pU0SFilh3zmVbg==:UudqzcEZZjEwYy5wKnyGghxKtVBEcL1LnGBec9dYCdo=',   'Nimal Perera',   'PATIENT',      TRUE, NOW())
    AS new ON DUPLICATE KEY UPDATE password_hash = new.password_hash;

-- --- Dentists -------------------------------------------------------
INSERT INTO dentist (id, user_uid, name, specialization, consultation_fee, active) VALUES
    ('d-silva',      'u-dent1', 'Dr. Ranil Silva',        'General Dentistry', 1500.00, TRUE),
    ('d-jayasuriya', 'u-dent2', 'Dr. Malini Jayasuriya',  'Orthodontics',      2500.00, TRUE)
    AS new ON DUPLICATE KEY UPDATE name = new.name;

-- --- Treatment catalogue --------------------------------------------
INSERT INTO treatment (id, name, description, base_cost, active) VALUES
    ('t-checkup',   'Routine check-up',   'Examination and advice',              1000.00, TRUE),
    ('t-scaling',   'Scaling & polishing','Removal of plaque and stains',        3500.00, TRUE),
    ('t-filling',   'Composite filling',  'Tooth-coloured restoration',          4500.00, TRUE),
    ('t-extraction','Extraction',         'Simple tooth extraction',             5000.00, TRUE),
    ('t-rootcanal', 'Root canal therapy', 'Endodontic treatment, single visit', 18000.00, TRUE),
    ('t-whitening', 'Teeth whitening',    'In-clinic whitening session',        12000.00, TRUE)
    AS new ON DUPLICATE KEY UPDATE base_cost = new.base_cost;

-- --- Patients -------------------------------------------------------
INSERT INTO patient (id, user_uid, name, address, contact_number, email, dob) VALUES
    ('p-nimal',  'u-pat1', 'Nimal Perera',   '14 Galle Road, Colombo 03', '0771234567', 'nimal@example.lk',  '1988-04-12'),
    ('p-sanduni', NULL,    'Sanduni Rathnayake', '8 Temple Lane, Kandy',  '0759876543', 'sanduni@example.lk','1995-11-02'),
    ('p-arun',    NULL,    'Arun Wickrama',  '221 Main Street, Negombo',  '0712223334', NULL,                '1972-01-25')
    AS new ON DUPLICATE KEY UPDATE name = new.name;
