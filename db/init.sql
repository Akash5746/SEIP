-- ─────────────────────────────────────────────────────────────────
-- SEIP Database Initialisation Script
-- Runs automatically when the postgres container starts for the
-- first time (mounted at /docker-entrypoint-initdb.d/init.sql).
-- ─────────────────────────────────────────────────────────────────

-- Create per-service schemas to maintain logical isolation
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS expense;
CREATE SCHEMA IF NOT EXISTS fraud;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS analytics;

-- Grant the seip_user access to all schemas
GRANT USAGE  ON SCHEMA auth, users, expense, fraud, notification, audit, analytics TO seip_user;
GRANT CREATE ON SCHEMA auth, users, expense, fraud, notification, audit, analytics TO seip_user;

-- ── Auth Schema ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS auth.users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_expired    BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked     BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS auth.roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS auth.user_roles (
    user_id BIGINT NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES auth.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS auth.refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token      TEXT        NOT NULL UNIQUE,
    expiry_date TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Users Schema ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users.profiles (
    id           BIGSERIAL PRIMARY KEY,
    auth_user_id BIGINT       NOT NULL UNIQUE,
    first_name   VARCHAR(100) NOT NULL,
    last_name    VARCHAR(100) NOT NULL,
    department   VARCHAR(100),
    manager_id   BIGINT,
    monthly_limit NUMERIC(12,2) DEFAULT 5000.00,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Expense Schema ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS expense.expenses (
    id              BIGSERIAL PRIMARY KEY,
    expense_number  VARCHAR(30)    NOT NULL UNIQUE,
    employee_id     BIGINT         NOT NULL,
    category_id     BIGINT,
    title           VARCHAR(200)   NOT NULL,
    description     VARCHAR(2000),
    amount          NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    currency        VARCHAR(10)    NOT NULL DEFAULT 'INR',
    merchant_name   VARCHAR(200),
    expense_date    DATE           NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    risk_score      INT            NOT NULL DEFAULT 0,
    risk_level      VARCHAR(10)    NOT NULL DEFAULT 'LOW',
    reviewer_id     BIGINT,
    review_notes    VARCHAR(2000),
    submitted_at    TIMESTAMPTZ,
    reviewed_at     TIMESTAMPTZ,
    reimbursed_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_expense_employee_id ON expense.expenses(employee_id);
CREATE INDEX IF NOT EXISTS idx_expense_status      ON expense.expenses(status);
CREATE INDEX IF NOT EXISTS idx_expense_submitted   ON expense.expenses(submitted_at DESC);

-- ── Seed expense categories ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS expense.expense_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    code        VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO expense.expense_categories (code, name, description, is_active)
VALUES
  ('FOOD',          'Food & Dining',       'Meals, restaurants, catering',              true),
  ('TRAVEL',        'Travel',              'Flights, hotels, transportation',           true),
  ('ACCOMMODATION', 'Accommodation',       'Hotel stays, lodging',                      true),
  ('OFFICE',        'Office Supplies',     'Stationery, equipment',                     true),
  ('TECH',          'Technology',          'Software, hardware, subscriptions',         true),
  ('TRAINING',      'Training & Education','Courses, conferences, certifications',      true),
  ('MARKETING',     'Marketing',           'Advertising, events, promotions',           true),
  ('ENTERTAINMENT', 'Entertainment',       'Client entertainment, team events',         true),
  ('COMMUNICATION', 'Communication',       'Phone, internet, postal',                   true),
  ('OTHER',         'Other',               'Miscellaneous expenses',                    true)
ON CONFLICT DO NOTHING;

-- ── Fraud Schema ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS fraud.fraud_records (
    id                BIGSERIAL PRIMARY KEY,
    expense_id        BIGINT         NOT NULL,
    user_id           BIGINT         NOT NULL,
    fraud_probability NUMERIC(5, 4)  NOT NULL,
    risk_level        VARCHAR(10)    NOT NULL,
    model_version     VARCHAR(50)    NOT NULL DEFAULT 'heuristic-v1',
    flagged           BOOLEAN        NOT NULL DEFAULT FALSE,
    reviewed          BOOLEAN        NOT NULL DEFAULT FALSE,
    reviewed_by       BIGINT,
    notes             TEXT,
    detected_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fraud_expense_id  ON fraud.fraud_records(expense_id);
CREATE INDEX IF NOT EXISTS idx_fraud_risk_level  ON fraud.fraud_records(risk_level);
CREATE INDEX IF NOT EXISTS idx_fraud_flagged     ON fraud.fraud_records(flagged) WHERE flagged = TRUE;

-- ── Notification Schema ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notification.notifications (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    type         VARCHAR(50) NOT NULL,
    subject      VARCHAR(255),
    body         TEXT,
    channel      VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    sent         BOOLEAN     NOT NULL DEFAULT FALSE,
    sent_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Audit Schema ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit.audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    action      VARCHAR(100)  NOT NULL,
    entity_type VARCHAR(100),
    entity_id   BIGINT,
    old_value   JSONB,
    new_value   JSONB,
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    occurred_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_user_id     ON audit.audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity      ON audit.audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_occurred_at ON audit.audit_logs(occurred_at DESC);

-- ── Analytics Schema ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS analytics.expense_summaries (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT         NOT NULL,
    period_year    INT            NOT NULL,
    period_month   INT            NOT NULL,
    total_amount   NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_count    INT            NOT NULL DEFAULT 0,
    approved_count INT            NOT NULL DEFAULT 0,
    rejected_count INT            NOT NULL DEFAULT 0,
    fraud_count    INT            NOT NULL DEFAULT 0,
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, period_year, period_month)
);

-- ── Seed admin user (password: admin123 BCrypt hash) ─────────────
INSERT INTO auth.roles (name)
VALUES ('ROLE_EMPLOYEE'), ('ROLE_MANAGER'), ('ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO auth.users (
    username,
    email,
    password,
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired
)
VALUES (
    'admin',
    'admin@seip.com',
    '$2b$12$2I8i6ftOGxbV0tJWnk3WrO5QbEua9zxWAoKwFTs6Pl4lgoLJwki2u',
    TRUE,
    TRUE,
    TRUE,
    TRUE
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
JOIN auth.roles r ON r.name = 'ROLE_ADMIN'
WHERE u.email = 'admin@seip.com'
ON CONFLICT DO NOTHING;
