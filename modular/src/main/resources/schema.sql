-- =====================================================================
--  Sunrise Dental Clinic — Appointment & Patient Management System
--  MySQL schema. Mirrors the domain class diagram one table per class.
--
--  14 tables, 16 enforced foreign keys. Documented in docs/er-diagram.md;
--  business rules that a foreign key cannot express are in procedures.sql.
--
--  Run in order:
--    mysql -u root -p < schema.sql
--    mysql -u root -p sunrise_dental < procedures.sql
--    mysql -u root -p sunrise_dental < demo-data.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS sunrise_dental
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sunrise_dental;

-- ---------------------------------------------------------------------
-- Identity
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_account (
    uid             VARCHAR(64)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,   -- PBKDF2: iterations:salt:hash
    display_name    VARCHAR(255),
    role            ENUM('PATIENT','RECEPTIONIST','DENTIST','ADMIN') NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_attempts INT          NOT NULL DEFAULT 0,
    locked          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NULL,
    PRIMARY KEY (uid),
    UNIQUE KEY uq_user_email (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- People
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS patient (
    id             VARCHAR(64)  NOT NULL,
    user_uid       VARCHAR(64),
    name           VARCHAR(255) NOT NULL,
    address        VARCHAR(500),
    contact_number VARCHAR(32),
    email          VARCHAR(255),
    dob            DATE,
    diagnosis_details TEXT NULL,
    PRIMARY KEY (id),
    KEY idx_patient_user (user_uid),
    CONSTRAINT fk_patient_user FOREIGN KEY (user_uid)
        REFERENCES user_account (uid) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- What the patient declares about themselves.
--
-- Readable by the patient and by a dentist treating them; never by
-- reception or the administrator.  The whole table is confidential, not
-- one column of it  (FR-NOTE-11, NFR-SEC-11).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS patient_note (
    id          VARCHAR(64)   NOT NULL,
    patient_id  VARCHAR(64)   NOT NULL,
    category    ENUM('ALLERGY','MEDICATION','CONDITION','OTHER') NOT NULL,
    detail      VARCHAR(1000) NOT NULL,
    critical    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP     NULL,
    updated_at  TIMESTAMP     NULL,
    PRIMARY KEY (id),
    KEY idx_note_patient (patient_id),
    KEY idx_note_critical (patient_id, critical),
    CONSTRAINT fk_note_patient FOREIGN KEY (patient_id)
        REFERENCES patient (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dentist (
    id               VARCHAR(64)    NOT NULL,
    user_uid         VARCHAR(64),
    name             VARCHAR(255)   NOT NULL,
    specialization   VARCHAR(255),
    phone            VARCHAR(64),
    consultation_fee DECIMAL(10, 2) NOT NULL DEFAULT 1500.00,
    active           BOOLEAN        NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    KEY idx_dentist_user (user_uid),
    CONSTRAINT fk_dentist_user FOREIGN KEY (user_uid)
        REFERENCES user_account (uid) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Catalogue
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS treatment (
    id          VARCHAR(64)    NOT NULL,
    name        VARCHAR(255)   NOT NULL,
    description VARCHAR(1000),
    base_cost   DECIMAL(10, 2) NOT NULL,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- GAP-FTB-07: which treatments each dentist offers. A dentist switches their
-- list on/off, and the booking screen offers a patient only what that dentist
-- actually performs.
CREATE TABLE IF NOT EXISTS dentist_treatment (
    dentist_id   VARCHAR(64) NOT NULL,
    treatment_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (dentist_id, treatment_id),
    CONSTRAINT fk_dt_dentist FOREIGN KEY (dentist_id) REFERENCES dentist (id) ON DELETE CASCADE,
    CONSTRAINT fk_dt_treatment FOREIGN KEY (treatment_id) REFERENCES treatment (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Scheduling
--   dentist_session is the published availability window;
--   slot is the fixed-length bookable unit exploded from it (composition —
--   ON DELETE CASCADE encodes "a slot cannot outlive its session").
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dentist_session (
    id                    VARCHAR(64) NOT NULL,
    dentist_id            VARCHAR(64) NOT NULL,
    session_date          DATE        NOT NULL,
    start_time            TIME        NOT NULL,
    end_time              TIME        NOT NULL,
    slot_duration_minutes INT         NOT NULL DEFAULT 30,
    published_by_uid      VARCHAR(64),
    PRIMARY KEY (id),
    KEY idx_session_dentist_date (dentist_id, session_date),
    KEY idx_session_publisher (published_by_uid),
    CONSTRAINT fk_session_dentist FOREIGN KEY (dentist_id)
        REFERENCES dentist (id) ON DELETE CASCADE,
    CONSTRAINT fk_session_publisher FOREIGN KEY (published_by_uid)
        REFERENCES user_account (uid) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS slot (
    id               VARCHAR(128) NOT NULL,   -- dentistId_date_startTime
    session_id       VARCHAR(64)  NOT NULL,
    dentist_id       VARCHAR(64)  NOT NULL,
    slot_date        DATE         NOT NULL,
    start_time       TIME         NOT NULL,
    duration_minutes INT          NOT NULL DEFAULT 30,
    status           ENUM('OPEN','BOOKED') NOT NULL DEFAULT 'OPEN',
    appointment_no   VARCHAR(32),
    PRIMARY KEY (id),
    KEY idx_slot_dentist_date (dentist_id, slot_date),
    KEY idx_slot_date_status (slot_date, status),
    -- A slot may back at most one appointment. This UNIQUE key is the
    -- last-resort database guarantee against double booking, on top of the
    -- SELECT ... FOR UPDATE taken by the booking transaction.
    UNIQUE KEY uq_slot_appointment (appointment_no),
    CONSTRAINT fk_slot_session FOREIGN KEY (session_id)
        REFERENCES dentist_session (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Clinical + financial records
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointment (
    appointment_no   VARCHAR(32) NOT NULL,    -- APT-yyyymmdd-####
    patient_id       VARCHAR(64) NOT NULL,
    dentist_id       VARCHAR(64) NOT NULL,
    slot_id          VARCHAR(128) NOT NULL,
    treatment_id     VARCHAR(64),
    appointment_date DATE        NOT NULL,
    appointment_time TIME        NOT NULL,
    status           ENUM('CONFIRMED','COMPLETED','BILLED','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    diagnosis        TEXT,                    -- CONFIDENTIAL: dentist + patient only
    patient_reason   TEXT,                    -- GAP-FTB-06: the reason a patient states when
                                              -- they book "Other (describe…)" rather than a
                                              -- listed procedure. NULL for a named treatment.
    custom_price     DECIMAL(10, 2) NULL,     -- GAP-DEN-13: the price the dentist enters when
                                              -- completing a visit with no catalog treatment.
                                              -- NULL for a named treatment (catalog rules).
    created_by_uid   VARCHAR(64),
    created_by_role  ENUM('PATIENT','RECEPTIONIST','DENTIST','ADMIN'),
    created_at       TIMESTAMP   NULL,
    PRIMARY KEY (appointment_no),
    KEY idx_appt_patient (patient_id),
    KEY idx_appt_dentist_date (dentist_id, appointment_date),
    KEY idx_appt_date (appointment_date),
    KEY idx_appt_creator (created_by_uid),
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_appt_dentist FOREIGN KEY (dentist_id) REFERENCES dentist (id),
    CONSTRAINT fk_appt_treatment FOREIGN KEY (treatment_id) REFERENCES treatment (id),
    CONSTRAINT fk_appt_creator FOREIGN KEY (created_by_uid)
        REFERENCES user_account (uid) ON DELETE SET NULL
    -- slot_id is deliberately unconstrained: appointment.slot_id and
    -- slot.appointment_no reference each other, so a foreign key in both
    -- directions makes the insert impossible. UNIQUE (slot.appointment_no)
    -- plus SELECT ... FOR UPDATE is the stronger guard - see er-diagram.md
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bill (
    id                   VARCHAR(64)    NOT NULL,
    appointment_no       VARCHAR(32)    NOT NULL,
    patient_id           VARCHAR(64)    NOT NULL,
    dentist_id           VARCHAR(64)    NOT NULL,
    receptionist_uid     VARCHAR(64),
    consultation_fee     DECIMAL(10, 2) NOT NULL DEFAULT 0,
    treatment_cost       DECIMAL(10, 2) NOT NULL DEFAULT 0,
    service_charge       DECIMAL(10, 2) NOT NULL DEFAULT 0,
    discount             DECIMAL(10, 2) NOT NULL DEFAULT 0,
    tax                  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total                DECIMAL(10, 2) NOT NULL DEFAULT 0,
    dentist_earning      DECIMAL(10, 2) NOT NULL DEFAULT 0,
    clinic_earning       DECIMAL(10, 2) NOT NULL DEFAULT 0,
    receptionist_earning DECIMAL(10, 2) NOT NULL DEFAULT 0,
    issued_at            TIMESTAMP      NULL,
    issued_by_uid        VARCHAR(64),
    PRIMARY KEY (id),
    -- One bill per appointment (composition in the class diagram).
    UNIQUE KEY uq_bill_appointment (appointment_no),
    KEY idx_bill_receptionist (receptionist_uid),
    CONSTRAINT fk_bill_appointment FOREIGN KEY (appointment_no)
        REFERENCES appointment (appointment_no) ON DELETE CASCADE,
    CONSTRAINT fk_bill_patient FOREIGN KEY (patient_id)
        REFERENCES patient (id),
    CONSTRAINT fk_bill_dentist FOREIGN KEY (dentist_id)
        REFERENCES dentist (id),
    CONSTRAINT fk_bill_receptionist FOREIGN KEY (receptionist_uid)
        REFERENCES user_account (uid) ON DELETE SET NULL,
    CONSTRAINT fk_bill_issuer FOREIGN KEY (issued_by_uid)
        REFERENCES user_account (uid) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notification (
    id             VARCHAR(64) NOT NULL,
    appointment_no VARCHAR(32) NOT NULL,
    channel        ENUM('EMAIL','SMS') NOT NULL,
    recipient      VARCHAR(255),
    subject        VARCHAR(255),
    body           TEXT,
    status         ENUM('SENT','FAILED','LOGGED') NOT NULL,
    sent_at        TIMESTAMP   NULL,
    PRIMARY KEY (id),
    KEY idx_notification_appointment (appointment_no),
    CONSTRAINT fk_notification_appointment FOREIGN KEY (appointment_no)
        REFERENCES appointment (appointment_no) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Audit trail — append-only
-- ---------------------------------------------------------------------
-- ---------------------------------------------------------------------
-- A concern a patient raises about a dentist.
--
-- Read by the administrator and never by the dentist named  (FR-CMP-08).
-- appointment_no is optional and SET NULL, so a concern outlives a tidied
-- appointment  (FR-DAT-06).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS complaint (
    id              VARCHAR(64)   NOT NULL,
    patient_id      VARCHAR(64),           -- GAP-PAT-32: NULL for an anonymous general
    dentist_id      VARCHAR(64),           -- concern naming no dentist. Named concerns
                                           -- always carry both, as before.
    appointment_no  VARCHAR(32),
    category        ENUM('CONDUCT','CLINICAL_CONCERN','WAIT_TIME','BILLING','OTHER') NOT NULL,
    detail          TEXT          NOT NULL,
    status          ENUM('SUBMITTED','UNDER_REVIEW','RESOLVED','DISMISSED')
                    NOT NULL DEFAULT 'SUBMITTED',
    submitted_at    TIMESTAMP     NULL,
    reviewed_by_uid VARCHAR(64),
    resolution      VARCHAR(2000),
    resolved_at     TIMESTAMP     NULL,
    PRIMARY KEY (id),
    KEY idx_complaint_patient (patient_id),
    KEY idx_complaint_dentist (dentist_id),
    KEY idx_complaint_status (status),
    KEY idx_complaint_reviewer (reviewed_by_uid),
    CONSTRAINT fk_complaint_reviewer FOREIGN KEY (reviewed_by_uid)
        REFERENCES user_account (uid) ON DELETE SET NULL,
    CONSTRAINT fk_complaint_patient FOREIGN KEY (patient_id)
        REFERENCES patient (id) ON DELETE CASCADE,
    CONSTRAINT fk_complaint_dentist FOREIGN KEY (dentist_id)
        REFERENCES dentist (id),
    CONSTRAINT fk_complaint_appointment FOREIGN KEY (appointment_no)
        REFERENCES appointment (appointment_no) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- A rating out of five for one visit.
--
-- UNIQUE on appointment_no is what enforces one review per visit
-- (FR-DAT-07) - a check in application code could be raced.
-- The dentist reads an aggregate of these rows and never a row
-- (FR-RVW-08, NFR-SEC-13).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dentist_review (
    id             VARCHAR(64)   NOT NULL,
    appointment_no VARCHAR(32)   NOT NULL,
    dentist_id     VARCHAR(64)   NOT NULL,
    patient_id     VARCHAR(64)   NOT NULL,
    rating         TINYINT       NOT NULL,
    comment        VARCHAR(1000),
    submitted_at   TIMESTAMP     NULL,
    updated_at     TIMESTAMP     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_review_appointment (appointment_no),
    KEY idx_review_dentist (dentist_id),
    CONSTRAINT fk_review_appointment FOREIGN KEY (appointment_no)
        REFERENCES appointment (appointment_no) ON DELETE CASCADE,
    CONSTRAINT fk_review_dentist FOREIGN KEY (dentist_id)
        REFERENCES dentist (id),
    CONSTRAINT fk_review_patient FOREIGN KEY (patient_id)
        REFERENCES patient (id) ON DELETE CASCADE,
    CONSTRAINT ck_review_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_event (
    id          VARCHAR(64) NOT NULL,
    actor_uid   VARCHAR(64),
    actor_role  ENUM('PATIENT','RECEPTIONIST','DENTIST','ADMIN'),
    action      VARCHAR(64) NOT NULL,
    target_type VARCHAR(64),
    target_id   VARCHAR(128),
    event_time  TIMESTAMP   NULL,
    PRIMARY KEY (id),
    KEY idx_audit_target (target_type, target_id),
    KEY idx_audit_time (event_time),
    KEY idx_audit_actor (actor_uid),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_uid)
        REFERENCES user_account (uid) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Daily counter backing the Singleton AppointmentNumberGenerator.
-- Keeping it in the database means the sequence stays unique even if the
-- application is restarted or run on more than one node.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointment_counter (
    day_key   CHAR(8) NOT NULL,   -- yyyyMMdd
    counter_value INT NOT NULL DEFAULT 0,
    PRIMARY KEY (day_key)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Clinic identity: name, phone, email, address — editable at runtime
-- by the administrator (all fields) or receptionist (phone only).
-- Falls back to clinic.properties defaults when the table is empty.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clinic_setting (
    setting_key   VARCHAR(64)  NOT NULL,
    setting_value VARCHAR(500) NOT NULL DEFAULT '',
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB;
