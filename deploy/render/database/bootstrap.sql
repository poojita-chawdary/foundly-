-- Optional: run as a database operator on an externally supplied MySQL 8 server.
-- The Render MySQL container already does this through MYSQL_DATABASE.
-- This creates ONLY an empty database. Configure a dedicated user separately.
CREATE DATABASE IF NOT EXISTS foundly
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Do NOT manually import V1__initial_schema.sql into the application database.
-- Spring Boot/Flyway applies it and records the checksum at first startup.
-- Canonical tables, indexes and seed data:
-- backend/src/main/resources/db/migration/V1__initial_schema.sql
