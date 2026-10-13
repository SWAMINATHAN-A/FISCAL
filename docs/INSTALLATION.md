# FISCAL — Installation, Database Setup & Deployment Guide

## 1. Prerequisites
Ensure the following software packages are installed on your host system:
1. **Java Development Kit (JDK):** Version 17, 21, 24, or 27.
2. **Apache Maven:** Version 3.8+ (Install via `brew install maven` or standard package manager).
3. **Oracle Database (Optional for Production):** Oracle Database Express Edition (XE), Oracle Database 23c Free, or Enterprise Oracle Database.
4. **Application Server:** Apache Tomcat 10+ (for standard WAR deployment) OR built-in zero-dependency launcher `FiscalDevServer`.

---

## 2. Oracle Database Setup (SQL*Plus)

### 2.1 Connect to Oracle as Administrator
```bash
sqlplus system/oracle@localhost:1521/XE
```
*(Replace `XE` with `FREE`, `ORCL`, or your pluggable database name `XEPDB1` if applicable)*.

### 2.2 Optional: Create Dedicated Application User/Schema
```sql
CREATE USER c##fiscal IDENTIFIED BY FiscalPass#2026;
GRANT CONNECT, RESOURCE, DBA TO c##fiscal;
GRANT UNLIMITED TABLESPACE TO c##fiscal;
CONNECT c##fiscal/FiscalPass#2026@localhost:1521/XE;
```

### 2.3 Execute Database Scripts in Sequence
Execute the SQL scripts located in the `database/` folder:

```sql
-- 1. Create Tables, Constraints, Checks, and Indexes
@database/01_schema.sql

-- 2. Create Sequences and Auto-Increment Triggers
@database/02_sequences.sql

-- 3. Load Seed Data & Verified Records
@database/03_seed_data.sql

-- 4. Verify Schema Health and Integrity
@database/05_verification.sql
```

---

## 3. Database Configuration (`src/main/resources/db.properties`)

Configure your Oracle JDBC URL, username, and password in `src/main/resources/db.properties`:

```properties
db.driver=oracle.jdbc.OracleDriver
db.url=jdbc:oracle:thin:@localhost:1521:XE
db.user=system
db.password=oracle
db.allowFallback=true
```

You may also configure environment variables without editing files:
```bash
export ORACLE_URL="jdbc:oracle:thin:@localhost:1521:XE"
export ORACLE_USER="system"
export ORACLE_PASSWORD="your_password"
```

*Note: If Oracle Database is temporarily offline or unreachable on your workstation, FISCAL's built-in offline engine will seamlessly keep all APIs and UI functional using the exact seed data.*

---

## 4. Building the Application with Maven

From the project root directory (`/Users/arpitraj/Desktop/FISCAL`):

```bash
mvn clean package
```

This compiles all Java source files and generates `target/fiscal.war`.

---

## 5. Deployment Options

### Option A: Instant Zero-Setup Development Server (Recommended for Fast Evaluation)
Run the embedded standalone runner directly from the project directory:

```bash
# Run using compiled classes and dependencies
mvn compile exec:java -Dexec.mainClass="com.fiscal.server.FiscalDevServer"
```
Or with Java:
```bash
java -cp "target/classes:$(mvn dependency:build-classpath | grep -v '\[INFO\]' | tr '\n' ':')" com.fiscal.server.FiscalDevServer
```

Access the application in your browser at:
`http://localhost:8080`

### Option B: Deploying to Apache Tomcat 10+
1. Copy the generated WAR file:
   ```bash
   cp target/fiscal.war $CATALINA_HOME/webapps/
   ```
2. Start Apache Tomcat:
   ```bash
   $CATALINA_HOME/bin/startup.sh
   ```
3. Open your browser:
   `http://localhost:8080/fiscal/`

---

## 6. Default Demonstration Accounts

| Role | Username | Password | Privileges |
|---|---|---|---|
| **Chief Administrator** | `admin` | `Admin@123` | Full access: Taxpayers, Tax Types, Payments, Operator Administration |
| **Assessment Officer** | `officer` | `Officer@123` | Taxpayer Management, Calculation, Payment Recording |
| **Compliance Auditor** | `auditor` | `Auditor@123` | Read-only ledger inspection, Status review |
