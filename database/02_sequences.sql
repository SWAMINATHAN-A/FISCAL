-- ============================================================================
-- FISCAL — Custom Tax Management System
-- Sequences and Auto-Increment Triggers (Oracle Database DDL)
-- File: 02_sequences.sql
-- ============================================================================

-- Drop sequences if they exist
BEGIN
    EXECUTE IMMEDIATE 'DROP SEQUENCE APP_USER_SEQ';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP SEQUENCE TAXPAYER_SEQ';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP SEQUENCE TAX_TYPE_SEQ';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP SEQUENCE TAX_PAYMENT_SEQ';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

-- ----------------------------------------------------------------------------
-- Create Sequences (Start with appropriate base numbers)
-- ----------------------------------------------------------------------------
CREATE SEQUENCE APP_USER_SEQ
    START WITH 101
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE TAXPAYER_SEQ
    START WITH 1001
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE TAX_TYPE_SEQ
    START WITH 201
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE TAX_PAYMENT_SEQ
    START WITH 5001
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- ----------------------------------------------------------------------------
-- Triggers for transparent auto-generation when PK is omitted
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_APP_USER_BI
BEFORE INSERT ON APP_USER
FOR EACH ROW
BEGIN
    IF :NEW.USER_ID IS NULL THEN
        SELECT APP_USER_SEQ.NEXTVAL INTO :NEW.USER_ID FROM DUAL;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_TAXPAYER_BI
BEFORE INSERT ON TAXPAYER
FOR EACH ROW
BEGIN
    IF :NEW.TAXPAYER_ID IS NULL THEN
        SELECT TAXPAYER_SEQ.NEXTVAL INTO :NEW.TAXPAYER_ID FROM DUAL;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_TAX_TYPE_BI
BEFORE INSERT ON TAX_TYPE
FOR EACH ROW
BEGIN
    IF :NEW.TAX_ID IS NULL THEN
        SELECT TAX_TYPE_SEQ.NEXTVAL INTO :NEW.TAX_ID FROM DUAL;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_TAX_PAYMENT_BI
BEFORE INSERT ON TAX_PAYMENT
FOR EACH ROW
BEGIN
    IF :NEW.PAYMENT_ID IS NULL THEN
        SELECT TAX_PAYMENT_SEQ.NEXTVAL INTO :NEW.PAYMENT_ID FROM DUAL;
    END IF;
END;
/
