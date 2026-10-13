# FISCAL — API Specification & Servlet Endpoints

## 1. Overview
FISCAL provides a clean, RESTful JSON API layer implemented through Java Servlets (`jakarta.servlet.*`), JDBC `PreparedStatement` transactions, and Google Gson serialization.

All endpoints adhere to a standardized JSON response envelope:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": 1776000000000
}
```

---

## 2. Authentication Endpoints

### 2.1 Operator Sign In
- **Method:** `POST`
- **Path:** `/api/auth/login`
- **Payload:**
  ```json
  {
    "username": "admin",
    "password": "Admin@123"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Login successful",
    "data": {
      "userId": 101,
      "username": "admin",
      "fullName": "Arpit Raj (Chief Administrator)",
      "email": "admin@fiscal.internal",
      "role": "ADMIN",
      "status": "ACTIVE"
    }
  }
  ```

### 2.2 Operator Sign Out
- **Method:** `POST`
- **Path:** `/api/auth/logout`
- **Response (200 OK):** Session invalidated, cookie cleared.

### 2.3 Verify Active Session
- **Method:** `GET`
- **Path:** `/api/auth/status` (or `/api/auth/me`)

---

## 3. Financial Dashboard

### 3.1 Get Overview Summary
- **Method:** `GET`
- **Path:** `/api/dashboard/summary`
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "totalTaxpayers": 10,
      "totalPayments": 10,
      "totalPaidRevenue": 1891000.00,
      "totalPendingAmount": 570500.00,
      "totalReversedAmount": 240000.00,
      "grossTurnoverMonitored": 24680000.00,
      "statusDistribution": {
        "PAID": 8,
        "PENDING": 2,
        "REVERSED": 1
      },
      "recentPayments": [ ... ],
      "taxCategoriesBreakdown": [ ... ]
    }
  }
  ```

---

## 4. Taxpayer Management

### 4.1 List Taxpayers (with Search, City Filter & Sort)
- **Method:** `GET`
- **Path:** `/api/taxpayers?search=sharma&city=Mumbai&sort=income_desc`

### 4.2 Get Taxpayer by ID (with Ledger Totals)
- **Method:** `GET`
- **Path:** `/api/taxpayers/{id}`

### 4.3 Distinct Jurisdictional Cities
- **Method:** `GET`
- **Path:** `/api/taxpayers/cities`

### 4.4 Register Taxpayer
- **Method:** `POST`
- **Path:** `/api/taxpayers`
- **Payload:**
  ```json
  {
    "name": "Rajesh Kumar",
    "panNo": "ABCDE1234F",
    "age": 36,
    "city": "Mumbai",
    "income": 2200000.00
  }
  ```

### 4.5 Update Taxpayer
- **Method:** `PUT`
- **Path:** `/api/taxpayers/{id}`

### 4.6 Delete Taxpayer
- **Method:** `DELETE`
- **Path:** `/api/taxpayers/{id}`

---

## 5. Tax Type & Rate Management

### 5.1 List Tax Categories
- **Method:** `GET`
- **Path:** `/api/tax-types`

### 5.2 Create Tax Category
- **Method:** `POST`
- **Path:** `/api/tax-types`
- **Payload:**
  ```json
  {
    "taxName": "Environmental Carbon Surcharge",
    "taxRate": 6.50,
    "description": "Statutory surcharge on heavy industrial energy consumption"
  }
  ```

### 5.3 Update Tax Category
- **Method:** `PUT`
- **Path:** `/api/tax-types/{id}`

### 5.4 Delete Tax Category
- **Method:** `DELETE`
- **Path:** `/api/tax-types/{id}`
- **Note:** Safely rejected with HTTP 409 Conflict if existing payments reference this category.

---

## 6. Statutory Tax Calculation & Assessment

### 6.1 Execute Calculation
- **Method:** `POST`
- **Path:** `/api/tax/calculate`
- **Payload:**
  ```json
  {
    "taxpayerId": 1001,
    "taxId": 201,
    "taxableAmount": 1850000.00
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Tax calculation computed successfully",
    "data": {
      "taxpayerId": 1001,
      "taxpayerName": "Aarav Sharma",
      "panNo": "ABCPS1234F",
      "taxId": 201,
      "taxName": "Income Tax",
      "taxRate": 15.00,
      "applicableAmount": 1850000.00,
      "calculatedTax": 277500.00,
      "calculationFormula": "1850000.00 × 15.00% ÷ 100 = 277500.00",
      "timestamp": "2026-10-12 18:30:00"
    }
  }
  ```

---

## 7. Tax Payment Ledger

### 7.1 List Payments (with Search, Status & Tax Type Filter)
- **Method:** `GET`
- **Path:** `/api/payments?status=PAID&sort=amount_desc`

### 7.2 Record New Tax Payment
- **Method:** `POST`
- **Path:** `/api/payments`
- **Payload:**
  ```json
  {
    "taxpayerId": 1001,
    "taxId": 201,
    "amount": 277500.00,
    "status": "PAID",
    "remarks": "Advance tax final fourth quarter installment"
  }
  ```

### 7.3 Update Payment Status
- **Method:** `POST`
- **Path:** `/api/payments/{id}/status`
- **Payload:**
  ```json
  {
    "status": "PAID",
    "remarks": "Clearance confirmed by central treasury batch reconciliation"
  }
  ```

### 7.4 Delete Payment Record
- **Method:** `DELETE`
- **Path:** `/api/payments/{id}`
