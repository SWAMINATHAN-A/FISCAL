-- ============================================================================
-- FISCAL — Custom Tax Management System
-- Seed Demonstration Data (Oracle Database DML)
-- File: 03_seed_data.sql
-- ============================================================================

-- Disable autocommit in SQL*Plus environment (managed by explicit COMMIT)
-- SET AUTOCOMMIT OFF;

-- ----------------------------------------------------------------------------
-- 1. APP_USER Seed Records (Demonstration Users)
-- Passwords:
--   admin    -> Admin@123   (Role: ADMIN)
--   officer  -> Officer@123 (Role: OFFICER)
--   auditor  -> Auditor@123 (Role: AUDITOR)
-- Uses PBKDF2WithHmacSHA256 (10,000 iterations, 32-byte hash)
-- ----------------------------------------------------------------------------
INSERT INTO APP_USER (USER_ID, USERNAME, PASSWORD_HASH, SALT, FULL_NAME, EMAIL, ROLE, STATUS, CREATED_AT)
VALUES (
    101,
    'admin',
    '567fe3eba4137898a0937d43028963e535e80ab460b12001bbf07f31587db107',
    'a1b2c3d4e5f67890',
    'Arpit Raj (Chief Administrator)',
    'admin@fiscal.internal',
    'ADMIN',
    'ACTIVE',
    TO_DATE('2026-01-10', 'YYYY-MM-DD')
);

INSERT INTO APP_USER (USER_ID, USERNAME, PASSWORD_HASH, SALT, FULL_NAME, EMAIL, ROLE, STATUS, CREATED_AT)
VALUES (
    102,
    'officer',
    '9f7d639f050edfb3a0bde235379f94fa97479a34073249a18ff9f279c80da28e',
    'b2c3d4e5f6a17890',
    'Siddharth Varma (Assessment Officer)',
    'officer@fiscal.internal',
    'OFFICER',
    'ACTIVE',
    TO_DATE('2026-01-15', 'YYYY-MM-DD')
);

INSERT INTO APP_USER (USER_ID, USERNAME, PASSWORD_HASH, SALT, FULL_NAME, EMAIL, ROLE, STATUS, CREATED_AT)
VALUES (
    103,
    'auditor',
    '9534df533ff1615ab57fdeac16f32f00e758f9ce063df9eebf9e98bbecb0046d',
    'c3d4e5f6a1b27890',
    'Nandini Sen (Compliance Auditor)',
    'auditor@fiscal.internal',
    'AUDITOR',
    'ACTIVE',
    TO_DATE('2026-02-01', 'YYYY-MM-DD')
);

-- ----------------------------------------------------------------------------
-- 2. TAX_TYPE Seed Records (Statutory Tax Types with Configured Percentage Rates)
-- ----------------------------------------------------------------------------
INSERT INTO TAX_TYPE (TAX_ID, TAX_NAME, TAX_RATE, DESCRIPTION, CREATED_AT)
VALUES (
    201,
    'Income Tax',
    15.00,
    'Statutory personal and salaried income taxation baseline assessment',
    TO_DATE('2026-01-01', 'YYYY-MM-DD')
);

INSERT INTO TAX_TYPE (TAX_ID, TAX_NAME, TAX_RATE, DESCRIPTION, CREATED_AT)
VALUES (
    202,
    'Goods and Services Tax (GST)',
    18.00,
    'Standard consolidated commercial turnover and services assessment rate',
    TO_DATE('2026-01-01', 'YYYY-MM-DD')
);

INSERT INTO TAX_TYPE (TAX_ID, TAX_NAME, TAX_RATE, DESCRIPTION, CREATED_AT)
VALUES (
    203,
    'Capital Gains Tax',
    12.50,
    'Taxation on long-term securities and real estate asset disposition gains',
    TO_DATE('2026-01-01', 'YYYY-MM-DD')
);

INSERT INTO TAX_TYPE (TAX_ID, TAX_NAME, TAX_RATE, DESCRIPTION, CREATED_AT)
VALUES (
    204,
    'Corporate Tax',
    22.00,
    'Operational net income assessment for registered corporate entities',
    TO_DATE('2026-01-01', 'YYYY-MM-DD')
);

INSERT INTO TAX_TYPE (TAX_ID, TAX_NAME, TAX_RATE, DESCRIPTION, CREATED_AT)
VALUES (
    205,
    'Professional Tax',
    5.00,
    'Municipal assessment on professional certification and trade practice',
    TO_DATE('2026-01-01', 'YYYY-MM-DD')
);

INSERT INTO TAX_TYPE (TAX_ID, TAX_NAME, TAX_RATE, DESCRIPTION, CREATED_AT)
VALUES (
    206,
    'Wealth & Luxury Surcharge',
    8.50,
    'High net-worth asset assessment and luxury consumption tax tier',
    TO_DATE('2026-01-01', 'YYYY-MM-DD')
);

-- ----------------------------------------------------------------------------
-- 3. TAXPAYER Seed Records (Diverse Demographics and Income Bands)
-- ----------------------------------------------------------------------------
INSERT INTO TAXPAYER (TAXPAYER_ID, NAME, PAN_NO, AGE, CITY, INCOME, CREATED_AT)
VALUES (1001, 'Aarav Sharma', 'ABCPS1234F', 34, 'Mumbai', 1850000.00, TO_DATE('2026-01-12', 'YYYY-MM-DD'));

INSERT INTO TAXPAYER (TAXPAYER_ID, NAME, PAN_NO, AGE, CITY, INCOME, CREATED_AT)
VALUES (1002, 'Priya Iyer', 'BKDPI5678K', 29, 'Bengaluru', 2400000.00, TO_DATE('2026-01-14', 'YYYY-MM-DD'));

INSERT INTO TAXPAYER (TAXPAYER_ID, NAME, PAN_NO, AGE, CITY, INCOME, CREATED_AT)
VALUES (1003, 'Rohan Mehta', 'CLPRM9012M', 45, 'New Delhi', 3200000.00, TO_DATE('2026-01-18', 'YYYY-MM-DD'));

INSERT INTO TAXPAYER (TAXPAYER_ID, NAME, PAN_NO, AGE, CITY, INCOME, CREATED_AT)
VALUES (1004, 'Ananya Deshmukh', 'DMKAD3456P', 38, 'Pune', 1450000.00, TO_DATE('2026-01-22', 'YYYY-MM-DD'));

INSERT INTO TAXPAYER (TAXPAYER_ID, NAME, PAN_NO, AGE, CITY, INCOME, CREATED_AT)
VALUES (1005, 'Vikramaditya Rao', 'EFGVR7890Q', 52, 'Hyderabad', 4800000.00, TO_DATE('2026-01-25', 'YYYY-MM-DD'));

INSERT INTO TAXPAYER (TAXPAYER_ID, NAME, PAN_NO, AGE, CITY, INCOME, CREATED_AT)
VALUES (1006, 'Sneha Kulkarni', 'GHISK2345T', 27, 'Chennai', 1200000.00, TO_DATE('2026-02-02', 'YYYY-MM-DD'));

INSERT INTO TAXPAYER (TAXPAYER_ID, NAME, PAN_NO, AGE, CITY, INCOME, CREATED_AT)
VALUES (1007, 'Kabir Singhania', 'JKLKS6789W', 41, 'Kolkata', 2950000.00, TO_DATE('2026-02-10', 'YYYY-MM-DD'));

INSERT INTO TAXPAYER (TAXPAYER_ID, NAME, PAN_NO, AGE, CITY, INCOME, CREATED_AT)
VALUES (1008, 'Meera Nambiar', 'MNPMN1122Z', 33, 'Kochi', 1600000.00, TO_DATE('2026-02-18', 'YYYY-MM-DD'));

INSERT INTO TAXPAYER (TAXPAYER_ID, NAME, PAN_NO, AGE, CITY, INCOME, CREATED_AT)
VALUES (1009, 'Devansh Verma', 'OPQDV3344B', 48, 'Ahmedabad', 3750000.00, TO_DATE('2026-03-01', 'YYYY-MM-DD'));

INSERT INTO TAXPAYER (TAXPAYER_ID, NAME, PAN_NO, AGE, CITY, INCOME, CREATED_AT)
VALUES (1010, 'Ishita Sen', 'RSTIS5566D', 31, 'Jaipur', 1380000.00, TO_DATE('2026-03-10', 'YYYY-MM-DD'));

-- ----------------------------------------------------------------------------
-- 4. TAX_PAYMENT Seed Records (Authoritative Ledger Entries)
-- Statuses: 'PAID', 'PENDING', 'REVERSED'
-- Formula Verification: Tax Amount = Applicable Amount * Rate / 100
-- ----------------------------------------------------------------------------
-- Aarav Sharma (1001): Income Tax 15% on 1,850,000 -> 277,500
INSERT INTO TAX_PAYMENT (PAYMENT_ID, TAXPAYER_ID, TAX_ID, AMOUNT, PAYMENT_DATE, STATUS, PAYMENT_REF, REMARKS)
VALUES (5001, 1001, 201, 277500.00, TO_DATE('2026-02-15', 'YYYY-MM-DD'), 'PAID', 'FSC-2026-5001', 'FY25-26 Annual Advance Income Tax Installment');

-- Priya Iyer (1002): GST 18% on 600,000 commercial turnover -> 108,000
INSERT INTO TAX_PAYMENT (PAYMENT_ID, TAXPAYER_ID, TAX_ID, AMOUNT, PAYMENT_DATE, STATUS, PAYMENT_REF, REMARKS)
VALUES (5002, 1002, 202, 108000.00, TO_DATE('2026-02-20', 'YYYY-MM-DD'), 'PAID', 'FSC-2026-5002', 'Q3 Goods & Services Tax Settlement');

-- Rohan Mehta (1003): Capital Gains 12.5% on 800,000 gain -> 100,000
INSERT INTO TAX_PAYMENT (PAYMENT_ID, TAXPAYER_ID, TAX_ID, AMOUNT, PAYMENT_DATE, STATUS, PAYMENT_REF, REMARKS)
VALUES (5003, 1003, 203, 100000.00, TO_DATE('2026-02-28', 'YYYY-MM-DD'), 'PAID', 'FSC-2026-5003', 'Long-term equity asset capital gains filing');

-- Vikramaditya Rao (1005): Corporate Tax 22% on 4,800,000 -> 1,056,000 (Installment 1 paid, installment 2 pending)
INSERT INTO TAX_PAYMENT (PAYMENT_ID, TAXPAYER_ID, TAX_ID, AMOUNT, PAYMENT_DATE, STATUS, PAYMENT_REF, REMARKS)
VALUES (5004, 1005, 204, 528000.00, TO_DATE('2026-03-05', 'YYYY-MM-DD'), 'PAID', 'FSC-2026-5004', 'Corporate Operational Tax Installment 1 of 2');

INSERT INTO TAX_PAYMENT (PAYMENT_ID, TAXPAYER_ID, TAX_ID, AMOUNT, PAYMENT_DATE, STATUS, PAYMENT_REF, REMARKS)
VALUES (5005, 1005, 204, 528000.00, TO_DATE('2026-03-25', 'YYYY-MM-DD'), 'PENDING', 'FSC-2026-5005', 'Corporate Operational Tax Installment 2 of 2 (Awaiting Clearance)');

-- Ananya Deshmukh (1004): Income Tax 15% on 1,450,000 -> 217,500
INSERT INTO TAX_PAYMENT (PAYMENT_ID, TAXPAYER_ID, TAX_ID, AMOUNT, PAYMENT_DATE, STATUS, PAYMENT_REF, REMARKS)
VALUES (5006, 1004, 201, 217500.00, TO_DATE('2026-03-12', 'YYYY-MM-DD'), 'PAID', 'FSC-2026-5006', 'Self-assessment annual income tax remittance');

-- Sneha Kulkarni (1006): Professional Tax 5% on 1,200,000 -> 60,000
INSERT INTO TAX_PAYMENT (PAYMENT_ID, TAXPAYER_ID, TAX_ID, AMOUNT, PAYMENT_DATE, STATUS, PAYMENT_REF, REMARKS)
VALUES (5007, 1006, 205, 60000.00, TO_DATE('2026-03-18', 'YYYY-MM-DD'), 'PAID', 'FSC-2026-5007', 'Municipal professional statutory tax deduction');

-- Kabir Singhania (1007): Luxury Surcharge 8.5% on 500,000 luxury asset assessment -> 42,500 (Pending bank processing)
INSERT INTO TAX_PAYMENT (PAYMENT_ID, TAXPAYER_ID, TAX_ID, AMOUNT, PAYMENT_DATE, STATUS, PAYMENT_REF, REMARKS)
VALUES (5008, 1007, 206, 42500.00, TO_DATE('2026-03-22', 'YYYY-MM-DD'), 'PENDING', 'FSC-2026-5008', 'Luxury asset acquisition surcharge - under verification');

-- Devansh Verma (1009): Income Tax 15% on 3,750,000 -> 562,500
INSERT INTO TAX_PAYMENT (PAYMENT_ID, TAXPAYER_ID, TAX_ID, AMOUNT, PAYMENT_DATE, STATUS, PAYMENT_REF, REMARKS)
VALUES (5009, 1009, 201, 562500.00, TO_DATE('2026-03-29', 'YYYY-MM-DD'), 'PAID', 'FSC-2026-5009', 'Advance tax fourth quarter remittance');

-- Meera Nambiar (1008): Reversal test case (Duplicate transmission reversed by audit)
INSERT INTO TAX_PAYMENT (PAYMENT_ID, TAXPAYER_ID, TAX_ID, AMOUNT, PAYMENT_DATE, STATUS, PAYMENT_REF, REMARKS)
VALUES (5010, 1008, 201, 240000.00, TO_DATE('2026-03-02', 'YYYY-MM-DD'), 'REVERSED', 'FSC-2026-5010', 'Reversed due to bank reconciliation duplicate batch transmission');

-- Commit Transaction
COMMIT;
