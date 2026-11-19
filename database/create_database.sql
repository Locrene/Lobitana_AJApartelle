-- Run once as the PostgreSQL superuser (postgres), e.g.:
--   psql -U postgres -f database/create_database.sql
-- Change the password here AND in backend/.env (DB_PASSWORD) if you use a different one.
CREATE USER ajapartelle WITH PASSWORD 'ajapartelle';
CREATE DATABASE ajapartelle OWNER ajapartelle;
-- Tables are created automatically by Flyway when the backend starts
-- (backend/src/main/resources/db/migration).
