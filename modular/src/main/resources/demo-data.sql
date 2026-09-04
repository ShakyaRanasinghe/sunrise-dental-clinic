-- =====================================================================
--  Sunrise Dental Clinic — demo data
--
--  Enough to exercise every screen, including the three tables added for
--  medical notes, complaints and reviews.
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
INSERT INTO user_account (uid, username, email, password_hash, display_name, role, active, created_at) VALUES
    ('u-admin', 'admin',      'admin@sunrisedental.lk',      '120000:F5/mkW3ArrZkD4wL+n3IdQ==:e21fp1QzrNg01tsSh2aL7slJJ3vUCC97zPO+AgaHH3k=',  'Anoma Fernando', 'ADMIN',        TRUE, NOW()),
    ('u-recep', 'reception',  'reception@sunrisedental.lk',  '120000:Ry7JwtDQ2FRQ1dH/DHG8Ng==:HLCxZ9k4liUSuXLzzP8Tjfc5Jg/mXOG0PDKcCAQC7aM=',  'Kumari Silva',   'RECEPTIONIST', TRUE, NOW()),
    ('u-dent1', 'silva',      'silva@sunrisedental.lk',      '120000:6u8txaX78YBpXF35aTMIiw==:ib1+vyTMB1gtt6FsCouUn44yqyec9c+hXIKTWIlrSaI=',  'Dr. Ranil Silva','DENTIST',      TRUE, NOW()),
    ('u-dent2', 'jayasuriya', 'jayasuriya@sunrisedental.lk', '120000:mbphz/mRW/Hv58sZ6yYDGA==:azdmsHf3mfsHp71PV8qloweZV4UbxBcLcV6mdAERZ0w=',  'Dr. Malini Jayasuriya', 'DENTIST', TRUE, NOW()),
    ('u-pat1',  NULL,         'nimal@example.lk',            '120000:0jEt7Bx5pU0SFilh3zmVbg==:UudqzcEZZjEwYy5wKnyGghxKtVBEcL1LnGBec9dYCdo=',   'Nimal Perera',   'PATIENT',      TRUE, NOW())
    AS new ON DUPLICATE KEY UPDATE password_hash = new.password_hash, username = new.username;

-- --- Dentists -------------------------------------------------------
INSERT INTO dentist (id, user_uid, name, specialization, phone, consultation_fee, active) VALUES
    ('d-silva',      'u-dent1', 'Dr. Ranil Silva',        'General Dentistry', '+94 77 123 4567', 1500.00, TRUE),
    ('d-jayasuriya', 'u-dent2', 'Dr. Malini Jayasuriya',  'Orthodontics',      '+94 71 987 6543', 2500.00, TRUE)
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

-- GAP-FTB-07: which treatments each dentist offers (toggleable on their dashboard).
INSERT INTO dentist_treatment (dentist_id, treatment_id) VALUES
    ('d-silva', 't-checkup'),
    ('d-silva', 't-scaling'),
    ('d-silva', 't-filling'),
    ('d-silva', 't-extraction'),
    ('d-silva', 't-rootcanal'),
    ('d-silva', 't-whitening'),
    ('d-jayasuriya', 't-checkup'),
    ('d-jayasuriya', 't-rootcanal'),
    ('d-jayasuriya', 't-whitening')
    AS new ON DUPLICATE KEY UPDATE dentist_id = new.dentist_id;

-- --- Patients -------------------------------------------------------
INSERT INTO patient (id, user_uid, name, address, contact_number, email, dob) VALUES
    ('p-nimal',  'u-pat1', 'Nimal Perera',   '14 Galle Road, Colombo 03', '0771234567', 'nimal@example.lk',  '1988-04-12'),
    ('p-sanduni', NULL,    'Sanduni Rathnayake', '8 Temple Lane, Kandy',  '0759876543', 'sanduni@example.lk','1995-11-02'),
    ('p-arun',    NULL,    'Arun Wickrama',  '221 Main Street, Negombo',  '0712223334', NULL,                '1972-01-25')
    AS new ON DUPLICATE KEY UPDATE name = new.name;

-- --- Medical notes, declared by the patient ---------------------------
--  Readable by the treating dentist and the patient only. Two criticals,
--  so the dentist's schedule shows a warning before a record is opened.
INSERT INTO patient_note (id, patient_id, category, detail, critical, created_at, updated_at) VALUES
    ('n-allergy', 'p-nimal', 'ALLERGY',
     'Penicillin — rash and swelling. Confirmed by my GP.', TRUE, NOW(), NOW()),
    ('n-warfarin', 'p-nimal', 'MEDICATION',
     'Warfarin, 3mg daily.', TRUE, NOW(), NOW()),
    ('n-diabetes', 'p-sanduni', 'CONDITION',
     'Type 2 diabetes, diet controlled.', FALSE, NOW(), NOW())
    AS new ON DUPLICATE KEY UPDATE detail = new.detail;

-- --- A concern raised by a patient ------------------------------------
--  Read by the administrator. Never by the dentist named.
INSERT INTO complaint (id, patient_id, dentist_id, appointment_no, category, detail,
                       status, submitted_at) VALUES
    ('c-conduct', 'p-nimal', 'd-silva', NULL, 'CONDUCT',
     'I asked twice for an explanation of the treatment and was told there was no time.',
     'UNDER_REVIEW', NOW()),
    ('c-wait', 'p-nimal', 'd-jayasuriya', NULL, 'WAIT_TIME',
     'Waited fifty minutes past my appointment time with no explanation.',
     'RESOLVED', NOW())
    AS new ON DUPLICATE KEY UPDATE detail = new.detail;

UPDATE complaint
   SET resolution = 'Afternoon clinics re-scheduled with longer gaps. Patient informed.',
       reviewed_by_uid = 'u-admin',
       resolved_at = NOW()
 WHERE id = 'c-wait';

-- --- Reviews ----------------------------------------------------------
--  Left deliberately below the five-review floor, so fn_dentist_rating
--  returns NULL and any screen must render "not enough reviews yet"
--  rather than a mean drawn from a handful of visits  (FR-RVW-12).
--
--  Reviews require a COMPLETED or BILLED appointment, so none are seeded
--  here — the demo appointments are CONFIRMED. Complete one first:
--
--    UPDATE appointment SET status = 'COMPLETED' WHERE appointment_no = '...';
--    INSERT INTO dentist_review (id, appointment_no, dentist_id, patient_id,
--                                rating, comment, submitted_at)
--         VALUES (UUID(), '...', 'd-silva', 'p-nimal', 4,
--                 'Explained everything clearly.', NOW());

-- --- Clinic identity ---------------------------------------------------
--  Seed from the same defaults clinic.properties carries. The admin edits
--  these at runtime via /admin/identity; reception may edit phone only.
INSERT IGNORE INTO clinic_setting (setting_key, setting_value) VALUES
    ('clinic.name',    'Sunrise Dental Clinic'),
    ('clinic.phone',   '+94 11 234 5678'),
    ('clinic.email',   'hello@sunrisedental.lk'),
    ('clinic.address', '123 Galle Road, Colombo 03');
