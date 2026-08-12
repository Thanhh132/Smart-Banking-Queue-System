CREATE TABLE IF NOT EXISTS branches (
    branch_id BIGSERIAL PRIMARY KEY,
    branch_code VARCHAR(50) NOT NULL UNIQUE,
    branch_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    bank_name VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    phone VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS counters (
    counter_id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    counter_code VARCHAR(50) NOT NULL,
    counter_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_counters_branch
        FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
);

CREATE TABLE IF NOT EXISTS services (
    service_id BIGSERIAL PRIMARY KEY,
    service_code VARCHAR(50) NOT NULL UNIQUE,
    service_name VARCHAR(255) NOT NULL,
    description TEXT,
    estimated_time INTEGER NOT NULL DEFAULT 15
);

CREATE TABLE IF NOT EXISTS customers (
    customer_id BIGSERIAL PRIMARY KEY,
    keycloak_user_id VARCHAR(100) UNIQUE,
    identity_provider VARCHAR(30),
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(255),
    citizen_id VARCHAR(50),
    vip_level VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS appointments (
    appointment_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appointments_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id),

    CONSTRAINT fk_appointments_branch
        FOREIGN KEY (branch_id) REFERENCES branches(branch_id),

    CONSTRAINT fk_appointments_service
        FOREIGN KEY (service_id) REFERENCES services(service_id)
);

CREATE TABLE IF NOT EXISTS tickets (
    ticket_id BIGSERIAL PRIMARY KEY,
    ticket_no VARCHAR(20) NOT NULL UNIQUE,
    appointment_id BIGINT NOT NULL UNIQUE,
    counter_id BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    called_at TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT fk_tickets_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id),

    CONSTRAINT fk_tickets_counter
        FOREIGN KEY (counter_id) REFERENCES counters(counter_id)
);

CREATE TABLE IF NOT EXISTS queue_logs (
    log_id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_queue_logs_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id)
);

CREATE TABLE IF NOT EXISTS ratings (
    rating_id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL UNIQUE,
    score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ratings_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id)
);

-- Phase 1/2 account/profile expansion. Legacy customer/appointment tables above are
-- intentionally retained until their own migration is approved.
CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    keycloak_user_id VARCHAR(255),
    identity_provider VARCHAR(30),
    phone VARCHAR(30),
    role VARCHAR(255),
    status VARCHAR(255) DEFAULT 'ACTIVE',
    created_at TIMESTAMP,
    branch_id BIGINT REFERENCES branches(branch_id)
);

CREATE TABLE IF NOT EXISTS customer_profiles (
    customer_profile_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    date_of_birth VARCHAR(30),
    gender VARCHAR(30),
    nationality VARCHAR(100),
    passport_number VARCHAR(50),
    visa_number VARCHAR(50),
    identity_number VARCHAR(30),
    identity_issue_date VARCHAR(30),
    identity_issue_place VARCHAR(255),
    permanent_address VARCHAR(500),
    contact_address VARCHAR(500),
    occupation VARCHAR(255),
    employment_status VARCHAR(100),
    employer_name VARCHAR(255),
    work_phone VARCHAR(30),
    job_title VARCHAR(100),
    monthly_income VARCHAR(50),
    salary_payment_method VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE services ADD COLUMN IF NOT EXISTS form_schema TEXT NOT NULL DEFAULT '[]';
ALTER TABLE services ADD COLUMN IF NOT EXISTS required_customer_fields VARCHAR(1000);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS customer_id BIGINT REFERENCES users(user_id);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS customer_email VARCHAR(255);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_tickets_customer_status_created
    ON tickets(customer_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS transaction_drafts (
    draft_id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL UNIQUE REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    service_id BIGINT,
    service_name VARCHAR(255) NOT NULL,
    schema_snapshot TEXT NOT NULL,
    profile_snapshot TEXT NOT NULL DEFAULT '{}',
    values_payload TEXT NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
