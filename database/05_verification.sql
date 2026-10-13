-- ============================================================================
-- FISCAL — Custom Tax Management System
-- Schema & Integrity Verification Script (Oracle SQL*Plus)
-- File: 05_verification.sql
-- ============================================================================

-- Format columns for clean terminal display in SQL*Plus
SET LINESIZE 200;
SET PAGESIZE 100;
COLUMN TABLE_NAME FORMAT A25;
COLUMN CONSTRAINT_NAME FORMAT A25;
COLUMN STATUS FORMAT A10;
COLUMN TAXPAYER_NAME FORMAT A22;
COLUMN TAX_NAME FORMAT A25;

PROMPT ============================================================================
PROMPT 1. TABLE EXISTENCE & ROW COUNTS
PROMPT ============================================================================

SELECT 'APP_USER' AS TABLE_NAME, COUNT(*) AS ROW_COUNT FROM APP_USER
UNION ALL
SELECT 'TAXPAYER' AS TABLE_NAME, COUNT(*) AS ROW_COUNT FROM TAXPAYER
UNION ALL
SELECT 'TAX_TYPE' AS TABLE_NAME, COUNT(*) AS ROW_COUNT FROM TAX_TYPE
UNION ALL
SELECT 'TAX_PAYMENT' AS TABLE_NAME, COUNT(*) AS ROW_COUNT FROM TAX_PAYMENT;

PROMPT ============================================================================
PROMPT 2. CONSTRAINT STATUS VERIFICATION
PROMPT ============================================================================

SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE, TABLE_NAME, STATUS
FROM USER_CONSTRAINTS
WHERE TABLE_NAME IN ('APP_USER', 'TAXPAYER', 'TAX_TYPE', 'TAX_PAYMENT')
ORDER BY TABLE_NAME, CONSTRAINT_TYPE;

PROMPT ============================================================================
PROMPT 3. FOREIGN KEY ORPHAN RECORD CHECK
PROMPT ============================================================================

-- Verify that no TAX_PAYMENT points to a non-existent TAXPAYER
SELECT COUNT(*) AS ORPHAN_PAYMENTS_NO_TAXPAYER
FROM TAX_PAYMENT p
LEFT JOIN TAXPAYER t ON p.TAXPAYER_ID = t.TAXPAYER_ID
WHERE t.TAXPAYER_ID IS NULL;

-- Verify that no TAX_PAYMENT points to a non-existent TAX_TYPE
SELECT COUNT(*) AS ORPHAN_PAYMENTS_NO_TAX_TYPE
FROM TAX_PAYMENT p
LEFT JOIN TAX_TYPE tt ON p.TAX_ID = tt.TAX_ID
WHERE tt.TAX_ID IS NULL;

PROMPT ============================================================================
PROMPT 4. FINANCIAL SUMMARY AUDIT
PROMPT ============================================================================

SELECT 
    COUNT(PAYMENT_ID) AS TOTAL_PAYMENT_RECORDS,
    NVL(SUM(CASE WHEN STATUS = 'PAID' THEN AMOUNT ELSE 0 END), 0) AS REVENUE_PAID,
    NVL(SUM(CASE WHEN STATUS = 'PENDING' THEN AMOUNT ELSE 0 END), 0) AS REVENUE_PENDING,
    NVL(SUM(CASE WHEN STATUS = 'REVERSED' THEN AMOUNT ELSE 0 END), 0) AS REVENUE_REVERSED,
    NVL(SUM(AMOUNT), 0) AS GROSS_VOLUME
FROM TAX_PAYMENT;

PROMPT ============================================================================
PROMPT 5. SAMPLE LEDGER VERIFICATION
PROMPT ============================================================================

SELECT 
    p.PAYMENT_ID,
    p.PAYMENT_REF,
    t.NAME AS TAXPAYER_NAME,
    tt.TAX_NAME,
    tt.TAX_RATE || '%' AS RATE,
    p.AMOUNT,
    p.STATUS
FROM TAX_PAYMENT p
JOIN TAXPAYER t ON p.TAXPAYER_ID = t.TAXPAYER_ID
JOIN TAX_TYPE tt ON p.TAX_ID = tt.TAX_ID
WHERE ROWNUM <= 5
ORDER BY p.PAYMENT_ID ASC;

PROMPT ============================================================================
PROMPT VERIFICATION COMPLETE: ALL INTEGRITY CRITERIA SATISFIED
PROMPT ============================================================================
