# FISCAL — Custom Tax Management System
> **Brand Tagline:** Clarity in every calculation.  
> **Academic Purpose:** Custom Tax Management / Web Application Development (PAT Project)  
> **Backend:** Java Servlets (`jakarta.servlet.*`) + JDBC  
> **Database:** Oracle Database (SQL*Plus DDL/DML, Sequences, Constraints, Triggers)  
> **Frontend:** HTML5 + Vanilla CSS3 + Vanilla JavaScript (Fetch API)  
> **Build System:** Maven 3.10+  
> **Application Server:** Apache Tomcat 10+ / Embedded `FiscalDevServer`

---

## 1. Project Title
**FISCAL — Custom Tax Management System**

---

## 2. Brand Description
FISCAL is a custom tax management workspace that brings taxpayer demographics, statutory tax categories, authoritative tax calculation rules, and payment ledger records into one unified, art-directed interface. It enables authorized tax operators and administrators to register taxpayers, maintain tax rules, calculate assessments with mathematical precision, record payments, and audit compliance status without feeling like conventional, sterile accounting software.

---

## 3. Problem Statement
Tax compliance in institutional and enterprise settings often suffers from fragmented data silos, opaque calculation rules, unverified manual inputs, and clunky user interfaces. Organizations struggle with tracking taxpayer liabilities against statutory rates, reconciling pending bank clearances, and maintaining transparent audit trails. FISCAL addresses this by establishing an authoritative Oracle-backed ledger, mathematical server-side calculation rules, and an editorial, tactile interface that simplifies financial oversight.

---

## 4. Project Objectives
1. **Authoritative Persistence:** Implement real Oracle Database tables (`TAXPAYER`, `TAX_TYPE`, `TAX_PAYMENT`, `APP_USER`) with primary keys, foreign keys, and check constraints.
2. **Statutory Tax Engine:** Implement authoritative server-side calculation logic (`Tax Amount = Applicable Amount × Tax Rate / 100`) with consistent rounding (`HALF_UP`).
3. **Ledger Integrity:** Provide multi-state payment tracking (`PAID`, `PENDING`, `REVERSED`) with atomic database transactions, commit, and rollback capabilities.
4. **Distinctive Visual Identity:** Deliver a bespoke, warm, tactile light-mode interface using the immutable Matcha (`#9CA763`) and Milk Honey (`#F1E8C7`) palette.
5. **Session Security:** Implement PBKDF2 password hashing with cryptographically secure salts, session fixation protection, and role-based authorization.

---

## 5. Key Features
- **Editorial Split-Screen Login:** Distinctive typography, geometric ledger artwork, and quick demonstration accounts.
- **Database-Driven Dashboard:** Real-time statistics including total registered taxpayers, total payments, verified revenue, pending collections, and category breakdowns.
- **Taxpayer Management:** Search by name or PAN, filter by city, sort by income, register new taxpayers, edit details, and view payment histories with masked PAN data.
- **Tax Category & Rate Governance:** Configurable statutory tax categories (Income Tax, GST, Capital Gains, Corporate Tax) with rate validation and deletion protection.
- **Interactive Calculation Workbench:** Server-side tax liability assessment with one-click conversion into a formal payment ledger record.
- **Payment Ledger & Status Transitions:** Filter by status, search by reference, record new payments, update status, and reverse transactions.
- **Administrator Governance:** Role-based access control (Admin, Officer, Auditor) and operator account provisioning.
- **Zero-Setup Offline Resilience:** Built-in embedded server and resilient repository engine ensuring full demonstration capability even when local Oracle instances are offline.

---

## 6. Technology Stack
- **Frontend:** HTML5, Vanilla CSS3 (Custom Properties / Design Tokens), Vanilla JavaScript (ES6+, Fetch API, custom events).
- **Backend:** Java 17+, Jakarta Servlet API 5.0, Java Database Connectivity (JDBC).
- **Database:** Oracle Database 19c / 21c / 23c Free / Express Edition (XE).
- **Libraries:** Oracle JDBC Driver (`ojdbc11`), Google Gson (`gson 2.10.1`), Java Cryptography Extension (`PBKDF2WithHmacSHA256`).
- **Server:** Apache Tomcat 10+ (Standard WAR) and Built-in `FiscalDevServer`.
- **Build System:** Apache Maven 3.10.

---

## 7. Architecture Diagram

```text
+-------------------------------------------------------------+
|                  CLIENT BROWSER (LIGHT MODE)                 |
| HTML5 + Vanilla CSS3 Tokens + Fetch API + Dynamic Micro-UI  |
+-------------------------------------------------------------+
                              |
                     JSON / HTTP Requests
                              v
+-------------------------------------------------------------+
|                     APPLICATION SERVER                      |
|                (Apache Tomcat / FiscalDevServer)            |
|                                                             |
|  [Filters]                                                  |
|   - CharacterEncodingFilter (UTF-8, Security Headers)       |
|   - AuthenticationFilter (Session Verification, RBAC)       |
|                                                             |
|  [Servlets]                                                 |
|   - AuthServlet (/api/auth/*)                               |
|   - DashboardServlet (/api/dashboard/summary)               |
|   - TaxpayerServlet (/api/taxpayers/*)                      |
|   - TaxTypeServlet (/api/tax-types/*)                       |
|   - TaxCalculationServlet (/api/tax/calculate)              |
|   - TaxPaymentServlet (/api/payments/*)                     |
|   - UserServlet (/api/users/*)                              |
|                                                             |
|  [Business Services Layer]                                  |
|   - AuthService, TaxpayerService, TaxTypeService            |
|   - TaxCalculationService (Authoritative Formula & Rounding)|
|   - TaxPaymentService, DashboardService                     |
|                                                             |
|  [Data Access Object (DAO) Layer]                           |
|   - PreparedStatement Queries & Transaction Commit/Rollback |
|   - DatabaseConfig Connection Factory                       |
+-------------------------------------------------------------+
                              |
                       Oracle JDBC Driver
                              v
+-------------------------------------------------------------+
|                       ORACLE DATABASE                       |
|  Tables: TAXPAYER, TAX_TYPE, TAX_PAYMENT, APP_USER          |
|  Sequences: TAXPAYER_SEQ, TAX_TYPE_SEQ, TAX_PAYMENT_SEQ     |
|  Triggers: Before-Insert Auto-Increment Handlers            |
|  Constraints: PK, FK (Cascade), UNIQUE (PAN), CHECK (Age)   |
+-------------------------------------------------------------+
```

---

## 8. Project Structure
```text
FISCAL/
├── README.md                      # Master build and technical documentation
├── pom.xml                        # Maven dependencies (Servlet 5.0, ojdbc11, Gson)
├── .gitignore                     # Git ignore rules for build artifacts and IDEs
├── docs/
│   ├── DATABASE_SCHEMA.md         # Full schema specification and ERD
│   ├── API_DOCUMENTATION.md       # Servlet RESTful endpoints documentation
│   ├── TEST_CASES.md              # Systematic test suites and status results
│   └── INSTALLATION.md            # Detailed installation and deployment guide
├── database/
│   ├── 01_schema.sql              # Table definitions, PK/FK, checks, and indexes
│   ├── 02_sequences.sql           # Oracle sequences and auto-increment triggers
│   ├── 03_seed_data.sql           # Verified demonstration records and users
│   ├── 04_sample_dml.sql          # Joins, aggregations, and transaction examples
│   └── 05_verification.sql        # Database integrity and row count audits
└── src/
    └── main/
        ├── resources/
        │   └── db.properties      # JDBC connection properties & fallback config
        ├── java/
        │   └── com/fiscal/
        │       ├── config/        # DatabaseConfig connection factory
        │       ├── model/         # AppUser, Taxpayer, TaxType, TaxPayment, etc.
        │       ├── dao/           # AppUserDAO, TaxpayerDAO, TaxTypeDAO, etc.
        │       ├── service/       # Business logic and calculation service
        │       ├── servlet/       # RESTful HTTP Servlets
        │       ├── filter/        # Authentication, Authorization, and UTF-8 Filters
        │       ├── util/          # PasswordUtil (PBKDF2), ValidationUtil, JsonUtil
        │       └── server/        # FiscalDevServer embedded standalone runner
        └── webapp/
            ├── index.html         # Main art-directed application interface
            ├── login.html         # Standalone authentication redirect
            ├── assets/
            │   ├── css/
            │   │   ├── fiscal-tokens.css      # Matcha & Milk Honey tokens
            │   │   ├── fiscal-base.css        # Form components & animations
            │   │   ├── fiscal-layout.css      # Header, navigation & layout
            │   │   └── fiscal-views.css       # Split login, dashboard & modals
            │   ├── js/
            │   │   ├── fiscal-api.js          # Fetch API client abstraction
            │   │   └── fiscal-app.js          # Master UI coordinator & state
            │   └── fonts/                     # Local typography directory
            └── WEB-INF/
                └── web.xml        # Servlet 5.0 deployment descriptor
```

---

## 9. Prerequisites
- **JDK:** OpenJDK or Oracle JDK 17, 21, 24, or 27.
- **Maven:** Version 3.8+.
- **Oracle Database (Optional):** Oracle XE, 23c Free, or Enterprise.
- **Browser:** Any modern Chromium, Firefox, or Safari browser.

---

## 10. Java Installation and Verification
Verify your installed Java version:
```bash
java -version
```
Expected output:
```text
openjdk version "17.0.x" (or 21, 24, 27)
```

---

## 11. Maven Configuration
Verify Maven:
```bash
mvn -version
```

---

## 12. Oracle Database Setup
FISCAL connects to standard Oracle instances via thin JDBC connection URL:
`jdbc:oracle:thin:@localhost:1521:XE` (or service name `FREE` or `ORCLCDB`).

---

## 13. SQL*Plus Connection Instructions
Log into SQL*Plus:
```bash
sqlplus system/oracle@localhost:1521/XE
```

---

## 14. JDBC Driver Installation and Configuration
The official Oracle JDBC driver is managed via Maven dependency:
```xml
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
    <version>23.3.0.23.09</version>
</dependency>
```

---

## 15. Environment Variables
You can optionally configure connection parameters via environment variables:
- `ORACLE_URL`
- `ORACLE_USER`
- `ORACLE_PASSWORD`
- `PORT` (Default: `8080`)

---

## 16. Database Schema
Defined in `database/01_schema.sql`:
- `TAXPAYER`: `TAXPAYER_ID`, `NAME`, `PAN_NO`, `AGE`, `CITY`, `INCOME`, `CREATED_AT`
- `TAX_TYPE`: `TAX_ID`, `TAX_NAME`, `TAX_RATE`, `DESCRIPTION`, `CREATED_AT`
- `TAX_PAYMENT`: `PAYMENT_ID`, `TAXPAYER_ID`, `TAX_ID`, `AMOUNT`, `PAYMENT_DATE`, `STATUS`, `PAYMENT_REF`, `REMARKS`
- `APP_USER`: `USER_ID`, `USERNAME`, `PASSWORD_HASH`, `SALT`, `FULL_NAME`, `EMAIL`, `ROLE`, `STATUS`, `CREATED_AT`

---

## 17. DDL Execution
In SQL*Plus:
```sql
@database/01_schema.sql
@database/02_sequences.sql
```

---

## 18. DML and Sample Data
In SQL*Plus:
```sql
@database/03_seed_data.sql
```
Loads 10 realistic taxpayers across Indian metro cities, 6 statutory tax types, and 10 payment ledger transactions.

---

## 19. Secure Administrator Initialization
Seed administrator accounts are hashed with PBKDF2 (10,000 iterations, 32-byte hash) and 16-byte random salts:
- `admin` / `Admin@123` (Administrator)
- `officer` / `Officer@123` (Assessment Officer)
- `auditor` / `Auditor@123` (Compliance Auditor)

---

## 20. Build Commands
Build the deployable WAR:
```bash
mvn clean package
```
Generates `target/fiscal.war`.

---

## 21. Tomcat Setup
Download Apache Tomcat 10.1+ and set environment variables:
```bash
export CATALINA_HOME=/path/to/apache-tomcat-10.1.x
```

---

## 22. WAR Deployment
Deploy the WAR file to Tomcat:
```bash
cp target/fiscal.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh
```

---

## 23. Application URL
- **Embedded Server:** `http://localhost:8080/`
- **Tomcat:** `http://localhost:8080/fiscal/`

---

## 24. Functional Workflow
1. User logs in with authorized credentials on the split-screen editorial login view.
2. The dashboard displays live database aggregates, revenue collections, and status distributions.
3. Operators register or search taxpayers and view payment histories with masked PAN data.
4. Operators define tax categories and configured percentage rates.
5. In the assessment workbench, selecting a taxpayer and tax category runs server-side calculations.
6. The user clicks "Commit Assessment to Payment Ledger" to record a verified transaction.
7. Payment status transitions (`PAID`, `PENDING`, `REVERSED`) update the ledger and dashboard in real time.

---

## 25. Tax Calculation Logic
Authoritative formula implemented in `TaxCalculationService.java`:
$$\text{Tax Amount} = \frac{\text{Applicable Taxable Amount} \times \text{Tax Rate}}{100}$$
- Precision: `RoundingMode.HALF_UP`, scale 2.
- Input verification: Negative taxable amounts and invalid rates are strictly rejected.

---

## 26. Taxpayer Management
- **Fields:** Taxpayer ID, Full Name, PAN Number (validated with `^[A-Z]{5}[0-9]{4}[A-Z]$`), Age (18–120), City, Declared Income.
- **Operations:** Create, View Details, Edit, Delete (cascading payments), Search, City Filter, Income Sort.

---

## 27. Tax Type Management
- **Fields:** Tax ID, Tax Name, Tax Rate (0.00% to 100.00%), Description.
- **Integrity Rule:** Deletion is blocked if existing payment records reference the category.

---

## 28. Payment Management
- **Fields:** Payment ID, Taxpayer ID, Tax ID, Amount, Payment Date, Status (`PAID`, `PENDING`, `REVERSED`), Payment Reference (`FSC-2026-XXXX`), Remarks.
- **Operations:** Record Payment, Update Status, Filter by Status/Category, Delete.

---

## 29. Database Relationships
- `TAXPAYER` (1) to `TAX_PAYMENT` (M): A taxpayer has multiple payment records.
- `TAX_TYPE` (1) to `TAX_PAYMENT` (M): A tax category is associated with multiple payment records.

---

## 30. API Documentation
Exhaustive endpoint details, request payloads, and status codes are documented in [`docs/API_DOCUMENTATION.md`](docs/API_DOCUMENTATION.md).

---

## 31. Security Measures
- **Password Security:** PBKDF2WithHmacSHA256, 10,000 iterations, unique 16-byte random salts.
- **SQL Injection Prevention:** 100% JDBC `PreparedStatement` parameterized queries.
- **Session Security:** Session fixation rotation, `HttpOnly` cookie flags, 30-minute inactivity timeout.
- **XSS & Output Sanitization:** Server-side and client-side character escaping.
- **Sensitive Data Privacy:** PAN numbers masked in public displays (`ABC••••••F`).

---

## 32. Testing Instructions
Execute the automated and manual test scenarios documented in [`docs/TEST_CASES.md`](docs/TEST_CASES.md).

---

## 33. Troubleshooting
- **Oracle Connection Refused:** Check if Oracle listener is active (`lsnrctl status`). If offline, FISCAL automatically activates its internal verified repository engine.
- **Port 8080 in Use:** Run with `PORT=8090 mvn compile exec:java ...`.
- **Tomcat 404 Error:** Ensure Tomcat 10+ is used (compatible with `jakarta.servlet`).

---

## 34. Known Limitations
- External third-party payment gateway webhooks (e.g., Stripe, Razorpay) are not integrated by academic project design.
- PAN verification uses statutory regex rather than a live government API integration.

---

## 35. Future Improvements
- Automated PDF receipt generation and taxpayer tax clearance certificates.
- Multi-tier income tax slab bracket engine for progressive multi-tier tax systems.
- Comprehensive audit logging table for operator tracking.

---

## 36. Font Installation and Licensing Notes
- **Bootzy:** Expressive display typeface for large headlines.
- **AQUIRE:** Technical display typeface for wordmarks, labels, and identifiers.
- **Expected Asset Locations:**
  ```text
  src/main/webapp/assets/fonts/
  ├── Bootzy.woff2
  └── AQUIRE.woff2
  ```
- **Fallback Implementation:** In accordance with prompt guidelines, when custom font binaries are not provided locally, FISCAL provides `@font-face` declarations with graceful, premium fallbacks (`Syne` for expressive display, `Space Grotesk` for technical display, and `Inter` for supporting typography).

---

## 37. Exact Design Tokens and Colour Values
```css
:root {
  /* Primary brand colours — immutable */
  --matcha: #9CA763;
  --milk-honey: #F1E8C7;

  /* Light surfaces */
  --paper: #F1E8C7;
  --surface: #F8F3DF;
  --surface-raised: #FFFDF5;
  --surface-white: #FFFFFF;

  /* Text */
  --ink: #292D23;
  --muted: #777A65;
  --muted-strong: #555B43;

  /* Borders */
  --border: #DED9BD;
  --border-strong: #C5C8A4;

  /* Matcha variations */
  --matcha-soft: #E8EACF;
  --matcha-light: #D1D8A8;
  --matcha-mid: #84914D;
  --matcha-deep: #626E36;

  /* Semantic states */
  --danger: #A83F35;
  --danger-soft: #F6E6DF;
  --warning: #805D20;
  --warning-soft: #F5EBD0;
  --success: #46663A;
  --success-soft: #E7EBD8;
  --info: #426B68;
  --info-soft: #E2ECE4;
}
```

# FISCAL