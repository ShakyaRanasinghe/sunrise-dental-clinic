-- =====================================================================
--  Sunrise Dental Clinic — stored procedures, functions and triggers
--
--  Business rules that belong in the database because a foreign key
--  cannot express them, or because the guarantee must hold however the
--  row arrives — from the application, from a script, or from a console.
--
--  Load after schema.sql:
--      mysql -u root -p sunrise_dental < procedures.sql
--
--  Requirements: FR-DAT-03, FR-DAT-04, FR-APT-04, FR-BIL-01, FR-BIL-05,
--                FR-AUD-01, FR-RVW-04
-- =====================================================================

USE sunrise_dental;

DROP PROCEDURE IF EXISTS sp_register_appointment;
DROP FUNCTION  IF EXISTS fn_calculate_bill;
DROP FUNCTION  IF EXISTS fn_dentist_rating;
DROP TRIGGER   IF EXISTS trg_prevent_double_booking;
DROP TRIGGER   IF EXISTS trg_check_receptionist_role;
DROP TRIGGER   IF EXISTS trg_bill_requires_completion;
DROP TRIGGER   IF EXISTS trg_review_requires_completion;
DROP TRIGGER   IF EXISTS trg_audit_appointment_status;

DELIMITER $$

-- ---------------------------------------------------------------------
--  fn_calculate_bill(treatment_id, consultation_fee, service_charge)
--
--  The brief's requirement 4: a total from the treatment type and the
--  consultation fee. A function rather than a procedure because it
--  returns one value and has no side effect, so it can be used inside
--  a SELECT.
-- ---------------------------------------------------------------------
CREATE FUNCTION fn_calculate_bill(
    p_treatment_id     VARCHAR(64),
    p_consultation_fee DECIMAL(10,2),
    p_service_charge   DECIMAL(10,2)
) RETURNS DECIMAL(10,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_treatment_cost DECIMAL(10,2) DEFAULT 0;

    SELECT base_cost INTO v_treatment_cost
      FROM treatment
     WHERE id = p_treatment_id
       AND active = TRUE;

    RETURN IFNULL(v_treatment_cost, 0) + p_consultation_fee + p_service_charge;
END$$

-- ---------------------------------------------------------------------
--  fn_dentist_rating(dentist_id)
--
--  Mean rating, or NULL where fewer than five reviews exist. The floor
--  is FR-RVW-12, and putting it here means no caller can accidentally
--  publish a mean drawn from one visit.
-- ---------------------------------------------------------------------
CREATE FUNCTION fn_dentist_rating(p_dentist_id VARCHAR(64))
RETURNS DECIMAL(3,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_mean  DECIMAL(3,2);

    SELECT COUNT(*), AVG(rating) INTO v_count, v_mean
      FROM dentist_review
     WHERE dentist_id = p_dentist_id;

    IF v_count < 5 THEN
        RETURN NULL;
    END IF;
    RETURN v_mean;
END$$

-- ---------------------------------------------------------------------
--  sp_register_appointment(...)
--
--  The brief's requirement 2, as one transaction: lock the slot, check
--  it is open, take the day's next number, insert the appointment, mark
--  the slot booked.
--
--  The SELECT ... FOR UPDATE is what makes FR-APT-04 hold under
--  concurrency. Two callers racing for the same slot serialise here;
--  the second sees status = 'BOOKED' and is refused.
-- ---------------------------------------------------------------------
CREATE PROCEDURE sp_register_appointment(
    IN  p_patient_id      VARCHAR(64),
    IN  p_slot_id         VARCHAR(128),
    IN  p_treatment_id    VARCHAR(64),
    IN  p_created_by_uid  VARCHAR(64),
    IN  p_created_by_role VARCHAR(16),
    OUT p_appointment_no  VARCHAR(32)
)
MODIFIES SQL DATA
BEGIN
    DECLARE v_status     VARCHAR(16);
    DECLARE v_dentist_id VARCHAR(64);
    DECLARE v_date       DATE;
    DECLARE v_time       TIME;
    DECLARE v_day_key    CHAR(8);
    DECLARE v_counter    INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    --  The row lock. Everything below runs while this slot is held.
    SELECT status, dentist_id, slot_date, start_time
      INTO v_status, v_dentist_id, v_date, v_time
      FROM slot
     WHERE id = p_slot_id
       FOR UPDATE;

    IF v_status IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No such slot.';
    END IF;

    IF v_status <> 'OPEN' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'That time has just been taken. Please choose another.';
    END IF;

    --  Gapless per-day sequence: APT-yyyymmdd-####
    SET v_day_key = DATE_FORMAT(v_date, '%Y%m%d');

    INSERT INTO appointment_counter (day_key, counter_value)
         VALUES (v_day_key, 1)
    ON DUPLICATE KEY UPDATE counter_value = counter_value + 1;

    SELECT counter_value INTO v_counter
      FROM appointment_counter
     WHERE day_key = v_day_key;

    SET p_appointment_no = CONCAT('APT-', v_day_key, '-', LPAD(v_counter, 4, '0'));

    INSERT INTO appointment
        (appointment_no, patient_id, dentist_id, slot_id, treatment_id,
         appointment_date, appointment_time, status,
         created_by_uid, created_by_role, created_at)
    VALUES
        (p_appointment_no, p_patient_id, v_dentist_id, p_slot_id, p_treatment_id,
         v_date, v_time, 'CONFIRMED',
         p_created_by_uid, p_created_by_role, NOW());

    UPDATE slot
       SET status = 'BOOKED',
           appointment_no = p_appointment_no
     WHERE id = p_slot_id;

    COMMIT;
END$$

-- ---------------------------------------------------------------------
--  trg_prevent_double_booking
--
--  The backstop. sp_register_appointment holds a row lock, but an
--  appointment inserted by any other route must still not claim a slot
--  that is already taken.
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_prevent_double_booking
BEFORE INSERT ON appointment
FOR EACH ROW
BEGIN
    DECLARE v_status VARCHAR(16);

    SELECT status INTO v_status
      FROM slot
     WHERE id = NEW.slot_id;

    IF v_status = 'BOOKED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'That slot is already booked.';
    END IF;
END$$

-- ---------------------------------------------------------------------
--  trg_check_receptionist_role
--
--  FR-DAT-03. A foreign key can require that bill.receptionist_uid names
--  a real account; it cannot require that the account's role is
--  RECEPTIONIST. That is why this is a trigger and not a constraint, and
--  it is the clearest example in the schema of a rule needing one.
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_check_receptionist_role
BEFORE INSERT ON bill
FOR EACH ROW
BEGIN
    DECLARE v_role VARCHAR(16);

    IF NEW.receptionist_uid IS NOT NULL THEN
        SELECT role INTO v_role
          FROM user_account
         WHERE uid = NEW.receptionist_uid;

        IF v_role NOT IN ('RECEPTIONIST', 'ADMIN') THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'A bill may only be credited to a receptionist or an administrator.';
        END IF;
    END IF;
END$$

-- ---------------------------------------------------------------------
--  trg_bill_requires_completion
--
--  FR-BIL-05. A bill may only be issued for an appointment the dentist
--  has marked complete.
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_bill_requires_completion
BEFORE INSERT ON bill
FOR EACH ROW
BEGIN
    DECLARE v_status VARCHAR(16);

    SELECT status INTO v_status
      FROM appointment
     WHERE appointment_no = NEW.appointment_no;

    IF v_status NOT IN ('COMPLETED', 'BILLED') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT =
                'The dentist must mark this appointment complete before it can be billed.';
    END IF;
END$$

-- ---------------------------------------------------------------------
--  trg_review_requires_completion
--
--  FR-RVW-04. You cannot rate care you have not received.
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_review_requires_completion
BEFORE INSERT ON dentist_review
FOR EACH ROW
BEGIN
    DECLARE v_status VARCHAR(16);

    SELECT status INTO v_status
      FROM appointment
     WHERE appointment_no = NEW.appointment_no;

    IF v_status NOT IN ('COMPLETED', 'BILLED') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'You can rate a visit once it has happened.';
    END IF;
END$$

-- ---------------------------------------------------------------------
--  trg_audit_appointment_status
--
--  FR-AUD-01. Every status change is recorded, whatever changed it. An
--  audit trail written only by the application is an audit trail with a
--  hole in it.
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_audit_appointment_status
AFTER UPDATE ON appointment
FOR EACH ROW
BEGIN
    IF NEW.status <> OLD.status THEN
        INSERT INTO audit_event
            (id, actor_uid, actor_role, action, target_type, target_id, event_time)
        VALUES
            (UUID(), NULL, NULL,
             CONCAT('APPOINTMENT_', OLD.status, '_TO_', NEW.status),
             'appointment', NEW.appointment_no, NOW());
    END IF;
END$$

DELIMITER ;

-- =====================================================================
--  Note on actor_uid in trg_audit_appointment_status
--
--  A trigger cannot see the application's signed-in user, so actor_uid
--  is NULL here. The application writes its own audit row carrying the
--  actor; this trigger is the safety net that records a change made by
--  any other route. Two rows for one change is the intended outcome:
--  one says who, the other says it happened at all.
-- =====================================================================
