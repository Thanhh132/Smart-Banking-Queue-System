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
