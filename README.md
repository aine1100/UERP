# National Utility Billing System

Spring Boot REST API for **WASAC Water** and **REG Electricity** utility billing in Rwanda.  
Built for academic demonstration with layered architecture, RBAC, JWT auth, and **PostgreSQL database routines**.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17, Spring Boot 3.2.5 |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Reports | OpenCSV, Apache POI, OpenPDF |

---

## Features

- **Authentication**: OTP invite flow, login, refresh tokens, password reset
- **RBAC**: `ADMIN`, `OPERATOR`, `FINANCE`, `CUSTOMER`
- **Billing**: Meter readings → automatic bill generation → payments
- **Locations**: Rwanda administrative hierarchy (`locations.json`)
- **UUID IDs**: All entities use standardized UUID primary keys
- **Database routines**: Triggers + stored procedure with cursor (see below)

---

## Database Routines (Invigilator Demo)

SQL lives in [`src/main/resources/db/routines.sql`](src/main/resources/db/routines.sql).  
Installed automatically on startup by `DatabaseRoutineInstaller`.

### 1. Trigger — Bill generation notification

| Object | Purpose |
|--------|---------|
| `fn_trg_bill_after_insert()` | Function called by trigger |
| `trg_bill_after_insert` | **AFTER INSERT** on `bills` |

**Behavior:** When an operator submits a reading and Java saves a new bill, PostgreSQL automatically inserts a `BILL_GENERATED` row into `notification_messages`.

### 2. Trigger — Full payment status + notification

| Object | Purpose |
|--------|---------|
| `fn_trg_payment_after_insert()` | Function called by trigger |
| `trg_payment_after_insert` | **AFTER INSERT** on `payments` |

**Behavior:** When a payment is recorded:

1. Recalculates `outstanding_balance`
2. Sets bill status to `PAID` (full) or `PARTIAL`
3. Inserts `PAYMENT_RECEIVED` notification
4. If fully paid, inserts `BILL_PAID` notification

Java (`PaymentService`) only inserts the payment row — the trigger enforces billing rules at DB level.

### 3. Stored procedure with CURSOR — Overdue reminders

| Object | Purpose |
|--------|---------|
| `sp_send_overdue_reminders()` | Procedure using a **server-side cursor** |

**Behavior:** Iterates all `UNPAID` / `PARTIAL` bills and inserts `OVERDUE_REMINDER` notifications.

**Invoke from API:**

```http
POST /api/notifications/send-overdue-reminders
Authorization: Bearer <finance-jwt>
```

Or directly in PostgreSQL:

```sql
CALL sp_send_overdue_reminders();
```

### View notifications

```http
GET /api/notifications
Authorization: Bearer <jwt>
```

Customers see their own messages; `ADMIN` / `FINANCE` see all.  
When a bill is generated or a payment is recorded, the matching DB notification is **automatically emailed** to the customer. Customers can also re-send any notification:

```http
POST /api/notifications/{id}/send-email
```

### Customer meter self-service

```http
GET  /api/meters/my              # list own meters
POST /api/meters/my              # register a meter (no customerId needed)
GET  /api/meters/{id}            # view own meter
```

---

## How Java and DB Work Together

```
Operator submits reading
        │
        ▼
ReadingService → BillService.generateBillFromReading()
        │
        ▼
INSERT INTO bills  ──trigger──►  notification_messages (BILL_GENERATED)
        │
        ▼
EmailService (application-level email)

Customer/Finance pays bill
        │
        ▼
PaymentService.processPayment() → INSERT INTO payments
        │
        ▼
trigger trg_payment_after_insert
        ├── UPDATE bills (balance + status)
        └── INSERT notification_messages (PAYMENT_RECEIVED, BILL_PAID)
```

Key classes with explanatory comments:

- `BillService` — bill generation; notes trigger side-effect
- `PaymentService` — payment insert; DB trigger updates bill
- `DatabaseRoutineInstaller` — loads `routines.sql` on startup
- `NotificationController` — exposes trigger/procedure output

---

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+ (for `EXECUTE FUNCTION` trigger syntax)
- Gmail SMTP credentials (optional, for emails)

---

## Configuration

Edit [`src/main/resources/application.properties`](src/main/resources/application.properties):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/utility
spring.datasource.username=postgres
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=create   # recreate schema on each run (dev)

app.admin.email=your-admin@email.com
```

> **Note:** `ddl-auto=create` drops and recreates tables on every startup, then reinstalls triggers/procedures.

---

## Run the Application

```bash
mvn spring-boot:run
```

- API: http://localhost:8080  
- Swagger UI: http://localhost:8080/swagger-ui.html  

Default admin is seeded on first run (OTP emailed to `app.admin.email`).

---

## Auth Flow (Admin / Staff / Customer)

1. Admin invites user via `/api/users/invite/operator`, `/finance`, or `/customer`
2. User verifies OTP: `POST /api/auth/verify-account`
3. User sets password: `POST /api/auth/setup-password`
4. User logs in: `POST /api/auth/login`

---

## Main API Groups

| Tag | Base Path | Roles |
|-----|-----------|-------|
| Authentication | `/api/auth` | Public (except invite) |
| Users | `/api/users` | ADMIN |
| Customers | `/api/customers` | ADMIN, OPERATOR, FINANCE |
| Meters | `/api/meters` | ADMIN, OPERATOR, FINANCE (all); CUSTOMER (`/my` only) |
| Readings | `/api/readings` | OPERATOR (create) |
| Bills | `/api/bills` | FINANCE, CUSTOMER |
| Payments | `/api/payments` | FINANCE, CUSTOMER |
| Notifications | `/api/notifications` | ADMIN, FINANCE, CUSTOMER |
| Tariffs | `/api/tariffs` | ADMIN, FINANCE |
| Reports | `/api/reports` | FINANCE |
| Locations | `/api/locations` | Public |

---

## Demonstrating DB Routines to Invigilators

1. **Show SQL file:** `src/main/resources/db/routines.sql`
2. **Start app** — check logs for `PostgreSQL billing routines installed`
3. **Submit a reading** (Swagger) → `GET /api/notifications` shows `BILL_GENERATED`
4. **Process full payment** → notifications show `PAYMENT_RECEIVED` + `BILL_PAID`, bill status `PAID`
5. **Run overdue procedure** → `POST /api/notifications/send-overdue-reminders`
6. **In pgAdmin/psql:**

```sql
-- List triggers
SELECT tgname, relname FROM pg_trigger t
JOIN pg_class c ON t.tgrelid = c.oid
WHERE tgname LIKE 'trg_%';

-- View notifications
SELECT notification_type, message, created_at
FROM notification_messages
ORDER BY created_at DESC;
```

---

## Project Structure

```
src/main/java/com/national/utility/billing/
├── config/          # Security, OpenAPI, DB routine installer
├── controller/      # REST endpoints
├── dto/             # Request/response objects
├── model/           # JPA entities (UUID-based)
├── repository/      # Spring Data repositories
├── security/        # JWT, RBAC, role permissions catalog
├── service/         # Business logic
└── validation/      # Rwanda-specific validators

src/main/resources/
├── application.properties
├── db/routines.sql  # Triggers, functions, stored procedure
└── locations.json   # Rwanda location hierarchy
```

---

## License

Academic project — National Utility Billing System.
