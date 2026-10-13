-- ============================================================================
-- FISCAL — Custom Tax Management System
-- Operational Queries, Joins, Aggregations & Transactions (Oracle DML)
-- File: 04_sample_dml.sql
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. DASHBOARD AGGREGATE QUERIES (Used by DashboardServlet)
-- ----------------------------------------------------------------------------

-- Total Taxpayers
SELECT COUNT(*) AS TOTAL_TAXPAYERS FROM TAXPAYER;

-- Total Payments Count
SELECT COUNT(*) AS TOTAL_PAYMENTS FROM TAX_PAYMENT;

-- Total Tax Revenue Collected (Status = 'PAID')
SELECT NVL(SUM(AMOUNT), 0) AS TOTAL_PAID_REVENUE 
FROM TAX_PAYMENT 
WHERE STATUS = 'PAID';

-- Total Pending Tax Amount
SELECT NVL(SUM(AMOUNT), 0) AS TOTAL_PENDING_AMOUNT 
FROM TAX_PAYMENT 
WHERE STATUS = 'PENDING';

-- Total Reversed Tax Amount
SELECT NVL(SUM(AMOUNT), 0) AS TOTAL_REVERSED_AMOUNT 
FROM TAX_PAYMENT 
WHERE STATUS = 'REVERSED';

-- Revenue Breakdown by Tax Category
SELECT 
    t.TAX_NAME,
    t.TAX_RATE,
    COUNT(p.PAYMENT_ID) AS TRANSACTION_COUNT,
    NVL(SUM(CASE WHEN p.STATUS = 'PAID' THEN p.AMOUNT ELSE 0 END), 0) AS TOTAL_COLLECTED,
    NVL(SUM(CASE WHEN p.STATUS = 'PENDING' THEN p.AMOUNT ELSE 0 END), 0) AS TOTAL_PENDING
FROM TAX_TYPE t
LEFT JOIN TAX_PAYMENT p ON t.TAX_ID = p.TAX_ID
GROUP BY t.TAX_NAME, t.TAX_RATE
ORDER BY TOTAL_COLLECTED DESC;

-- Payment Distribution by Status
SELECT 
    STATUS, 
    COUNT(*) AS COUNT, 
    NVL(SUM(AMOUNT), 0) AS SUM_AMOUNT 
FROM TAX_PAYMENT 
GROUP BY STATUS;

-- ----------------------------------------------------------------------------
-- 2. RECENT PAYMENTS JOIN QUERY (Used on Dashboard & Payments Table)
-- ----------------------------------------------------------------------------
SELECT 
    p.PAYMENT_ID,
    p.PAYMENT_REF,
    p.TAXPAYER_ID,
    tp.NAME AS TAXPAYER_NAME,
    tp.PAN_NO,
    p.TAX_ID,
    tt.TAX_NAME,
    tt.TAX_RATE,
    p.AMOUNT,
    TO_CHAR(p.PAYMENT_DATE, 'YYYY-MM-DD') AS PAYMENT_DATE,
    p.STATUS,
    p.REMARKS
FROM TAX_PAYMENT p
JOIN TAXPAYER tp ON p.TAXPAYER_ID = tp.TAXPAYER_ID
JOIN TAX_TYPE tt ON p.TAX_ID = tt.TAX_ID
ORDER BY p.PAYMENT_DATE DESC, p.PAYMENT_ID DESC;

-- ----------------------------------------------------------------------------
-- 3. TAXPAYER SPECIFIC QUERIES
-- ----------------------------------------------------------------------------

-- Search taxpayers by Name or PAN with case-insensitivity
SELECT * FROM TAXPAYER 
WHERE UPPER(NAME) LIKE UPPER('%Sharma%') 
   OR UPPER(PAN_NO) LIKE UPPER('%Sharma%')
ORDER BY NAME ASC;

-- Filter taxpayers by City and minimum income
SELECT * FROM TAXPAYER
WHERE UPPER(CITY) = UPPER('Mumbai') 
  AND INCOME >= 1500000
ORDER BY INCOME DESC;

-- Taxpayer Detail with Cumulative Tax Paid
SELECT 
    tp.TAXPAYER_ID,
    tp.NAME,
    tp.PAN_NO,
    tp.AGE,
    tp.CITY,
    tp.INCOME,
    NVL(SUM(CASE WHEN p.STATUS = 'PAID' THEN p.AMOUNT ELSE 0 END), 0) AS TOTAL_TAX_PAID,
    COUNT(p.PAYMENT_ID) AS PAYMENT_COUNT
FROM TAXPAYER tp
LEFT JOIN TAX_PAYMENT p ON tp.TAXPAYER_ID = p.TAXPAYER_ID
WHERE tp.TAXPAYER_ID = 1001
GROUP BY tp.TAXPAYER_ID, tp.NAME, tp.PAN_NO, tp.AGE, tp.CITY, tp.INCOME;

-- Ledger of payments for a single taxpayer
SELECT 
    p.PAYMENT_ID,
    p.PAYMENT_REF,
    tt.TAX_NAME,
    tt.TAX_RATE,
    p.AMOUNT,
    p.PAYMENT_DATE,
    p.STATUS,
    p.REMARKS
FROM TAX_PAYMENT p
JOIN TAX_TYPE tt ON p.TAX_ID = tt.TAX_ID
WHERE p.TAXPAYER_ID = 1001
ORDER BY p.PAYMENT_DATE DESC;

-- ----------------------------------------------------------------------------
-- 4. TAX CALCULATION & VERIFICATION QUERIES
-- ----------------------------------------------------------------------------
-- Calculate statutory tax using configured rate: Amount = Applicable Income * Rate / 100
SELECT 
    tp.TAXPAYER_ID,
    tp.NAME,
    tp.INCOME,
    tt.TAX_NAME,
    tt.TAX_RATE,
    ROUND((tp.INCOME * tt.TAX_RATE / 100), 2) AS CALCULATED_TAX_LIABILITY
FROM TAXPAYER tp
CROSS JOIN TAX_TYPE tt
WHERE tp.TAXPAYER_ID = 1002 AND tt.TAX_ID = 201;

-- ----------------------------------------------------------------------------
-- 5. TRANSACTION INTEGRITY DEMONSTRATIONS (Commit & Rollback)
-- ----------------------------------------------------------------------------

-- Successful Transaction Example: Recording new payment and committing
SAVEPOINT sp_before_payment;

INSERT INTO TAX_PAYMENT (
    PAYMENT_ID, TAXPAYER_ID, TAX_ID, AMOUNT, PAYMENT_DATE, STATUS, PAYMENT_REF, REMARKS
) VALUES (
    TAX_PAYMENT_SEQ.NEXTVAL, 1003, 201, 150000.00, SYSDATE, 'PAID', 'FSC-TX-DEMO-01', 'Verified via net-banking batch'
);

-- Commit permanent persistence
COMMIT;

-- Rollback Transaction Demonstration: Attempting invalid or aborted insertion
SAVEPOINT sp_aborted_operation;

INSERT INTO TAX_PAYMENT (
    PAYMENT_ID, TAXPAYER_ID, TAX_ID, AMOUNT, PAYMENT_DATE, STATUS, PAYMENT_REF, REMARKS
) VALUES (
    TAX_PAYMENT_SEQ.NEXTVAL, 1004, 202, 999999.00, SYSDATE, 'PENDING', 'FSC-TX-ABORT-01', 'Test anomaly rollback'
);

-- Abort and restore state
ROLLBACK TO sp_aborted_operation;
