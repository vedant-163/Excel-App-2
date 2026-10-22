-- =============================================================
--  Stored Procedures & Triggers for excel_app
--  Run AFTER 03_views.sql
-- =============================================================
USE excel_app;
DELIMITER $$

-- -------------------------------------------------------------------
-- SP 1: Search students (replaces Q02 — used by Android GET /students)
-- Handles all slicers + free-text search + pagination
-- -------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_search_students$$
CREATE PROCEDURE sp_search_students(
    IN  p_q                 VARCHAR(255),   -- free-text: name / enrolment / roll
    IN  p_branch            VARCHAR(32),
    IN  p_admission_year    VARCHAR(16),
    IN  p_eligible_category VARCHAR(16),
    IN  p_alloted_category  VARCHAR(64),
    IN  p_gender            CHAR(1),
    IN  p_ews               CHAR(1),
    IN  p_domicile          CHAR(1),
    IN  p_religion          VARCHAR(32),
    IN  p_remarks           VARCHAR(64),
    IN  p_limit             INT,
    IN  p_offset            INT
)
BEGIN
    SET p_limit  = IFNULL(p_limit, 30);
    SET p_offset = IFNULL(p_offset, 0);

    SELECT
        student_id, sr_no, college_unique_id, class_roll_no,
        enrolment_no, name, father_name, mother_name,
        branch_code, eligible_category, alloted_category,
        domicile, gender, ews, religion,
        fee_structure, admission_year, admission_status, admission_round, remarks,
        student_phone,
        total_amount, student_submitted_amount AS paid_amount, pending_amount
    FROM v_student_search
    WHERE
        (p_branch            IS NULL OR branch_code        = p_branch)
        AND (p_admission_year    IS NULL OR admission_year     = p_admission_year)
        AND (p_eligible_category IS NULL OR eligible_category  = p_eligible_category)
        AND (p_alloted_category  IS NULL OR alloted_category   = p_alloted_category)
        AND (p_gender            IS NULL OR gender             = p_gender)
        AND (p_ews               IS NULL OR ews                = p_ews)
        AND (p_domicile          IS NULL OR domicile           = p_domicile)
        AND (p_religion          IS NULL OR religion           = p_religion)
        AND (p_remarks           IS NULL OR remarks            = p_remarks)
        AND (
            p_q IS NULL
            OR name             LIKE CONCAT('%', p_q, '%')
            OR enrolment_no     LIKE CONCAT('%', p_q, '%')
            OR class_roll_no    LIKE CONCAT('%', p_q, '%')
            OR college_unique_id LIKE CONCAT('%', p_q, '%')
            OR student_phone    LIKE CONCAT('%', p_q, '%')
        )
    ORDER BY sr_no
    LIMIT p_limit OFFSET p_offset;
END$$

-- -------------------------------------------------------------------
-- SP 2: Get single student full detail (GET /students/{id})
-- -------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_get_student$$
CREATE PROCEDURE sp_get_student(
    IN p_student_id   INT UNSIGNED,
    IN p_enrolment_no VARCHAR(32)
)
BEGIN
    -- Core + contact + admission + payment
    SELECT
        s.student_id, s.sr_no, s.college_unique_id, s.class_roll_no,
        s.enrolment_no, s.name, s.father_name, s.mother_name,
        s.rank_text, s.marks, s.exam_roll_no, s.date_of_birth,
        s.domicile, s.gender, s.ews,
        b.branch_code, b.branch_name,
        ec.category_code AS eligible_category,
        ac.category_code AS alloted_category,
        r.religion_code  AS religion,
        fs.fee_mast_code AS fee_structure,
        a.admission_year, a.admission_year_start, a.admission_no,
        ast.status_code  AS admission_status,
        rd.round_code    AS admission_round,
        rk.remark_code   AS remarks, a.remarks_raw,
        c.student_phone, c.student_email,
        c.parents_phone, c.parents_email,
        c.aadhaar_no,
        c.permanent_address, c.present_address,
        p.total_amount, p.total_amount_raw,
        p.student_submitted_amount, p.student_submitted_amount_raw,
        p.online_application_txn_id,
        pm.mode_code AS payment_mode, p.payment_mode_raw,
        p.payment_date, p.payment_date_raw, p.dte_fee,
        p.payment_status, p.payment_reference_no,
        GREATEST(COALESCE(p.total_amount,0) - COALESCE(p.student_submitted_amount,0), 0) AS pending_amount
    FROM student s
    JOIN branch b             ON b.branch_id = s.branch_id
    JOIN eligible_category ec ON ec.eligible_category_id = s.eligible_category_id
    JOIN alloted_category ac  ON ac.alloted_category_id  = s.alloted_category_id
    JOIN religion r           ON r.religion_id           = s.religion_id
    JOIN fee_structure fs     ON fs.fee_structure_id     = s.fee_structure_id
    JOIN admission a          ON a.student_id            = s.student_id
    JOIN admission_status ast ON ast.status_id           = a.status_id
    LEFT JOIN admission_round rd ON rd.round_id          = a.round_id
    LEFT JOIN remark_type rk     ON rk.remark_type_id   = a.remark_type_id
    LEFT JOIN contact c       ON c.student_id            = s.student_id
    LEFT JOIN payment p       ON p.student_id            = s.student_id
    LEFT JOIN payment_mode pm ON pm.payment_mode_id      = p.payment_mode_id
    WHERE s.student_id = COALESCE(p_student_id, s.student_id)
      AND (p_enrolment_no IS NULL OR s.enrolment_no = p_enrolment_no);
END$$

-- -------------------------------------------------------------------
-- SP 3: Get term-wise fees for one student (GET /students/{id}/fees)
-- -------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_get_student_fees$$
CREATE PROCEDURE sp_get_student_fees(IN p_student_id INT UNSIGNED)
BEGIN
    SELECT
        t.term_code, t.calendar_year, t.term_half, t.sort_order,
        f.amount AS scheduled_fee
    FROM student_term_fee f
    JOIN academic_term t ON t.term_id = f.term_id
    WHERE f.student_id = p_student_id
    ORDER BY t.sort_order;
END$$

-- -------------------------------------------------------------------
-- SP 4: Report — branch × category × gender pivot (GET /reports/branch-category)
-- -------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_report_branch_category$$
CREATE PROCEDURE sp_report_branch_category(IN p_year VARCHAR(16))
BEGIN
    SELECT
        branch_code,
        SUM(CASE WHEN eligible_category='OBC' AND gender='F' THEN student_count ELSE 0 END) AS obc_f,
        SUM(CASE WHEN eligible_category='OBC' AND gender='M' THEN student_count ELSE 0 END) AS obc_m,
        SUM(CASE WHEN eligible_category='OBC'               THEN student_count ELSE 0 END) AS obc_total,
        SUM(CASE WHEN eligible_category='SC'  AND gender='F' THEN student_count ELSE 0 END) AS sc_f,
        SUM(CASE WHEN eligible_category='SC'  AND gender='M' THEN student_count ELSE 0 END) AS sc_m,
        SUM(CASE WHEN eligible_category='SC'                THEN student_count ELSE 0 END) AS sc_total,
        SUM(CASE WHEN eligible_category='ST'  AND gender='F' THEN student_count ELSE 0 END) AS st_f,
        SUM(CASE WHEN eligible_category='ST'  AND gender='M' THEN student_count ELSE 0 END) AS st_m,
        SUM(CASE WHEN eligible_category='ST'                THEN student_count ELSE 0 END) AS st_total,
        SUM(CASE WHEN eligible_category='UR'  AND gender='F' THEN student_count ELSE 0 END) AS ur_f,
        SUM(CASE WHEN eligible_category='UR'  AND gender='M' THEN student_count ELSE 0 END) AS ur_m,
        SUM(CASE WHEN eligible_category='UR'                THEN student_count ELSE 0 END) AS ur_total,
        SUM(student_count) AS grand_total
    FROM v_report_branch_category
    WHERE admission_year = p_year
    GROUP BY branch_code
    ORDER BY branch_code;
END$$

-- -------------------------------------------------------------------
-- SP 5: Dashboard totals (GET /dashboard)
-- -------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_dashboard$$
CREATE PROCEDURE sp_dashboard()
BEGIN
    SELECT * FROM v_dashboard_totals;
    SELECT * FROM v_admission_year_summary;
END$$

-- -------------------------------------------------------------------
-- SP 6: Add / update payment (POST /payments or PUT /payments/{id})
-- -------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_upsert_payment$$
CREATE PROCEDURE sp_upsert_payment(
    IN p_student_id                  INT UNSIGNED,
    IN p_total_amount                DECIMAL(12,2),
    IN p_total_amount_raw            VARCHAR(128),
    IN p_student_submitted_amount    DECIMAL(12,2),
    IN p_student_submitted_amount_raw VARCHAR(128),
    IN p_online_txn_id               VARCHAR(255),
    IN p_payment_mode_code           VARCHAR(64),
    IN p_payment_date                DATE,
    IN p_dte_fee                     DECIMAL(12,2)
)
BEGIN
    DECLARE v_mode_id INT UNSIGNED DEFAULT NULL;

    -- Resolve payment mode
    SELECT payment_mode_id INTO v_mode_id
    FROM payment_mode WHERE mode_code = p_payment_mode_code LIMIT 1;

    -- Insert or update (one payment per student in current Excel model)
    INSERT INTO payment (
        student_id, total_amount, total_amount_raw,
        student_submitted_amount, student_submitted_amount_raw,
        online_application_txn_id, payment_mode_id, payment_mode_raw,
        payment_date, dte_fee
    ) VALUES (
        p_student_id, p_total_amount, p_total_amount_raw,
        p_student_submitted_amount, p_student_submitted_amount_raw,
        p_online_txn_id, v_mode_id, p_payment_mode_code,
        p_payment_date, p_dte_fee
    )
    ON DUPLICATE KEY UPDATE
        total_amount                 = p_total_amount,
        total_amount_raw             = p_total_amount_raw,
        student_submitted_amount     = p_student_submitted_amount,
        student_submitted_amount_raw = p_student_submitted_amount_raw,
        online_application_txn_id   = p_online_txn_id,
        payment_mode_id              = v_mode_id,
        payment_mode_raw             = p_payment_mode_code,
        payment_date                 = p_payment_date,
        dte_fee                      = p_dte_fee;

    SELECT ROW_COUNT() AS affected_rows;
END$$

-- -------------------------------------------------------------------
-- Trigger: Prevent deleting the last admission_status row
-- -------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_prevent_last_status_delete$$
CREATE TRIGGER trg_prevent_last_status_delete
BEFORE DELETE ON admission_status FOR EACH ROW
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt FROM admission_status;
    IF cnt <= 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot delete the only admission status row.';
    END IF;
END$$

-- -------------------------------------------------------------------
-- Trigger: Log updated_at timestamp on student change
-- (Add updated_at column if you want this; documented here for app devs)
-- -------------------------------------------------------------------

DELIMITER ;

-- Quick smoke test: call sp_dashboard
CALL sp_dashboard();
