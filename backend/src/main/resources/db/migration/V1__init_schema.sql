-- V1__init_schema.sql
-- ITR-1 Filing Assistant - Phase 1 Database Schema

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(100) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE clients (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id) ON DELETE CASCADE,
    pan         VARCHAR(10) UNIQUE NOT NULL,
    name        VARCHAR(125) NOT NULL,
    email       VARCHAR(100),
    mobile      VARCHAR(15),
    aadhaar     VARCHAR(12),
    dob         DATE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE client_year_data (
    id                  BIGSERIAL PRIMARY KEY,
    client_id           BIGINT REFERENCES clients(id) ON DELETE CASCADE,
    assessment_year     VARCHAR(10) NOT NULL,
    raw_prefill_json    TEXT,
    computed_itr1_json  TEXT,
    status              VARCHAR(20) DEFAULT 'draft',
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(client_id, assessment_year)
);

-- Indexes
CREATE INDEX idx_clients_user_id      ON clients(user_id);
CREATE INDEX idx_clients_pan          ON clients(pan);
CREATE INDEX idx_year_data_client     ON client_year_data(client_id);
CREATE INDEX idx_year_data_year       ON client_year_data(assessment_year);
