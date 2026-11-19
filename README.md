# AJ Apartelle – Booking and Operations Management System

IT342-G01 individual project by **Maria Loraine L. Lobitana**. The full SRS is in [`docs/`](docs/).

AJ Apartelle is a booking and operations system for a small, family-run apartelle. Guests request bookings online, front-desk staff run daily operations, and the owner monitors the business.

## Current increment: Project Increment 1 – User Accounts and Authentication

| Feature | What is implemented |
|---|---|
| **User account creation** | Guest self-registration (full name, email, PH mobile `09XXXXXXXXX`, password + confirmation). Fields are validated on the frontend **and** the backend. Duplicate emails are blocked by an API check (HTTP 409) **and** a case-insensitive unique index in PostgreSQL. Passwords are hashed with **bcrypt** (cost 12). Public registration always creates role **GUEST**. The **OWNER** creates **STAFF** accounts, and the first OWNER is seeded from `.env`. |
| **Login and logout** | Email + password login. The error message ("Invalid email or password.") never says which of the two was wrong. A JWT (HS256, 8-hour expiry per SRS FR-042) is stored in an **httpOnly, SameSite=Strict cookie**. Logout revokes the token server-side (`revoked_tokens` table) and clears the cookie. A deactivated account is refused even if its token has not expired (FR-045). |
| **Role-based dashboards** | Guest, Staff and Owner dashboards with role-specific navigation. After login, each user is redirected to their own dashboard. Frontend route guards send signed-out users to `/login` and wrong-role users to a 403 page. Backend URL rules and `@PreAuthorize` return **401** (not signed in) or **403** (wrong role). |

### Roles (SRS Section 2.2 / Appendix F)

| Role | How the account is created | Can access |
|---|---|---|
| `GUEST` | Self-registration at `/register` | Guest dashboard (`/guest`, `/api/v1/guest/**`) |
| `STAFF` | The owner adds it at **Staff Accounts** (`/owner/staff`) | Staff dashboard (`/staff`, `/api/v1/staff/**`) |
| `OWNER` | Created automatically on first backend start from `OWNER_*` values in `backend/.env` (only if no owner exists) | Owner dashboard, Staff Accounts (add / deactivate / reactivate staff) and the Staff Workspace (the owner can do any staff task) |

> **Note on sign-in method:** The SRS (FR-041) plans Google OAuth 2.0 sign-in. This increment's assignment requires registration with securely hashed passwords, so email/password authentication is implemented now. The JWT, roles and access rules are built so that Google Sign-In can be added later as another way to get the same JWT.

## Tech stack (per SRS Section 2.3)

- **Frontend:** ReactJS 18 (Vite), React Router
- **Backend:** Java 21, Spring Boot 3.3 (Spring Web, Spring Data JPA, Spring Security, Validation), JJWT
- **Database:** PostgreSQL 16, schema managed by **Flyway** migrations (`backend/src/main/resources/db/migration`)

## Project structure

```
backend/                 Spring Boot REST API (/api/v1)
  src/main/java/.../config       SecurityConfig, owner seeder
  src/main/java/.../security     JWT service, cookie handling, auth filter
  src/main/java/.../controller   Auth, dashboards, owner staff management
  src/main/resources/db/migration Flyway SQL migrations
  .env.example           Template for secrets (copy to .env)
frontend/                React web app
  src/services/          All API calls (api.js, authService.js) and validation
  src/context/           AuthContext: the only place auth state lives
  src/routes/            ProtectedRoute / PublicOnlyRoute guards
  src/pages/, src/components/  UI (temporary, will be redesigned)
database/create_database.sql   Creates the DB user and database
docs/                    SRS
```

## Prerequisites (Windows)

1. **Git**: https://git-scm.com/download/win
2. **Java 21 JDK** (Eclipse Temurin 21): https://adoptium.net/ (during setup, enable *Set JAVA_HOME variable*)
3. **Node.js LTS**: https://nodejs.org/
4. **PostgreSQL 16**: https://www.postgresql.org/download/windows/ (EDB installer; remember the `postgres` password; keep port 5432)

You do not need to install Maven, because the repository includes the Maven wrapper (`mvnw` / `mvnw.cmd`).

## Setup

### 1. Clone

```bash
git clone https://github.com/Locrene/Lobitana_AJApartelle.git
cd Lobitana_AJApartelle
```

### 2. Database

Open **SQL Shell (psql)** from the Start menu (press Enter for the defaults, then type your `postgres` password), and run:

```sql
CREATE USER ajapartelle WITH PASSWORD 'ajapartelle';
CREATE DATABASE ajapartelle OWNER ajapartelle;
```

(Or run `psql -U postgres -f database/create_database.sql`.) You do not need to create the tables by hand: Flyway creates them when the backend starts.
#### No admin rights? (school/lab PCs)
Extract the PostgreSQL 16 Windows binaries ZIP (enterprisedb.com/download-postgresql-binaries) to `%USERPROFILE%`, then in `%USERPROFILE%\pgsql\bin` run `initdb -D %USERPROFILE%\pgdata -U postgres -A trust -E UTF8` and `pg_ctl -D %USERPROFILE%\pgdata -l %USERPROFILE%\pg.log start`, then create the user and database with `psql -U postgres` using the SQL above. No administrator rights are needed.


### 3. Backend `.env`

```bash
cd backend
copy .env.example .env
```

Edit `backend/.env`:

| Variable | Meaning |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection (must match step 2) |
| `JWT_SECRET` | Random string, **at least 32 characters** |
| `JWT_EXPIRATION_MINUTES` | Token lifetime (default 480 = 8 hours) |
| `COOKIE_SECURE` | `true` only when served over HTTPS |
| `FRONTEND_ORIGIN` | Frontend URL (default `http://localhost:5173`) |
| `OWNER_EMAIL`, `OWNER_PASSWORD`, `OWNER_FULL_NAME`, `OWNER_MOBILE` | First owner account (seeded once, password stored as a bcrypt hash) |

`.env` is gitignored. Never commit real secrets.

### 4. Run the backend (terminal 1)

```bash
cd backend
mvnw.cmd spring-boot:run
```

(macOS/Linux: `./mvnw spring-boot:run`.) Wait for `Started AjApartelleApplication`. The first run also logs `Initial OWNER account created for ...`. The API runs at `http://localhost:8080/api/v1`.

### 5. Run the frontend (terminal 2)

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. The Vite dev server forwards `/api` to the backend.

## Creating each type of account

- **Owner:** set `OWNER_*` in `backend/.env` before the first start. It is created once, when no owner exists yet.
- **Guest:** click *Create a guest account* on the login page.
- **Staff:** sign in as the owner, open **Staff Accounts**, fill in the form, and click **Add staff**. The owner can also deactivate and reactivate staff there.

## API endpoints (this increment)

| Method | Path | Access |
|---|---|---|
| POST | `/api/v1/auth/register` | Public (creates GUEST) |
| POST | `/api/v1/auth/login` | Public (sets httpOnly cookie) |
| POST | `/api/v1/auth/logout` | Public (revokes token, clears cookie) |
| GET | `/api/v1/auth/me` | Any signed-in user |
| GET | `/api/v1/guest/dashboard` | GUEST |
| GET | `/api/v1/staff/dashboard` | STAFF, OWNER |
| GET | `/api/v1/owner/dashboard` | OWNER |
| GET, POST | `/api/v1/owner/staff` | OWNER |
| PATCH | `/api/v1/owner/staff/{id}` | OWNER (`{"active": true/false}`) |

Errors use the SRS format `{"code": "...", "message": "..."}` with HTTP 400, 401, 403, 404 or 409.

## Upcoming increments

These come from the SRS: room listing and availability, booking requests, staff daily workflow, payments, expenses, owner reports, Google Sign-In, Google Calendar sync, SMS notifications, and the React Native mobile app.
