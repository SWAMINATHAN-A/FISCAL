-- ============================================================================
-- FISCAL — Custom Tax Management System
-- Database Schema Definition (Oracle Database DDL)
-- File: 01_schema.sql
-- ============================================================================

-- Clean up existing tables if necessary (executed in child-to-parent order)
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE TAX_PAYMENT CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE TAXPAYER CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE TAX_TYPE CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE APP_USER CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

-- ----------------------------------------------------------------------------
-- Table 1: APP_USER (Authentication & Authorization)
-- ----------------------------------------------------------------------------
CREATE TABLE APP_USER (
    USER_ID         NUMBER          NOT NULL,
    USERNAME        VARCHAR2(50)    NOT NULL,
    PASSWORD_HASH   VARCHAR2(255)   NOT NULL,
    SALT            VARCHAR2(64)    NOT NULL,
    FULL_NAME       VARCHAR2(100)   NOT NULL,
    EMAIL           VARCHAR2(100)   NOT NULL,
    ROLE            VARCHAR2(20)    DEFAULT 'OFFICER' NOT NULL,
    STATUS          VARCHAR2(20)    DEFAULT 'ACTIVE' NOT NULL,
    CREATED_AT      DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PK_APP_USER PRIMARY KEY (USER_ID),
    CONSTRAINT UQ_APP_USER_USERNAME UNIQUE (USERNAME),
    CONSTRAINT UQ_APP_USER_EMAIL UNIQUE (EMAIL),
    CONSTRAINT CHK_APP_USER_ROLE CHECK (ROLE IN ('ADMIN', 'OFFICER', 'AUDITOR')),
    CONSTRAINT CHK_APP_USER_STATUS CHECK (STATUS IN ('ACTIVE', 'INACTIVE'))
);

-- ----------------------------------------------------------------------------
-- Table 2: TAXPAYER (Registered Taxpayers)
-- ----------------------------------------------------------------------------
CREATE TABLE TAXPAYER (
    TAXPAYER_ID     NUMBER          NOT NULL,
    NAME            VARCHAR2(100)   NOT NULL,
    PAN_NO          VARCHAR2(10)    NOT NULL,
    AGE             NUMBER(3)       NOT NULL,
    CITY            VARCHAR2(100)   NOT NULL,
    INCOME          NUMBER(14, 2)   NOT NULL,
    CREATED_AT      DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PK_TAXPAYER PRIMARY KEY (TAXPAYER_ID),
    CONSTRAINT UQ_TAXPAYER_PAN UNIQUE (PAN_NO),
    CONSTRAINT CHK_TAXPAYER_AGE CHECK (AGE >= 18 AND AGE <= 120),
    CONSTRAINT CHK_TAXPAYER_INCOME CHECK (INCOME >= 0),
    CONSTRAINT CHK_TAXPAYER_PAN_LEN CHECK (LENGTH(PAN_NO) = 10)
);

-- ----------------------------------------------------------------------------
-- Table 3: TAX_TYPE (Tax Categories and Configured Rates)
-- ----------------------------------------------------------------------------
CREATE TABLE TAX_TYPE (
    TAX_ID          NUMBER          NOT NULL,
    TAX_NAME        VARCHAR2(100)   NOT NULL,
    TAX_RATE        NUMBER(5, 2)    NOT NULL,
    DESCRIPTION     VARCHAR2(255),
    CREATED_AT      DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PK_TAX_TYPE PRIMARY KEY (TAX_ID),
    CONSTRAINT UQ_TAX_TYPE_NAME UNIQUE (TAX_NAME),
    CONSTRAINT CHK_TAX_TYPE_RATE CHECK (TAX_RATE >= 0.00 AND TAX_RATE <= 100.00)
);

-- ----------------------------------------------------------------------------
-- Table 4: TAX_PAYMENT (Recorded Tax Payments & Ledger)
-- ----------------------------------------------------------------------------
CREATE TABLE TAX_PAYMENT (
    PAYMENT_ID      NUMBER          NOT NULL,
    TAXPAYER_ID     NUMBER          NOT NULL,
    TAX_ID          NUMBER          NOT NULL,
    AMOUNT          NUMBER(14, 2)   NOT NULL,
    PAYMENT_DATE    DATE            DEFAULT SYSDATE NOT NULL,
    STATUS          VARCHAR2(20)    DEFAULT 'PAID' NOT NULL,
    PAYMENT_REF     VARCHAR2(50)    NOT NULL,
    REMARKS         VARCHAR2(255),
    CONSTRAINT PK_TAX_PAYMENT PRIMARY KEY (PAYMENT_ID),
    CONSTRAINT UQ_TAX_PAYMENT_REF UNIQUE (PAYMENT_REF),
    CONSTRAINT FK_PAYMENT_TAXPAYER FOREIGN KEY (TAXPAYER_ID)
        REFERENCES TAXPAYER (TAXPAYER_ID) ON DELETE CASCADE,
    CONSTRAINT FK_PAYMENT_TAX_TYPE FOREIGN KEY (TAX_ID)
        REFERENCES TAX_TYPE (TAX_ID),
    CONSTRAINT CHK_PAYMENT_AMOUNT CHECK (AMOUNT >= 0),
    CONSTRAINT CHK_PAYMENT_STATUS CHECK (STATUS IN ('PAID', 'PENDING', 'REVERSED'))
);

-- Performance Indexes
CREATE INDEX IDX_TAXPAYER_CITY ON TAXPAYER(CITY);
CREATE INDEX IDX_TAXPAYER_NAME ON TAXPAYER(NAME);
CREATE INDEX IDX_PAYMENT_TAXPAYER ON TAX_PAYMENT(TAXPAYER_ID);
CREATE INDEX IDX_PAYMENT_TAX_TYPE ON TAX_PAYMENT(TAX_ID);
CREATE INDEX IDX_PAYMENT_DATE ON TAX_PAYMENT(PAYMENT_DATE);
CREATE INDEX IDX_PAYMENT_STATUS ON TAX_PAYMENT(STATUS);

COMMENT ON TABLE TAXPAYER IS 'Master taxpayer registry containing personal demographics, PAN, and declared gross annual income';
COMMENT ON TABLE TAX_TYPE IS 'Configured statutory tax categories with percentage rate specifications';
COMMENT ON TABLE TAX_PAYMENT IS 'Authoritative ledger of tax payments, assessments, and transactional statuses';
COMMENT ON TABLE APP_USER IS 'System users, credentials with cryptographic salts, and role authorizations';
