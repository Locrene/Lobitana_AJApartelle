# AJ Apartelle – Booking and Operations Management System

IT342-G01 individual project by **Maria Loraine L. Lobitana**. The full SRS is in [`docs/`](docs/).

AJ Apartelle is a booking and operations system for a small, family-run apartelle. Guests request bookings online, front-desk staff run daily operations, and the owner monitors the business.

## Current increment: Project Increment 2 – User Profile and Password Management

| Feature | What is implemented |
|---|---|
| **User profile** (FR-072) | Every role can view their own profile at `/profile` and edit the full name and mobile number, with the registration rules. Email and role are read-only. The endpoints have no user ID: the account always comes from the session. |
| **Forgot / reset password** (FR-074, FR-075) | *Forgot password?* on the login page emails a link through Gmail SMTP. The answer is the same whether or not the account exists. The token is 32 random bytes, stored only as a SHA-256 hash, valid for 30 minutes and usable once. A new request cancels earlier links, at most 3 links are sent per account per hour, and deactivated accounts get no email. |
| **Change password** (FR-073) | On the profile page. Requires the current password and a different new one with confirmation. The other sessions are signed out and the current one continues with a fresh cookie. |
| **Session invalidation** (FR-045) | Each JWT carries the user's `token_version`. A password change, a reset or a deactivation increases it, so older tokens get 401. |
| **Password rules** (FR-071, FR-073, FR-075) | At least 8 characters and at most 72 UTF-8 bytes (bcrypt reads only 72 bytes), with at least one letter and one number. Checked in the browser and on the server; a longer password never matches at sign-in. |

**New endpoints:** `POST /api/v1/auth/forgot-password`, `POST /api/v1/auth/reset-password`, `GET`/`PUT /api/v1/me/profile` and `POST /api/v1/me/password`. The full list is under [API endpoints](#api-endpoints-increments-1-and-2).

**Email setup:** reset emails need `MAIL_USERNAME`, `MAIL_PASSWORD` (a Google app password), `MAIL_FROM` and `APP_FRONTEND_URL` in `backend/.env`. See [Gmail for password reset emails](#gmail-for-password-reset-emails-free).

**Look:** the web app was restyled in this increment (pandan green, Barlow font). This changed presentation only, not behavior.

Upgrading from Increment 1 applies migration V2 automatically. Everyone has to sign in again once, because older tokens have no version.

## Project Increment 1 – User Accounts and Authentication

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
backend/                 Spring Boot REST API (/api/v1), one package per module,
                         each with controller -> service -> repository
  src/main/java/.../auth        Login, logout, JWT, auth cookie, JWT filter, change / forgot / reset password
  src/main/java/.../user        Users, own profile, owner staff management, owner seeder
  src/main/java/.../dashboard   Role dashboards
  src/main/java/.../common      Errors (ApiError), security, OpenAPI and time-zone config
  src/main/resources/db/migration Flyway SQL migrations
  src/test/java/...      Unit tests and Testcontainers integration tests
  .env.example           Template for secrets (copy to .env)
frontend/                React web app
  src/services/          All API calls (api.js, authService.js) and validation
  src/context/           AuthContext: the only place auth state lives
  src/routes/            ProtectedRoute / PublicOnlyRoute guards
  src/pages/, src/components/  UI (temporary, will be redesigned)
database/create_database.sql   Creates the DB user and database
docker-compose.yml       Local PostgreSQL 16 (laptops with Docker)
.github/workflows/ci.yml GitHub Actions CI (NFR-26)
docs/                    SRS (SRS.md living copy, submitted PDF, diagrams)
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

Pick **one** of the three options below.

#### Docker (laptop)

If Docker Desktop is installed, start PostgreSQL 16 from the repository root:

```bash
docker compose up -d
```

This creates the `ajapartelle` database and user with the password `change-me`, the same as `backend/.env.example`. To use another password, set `DB_PASSWORD` in a root `.env` file **and** in `backend/.env`. Stop it with `docker compose down`; add `-v` to delete the data.

#### PostgreSQL installed on Windows

Open **SQL Shell (psql)** from the Start menu (press Enter for the defaults, then type your `postgres` password), and run:

```sql
CREATE USER ajapartelle WITH PASSWORD 'ajapartelle';
CREATE DATABASE ajapartelle OWNER ajapartelle;
```

(Or run `psql -U postgres -f database/create_database.sql`.) You do not need to create the tables by hand: Flyway creates them when the backend starts.
If you use this option, set `DB_PASSWORD=ajapartelle` in `backend/.env` (step 3).

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
| `MAIL_USERNAME` | Gmail address that sends the password reset emails |
| `MAIL_PASSWORD` | Google **app password** for that account (see below), not the Gmail password |
| `MAIL_FROM` | Sender address, normally the same as `MAIL_USERNAME` |
| `APP_FRONTEND_URL` | Web app base URL used in the reset link (default `http://localhost:5173`) |
| `OWNER_EMAIL`, `OWNER_PASSWORD`, `OWNER_FULL_NAME`, `OWNER_MOBILE` | First owner account (seeded once, password stored as a bcrypt hash; the password must be at least 8 characters and at most 72 bytes) |

`.env` is gitignored. Never commit real secrets.

#### Gmail for password reset emails (free)

1. Turn on **2-Step Verification** for the sending Google account.
2. Create an app password at https://myaccount.google.com/apppasswords and put it in `MAIL_PASSWORD`.
3. Restart the backend.

Mail is sent in the background over `smtp.gmail.com:587` with STARTTLS. If sending fails, the page still shows the normal message and the backend logs `Password reset email failed (<reason>)`. `MailAuthenticationException` means the address or app password is wrong, and `MailSendException` usually means port 587 is blocked on the network. There is no automatic retry: request a new link.

The link opens `APP_FRONTEND_URL`, so with the localhost default it works only on the PC running the app.

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

## API endpoints (Increments 1 and 2)

| Method | Path | Access |
|---|---|---|
| POST | `/api/v1/auth/register` | Public (creates GUEST) |
| POST | `/api/v1/auth/login` | Public (sets httpOnly cookie) |
| POST | `/api/v1/auth/logout` | Public (revokes token, clears cookie) |
| GET | `/api/v1/auth/me` | Any signed-in user |
| POST | `/api/v1/auth/forgot-password` | Public (`{"email"}`; always the same 200 message) |
| POST | `/api/v1/auth/reset-password` | Public (`{"token", "newPassword", "confirmPassword"}`) |
| GET, PUT | `/api/v1/me/profile` | Any signed-in user, own account (`{"fullName", "mobileNumber"}`) |
| POST | `/api/v1/me/password` | Any signed-in user, own account (`{"currentPassword", "newPassword", "confirmPassword"}`) |
| GET | `/api/v1/guest/dashboard` | GUEST |
| GET | `/api/v1/staff/dashboard` | STAFF, OWNER |
| GET | `/api/v1/owner/dashboard` | OWNER |
| GET, POST | `/api/v1/owner/staff` | OWNER |
| PATCH | `/api/v1/owner/staff/{id}` | OWNER (`{"active": true/false}`) |

Errors use the SRS format `{"code": "...", "message": "..."}`, plus `fieldErrors` for invalid fields, with HTTP 400, 401, 403, 404, 405, 409, 415 or 500.

**API documentation:** with the backend running, open **http://localhost:8080/swagger-ui.html** (OpenAPI JSON at `/v3/api-docs`). Endpoints are grouped by module (Auth, Password, Profile, User, Dashboard). Protected endpoints use the login cookie, so call `POST /api/v1/auth/login` in Swagger UI first.

## Time zone

Business dates and schedules use **Asia/Manila** (`app.timezone` in `application.properties`). Timestamps are stored as `TIMESTAMPTZ`, and JSON dates are returned in Manila time.

## Tests and checks

```bash
cd backend
mvnw.cmd verify        # unit + integration tests, coverage report in target/site/jacoco/index.html
cd ../frontend
npm ci
npm run lint
npm run build
```

The integration tests start a temporary PostgreSQL 16 with **Testcontainers**, so they need Docker running. Without Docker they are skipped, and only the unit tests run. They always run in CI. The mail sender is mocked in these tests, so they never send email.

## Continuous integration (NFR-26)

`.github/workflows/ci.yml` runs on every push to `main` and on every pull request (from any branch):

| Job | What it does |
|---|---|
| `backend` | Java 21, `./mvnw verify` (tests against PostgreSQL 16), uploads the JaCoCo report |
| `web` | Node LTS, `npm ci`, `npm run lint`, `npm run build` |
| `secret-scan` | gitleaks over the full git history |
| `build-artifacts` | Pushes to `main` only: builds and uploads the backend JAR |

To block merges when CI fails, turn on branch protection for `main` in GitHub (**Settings → Branches**) and require the four checks.

## Upcoming increments

These come from the SRS: room listing and availability, booking requests, staff daily workflow, payments, expenses, owner reports, Google Sign-In, Google Calendar sync, SMS notifications, and the React Native mobile app.
