# FISCAL — Comprehensive System Test Cases

This document defines formal test suites covering authentication, authorization, taxpayer validation, tax rules, calculation accuracy, payment ledger operations, and transaction integrity.

---

## Suite 1: Authentication & Session Security

| ID | Test Case | Inputs | Expected Output | Status |
|---|---|---|---|---|
| **AUTH-01** | Valid Administrator Login | `admin` / `Admin@123` | HTTP 200, Session established, Role = ADMIN | **PASS** |
| **AUTH-02** | Valid Assessment Officer Login | `officer` / `Officer@123` | HTTP 200, Session established, Role = OFFICER | **PASS** |
| **AUTH-03** | Invalid Password Attempt | `admin` / `WrongPass!` | HTTP 401 Unauthorized, generic error message | **PASS** |
| **AUTH-04** | Non-existent User | `ghost_user` / `AnyPass` | HTTP 401 Unauthorized | **PASS** |
| **AUTH-05** | Session Invalidation on Logout | Click Sign Out | HTTP 200, `FISCALSESSIONID` cleared, redirect | **PASS** |
| **AUTH-06** | Protected Endpoint Access Without Session | `GET /api/taxpayers` without cookie | HTTP 401 Unauthorized, JSON error | **PASS** |

---

## Suite 2: Taxpayer Management & Statutory Validations

| ID | Test Case | Inputs | Expected Output | Status |
|---|---|---|---|---|
| **TP-01** | Register Taxpayer with Valid Data | Name: `Harish Patel`, PAN: `HPXYZ9988K`, Age: 39, City: `Surat`, Income: `2100000.00` | HTTP 201 Created, PK assigned by `TAXPAYER_SEQ` | **PASS** |
| **TP-02** | Duplicate PAN Rejection | Name: `Another Person`, PAN: `ABCPS1234F` (already exists) | HTTP 400 Bad Request, Unique constraint check | **PASS** |
| **TP-03** | Invalid PAN Length / Format | PAN: `INVALID12` (9 chars) | HTTP 400 Bad Request, regex rejected | **PASS** |
| **TP-04** | Age Boundary Enforcement | Age: 17 (< 18) | HTTP 400 Bad Request, check constraint enforced | **PASS** |
| **TP-05** | Negative Income Rejection | Income: `-50000.00` | HTTP 400 Bad Request, rejected | **PASS** |
| **TP-06** | Search Taxpayer by Name/PAN | Query: `sharma` | Returns `Aarav Sharma` | **PASS** |
| **TP-07** | Filter Taxpayers by City | City: `Mumbai` | Returns Mumbai taxpayers only | **PASS** |
| **TP-08** | Sort Taxpayers by Income Descending | Sort: `income_desc` | Returns records ordered by highest declared income | **PASS** |

---

## Suite 3: Tax Type & Configured Rate Rules

| ID | Test Case | Inputs | Expected Output | Status |
|---|---|---|---|---|
| **TT-01** | Create Tax Category | Name: `Digital Services Tax`, Rate: `6.00%` | HTTP 201 Created, PK assigned | **PASS** |
| **TT-02** | Tax Rate > 100% Rejection | Rate: `105.00%` | HTTP 400 Bad Request | **PASS** |
| **TT-03** | Negative Tax Rate Rejection | Rate: `-2.00%` | HTTP 400 Bad Request | **PASS** |
| **TT-04** | Referential Integrity on Delete | Delete Tax Type 201 (referenced by payments) | HTTP 409 Conflict, deletion blocked to preserve ledger | **PASS** |

---

## Suite 4: Tax Calculation Precision & Assessment Engine

| ID | Test Case | Inputs | Expected Output | Status |
|---|---|---|---|---|
| **CALC-01** | Standard 15% Income Tax Calculation | Taxpayer 1001 (Income ₹1,850,000), Tax Type 201 (15%) | Calculated: `₹277,500.00` (`1850000 * 15 / 100`) | **PASS** |
| **CALC-02** | 18% GST Turnover Assessment | Amount: ₹600,000, Tax Type 202 (18%) | Calculated: `₹108,000.00` | **PASS** |
| **CALC-03** | Fractional Rate Rounding (HALF_UP) | Amount: ₹125,555.50, Rate: 12.50% | Calculated: `₹15,694.44` | **PASS** |
| **CALC-04** | Negative Taxable Base Rejection | Amount: `-1000.00` | HTTP 400 Bad Request | **PASS** |
| **CALC-05** | Conversion to Formal Ledger Entry | Click "Commit Assessment to Payment Ledger" | Pre-fills Payment Modal with exact calculated figure | **PASS** |

---

## Suite 5: Payment Ledger & Transaction Integrity

| ID | Test Case | Inputs | Expected Output | Status |
|---|---|---|---|---|
| **PAY-01** | Record New Payment Entry | Taxpayer: 1001, Tax Type: 201, Amount: ₹277,500.00, Status: `PAID` | HTTP 201 Created, Unique `FSC-2026-XXXX` reference assigned | **PASS** |
| **PAY-02** | Non-existent Taxpayer Foreign Key | Taxpayer: 999999 | HTTP 400 Bad Request, foreign key failure | **PASS** |
| **PAY-03** | Zero or Negative Amount Rejection | Amount: `0.00` | HTTP 400 Bad Request | **PASS** |
| **PAY-04** | Status Transition from PENDING to PAID | Payment #5005, Status -> `PAID` | HTTP 200, Dashboard revenue updated immediately | **PASS** |
| **PAY-05** | Audit Reversal Transition | Payment #5010, Status -> `REVERSED` | HTTP 200, Marked as reversed in ledger | **PASS** |
| **PAY-06** | Filter Ledger by Status | Status: `PENDING` | Returns pending installment records only | **PASS** |

---

## Suite 6: Administrative Role-Based Access Control

| ID | Test Case | Inputs | Expected Output | Status |
|---|---|---|---|---|
| **RBAC-01** | Admin Access to Operator Management | Logged in as `admin` | Allowed, HTTP 200, operator roster visible | **PASS** |
| **RBAC-02** | Non-Admin Access Rejection | Logged in as `officer`, request `/api/users` | HTTP 403 Forbidden | **PASS** |
| **RBAC-03** | Secure Password Hash Storage | Query `APP_USER` | Plaintext passwords never stored; PBKDF2 hash & salt verified | **PASS** |
