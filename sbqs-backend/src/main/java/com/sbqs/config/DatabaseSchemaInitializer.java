package com.sbqs.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    /**
     * Bổ sung schema theo cách idempotent cho môi trường đồ án đang tắt Flyway.
     * Production nên chuyển các câu lệnh này thành migration có version để audit dễ hơn.
     */
    public void initialize() {
        createCoreTablesIfMissing();

        executeIfPossible("alter table branches add column if not exists province varchar(255)");
        executeIfPossible("alter table branches add column if not exists district varchar(255)");
        executeIfPossible("alter table branches add column if not exists ward varchar(255)");
        executeIfPossible("alter table services add column if not exists branch_id bigint");
        executeIfPossible("alter table services add column if not exists service_type varchar(255) not null default 'BASIC'");
        executeIfPossible("alter table services add column if not exists status varchar(255) not null default 'ACTIVE'");
        executeIfPossible("alter table tickets add column if not exists customer_email varchar(255)");
        executeIfPossible("alter table tickets add column if not exists branch_id bigint");
        executeIfPossible("alter table tickets add column if not exists service_id bigint");
        executeIfPossible("alter table tickets add column if not exists queue_machine_id bigint");
        executeIfPossible("alter table tickets add column if not exists ticket_number integer");
        executeIfPossible("alter table tickets add column if not exists serving_started_at timestamp");
        executeIfPossible("alter table tickets add column if not exists created_at timestamp not null default current_timestamp");
        executeIfPossible("alter table tickets alter column appointment_id drop not null");
        executeIfPossible("alter table tickets alter column service_id drop not null");
        executeIfPossible("alter table services add column if not exists required_customer_fields varchar(1000)");
        executeIfPossible("alter table users add column if not exists date_of_birth varchar(30)");
        executeIfPossible("alter table users add column if not exists gender varchar(30)");
        executeIfPossible("alter table users add column if not exists nationality varchar(100)");
        executeIfPossible("alter table users add column if not exists passport_number varchar(50)");
        executeIfPossible("alter table users add column if not exists visa_number varchar(50)");
        executeIfPossible("alter table users add column if not exists identity_number varchar(30)");
        executeIfPossible("alter table users add column if not exists identity_issue_date varchar(30)");
        executeIfPossible("alter table users add column if not exists identity_issue_place varchar(255)");
        executeIfPossible("alter table users add column if not exists permanent_address varchar(500)");
        executeIfPossible("alter table users add column if not exists contact_address varchar(500)");
        executeIfPossible("alter table users add column if not exists occupation varchar(255)");
        executeIfPossible("alter table users add column if not exists employment_status varchar(100)");
        executeIfPossible("alter table users add column if not exists employer_name varchar(255)");
        executeIfPossible("alter table users add column if not exists work_phone varchar(30)");
        executeIfPossible("alter table users add column if not exists job_title varchar(100)");
        executeIfPossible("alter table users add column if not exists monthly_income varchar(50)");
        executeIfPossible("alter table users add column if not exists salary_payment_method varchar(255)");
        executeIfPossible("alter table users add column if not exists account_number varchar(50)");
        executeIfPossible("alter table users add column if not exists card_delivery_address varchar(500)");
        executeIfPossible("alter table queue_machines add column if not exists last_ticket_number integer not null default 0");
        executeIfPossible("alter table counters add column if not exists queue_machine_id bigint");
        executeIfPossible("alter table counters add column if not exists current_ticket_id bigint");
        executeIfPossible("alter table appointments add column if not exists customer_name varchar(255)");
        executeIfPossible("alter table appointments add column if not exists customer_phone varchar(30)");
        executeIfPossible("alter table users drop constraint if exists users_status_check");
        executeIfPossible("""
                update queue_machines qm
                set last_ticket_number = greatest(
                    qm.last_ticket_number,
                    coalesce((
                        select max(t.ticket_number)
                        from tickets t
                        where t.queue_machine_id = qm.queue_machine_id
                    ), 0)
                )
                """);
        executeIfPossible("alter table tickets drop constraint if exists tickets_ticket_number_key");
        executeIfPossible("alter table tickets drop constraint if exists uk_tickets_ticket_number");
        executeIfPossible("alter table tickets drop constraint if exists unique_ticket_per_machine");
        executeIfPossible("""
                do $$
                declare
                    constraint_name text;
                begin
                    for constraint_name in
                        select c.conname
                        from pg_constraint c
                        join pg_class t on t.oid = c.conrelid
                        join pg_namespace n on n.oid = t.relnamespace
                        where t.relname = 'tickets'
                        and n.nspname = current_schema()
                        and c.contype = 'u'
                        and (
                            select array_agg(a.attname order by a.attnum)
                            from unnest(c.conkey) key(attnum)
                            join pg_attribute a on a.attrelid = t.oid and a.attnum = key.attnum
                        ) = array['ticket_number']
                    loop
                        execute format('alter table tickets drop constraint %I', constraint_name);
                    end loop;
                end $$;
                """);

        jdbcTemplate.execute("""
                create table if not exists service_histories (
                    history_id bigserial primary key,
                    ticket_id bigint,
                    branch_id bigint not null,
                    queue_machine_id bigint,
                    counter_id bigint,
                    service_id bigint,
                    staff_id bigint,
                    customer_email varchar(255),
                    branch_name varchar(255),
                    queue_machine_name varchar(255),
                    counter_name varchar(255),
                    service_name varchar(255),
                    staff_name varchar(255),
                    ticket_number integer not null,
                    started_at timestamp,
                    completed_at timestamp,
                    staff_note varchar(255),
                    status varchar(255),
                    created_at timestamp
                )
                """);
        jdbcTemplate.execute("alter table service_histories add column if not exists staff_id bigint");
        jdbcTemplate.execute("alter table service_histories add column if not exists status varchar(255)");
        jdbcTemplate.execute("alter table service_histories add column if not exists customer_email varchar(255)");
        jdbcTemplate.execute("alter table service_histories add column if not exists branch_name varchar(255)");
        jdbcTemplate.execute("alter table service_histories add column if not exists queue_machine_name varchar(255)");
        jdbcTemplate.execute("alter table service_histories add column if not exists counter_name varchar(255)");
        jdbcTemplate.execute("alter table service_histories add column if not exists service_name varchar(255)");
        jdbcTemplate.execute("alter table service_histories add column if not exists staff_name varchar(255)");

        executeIfPossible("""
                update service_histories h
                set customer_email = coalesce(h.customer_email, t.customer_email)
                from tickets t
                where h.ticket_id = t.ticket_id
                """);
        executeIfPossible("""
                update service_histories h
                set branch_name = coalesce(h.branch_name, b.branch_name)
                from branches b
                where h.branch_id = b.branch_id
                """);
        executeIfPossible("""
                update service_histories h
                set queue_machine_name = coalesce(h.queue_machine_name, qm.machine_name)
                from queue_machines qm
                where h.queue_machine_id = qm.queue_machine_id
                """);
        executeIfPossible("""
                update service_histories h
                set counter_name = coalesce(h.counter_name, c.counter_name)
                from counters c
                where h.counter_id = c.counter_id
                """);
        executeIfPossible("""
                update service_histories h
                set service_name = coalesce(h.service_name, s.service_name)
                from services s
                where h.service_id = s.service_id
                """);
        executeIfPossible("""
                update service_histories h
                set staff_name = coalesce(h.staff_name, u.full_name)
                from users u
                where h.staff_id = u.user_id
                """);
        executeIfPossible("""
                do $$
                declare
                    constraint_name text;
                begin
                    for constraint_name in
                        select c.conname
                        from pg_constraint c
                        join pg_class t on t.oid = c.conrelid
                        join pg_namespace n on n.oid = t.relnamespace
                        where t.relname = 'service_histories'
                        and n.nspname = current_schema()
                        and c.contype = 'f'
                    loop
                        execute format('alter table service_histories drop constraint %I', constraint_name);
                    end loop;
                end $$;
                """);

        executeIfPossible("""
                create table if not exists password_reset_tokens (
                    password_reset_token_id bigserial primary key,
                    user_id bigint not null references users(user_id) on delete cascade,
                    token_hash varchar(64) not null unique,
                    expires_at timestamp not null,
                    used_at timestamp,
                    created_at timestamp not null
                )
                """);

        executeIfPossible("""
                create table if not exists email_verification_tokens (
                    email_verification_token_id bigserial primary key,
                    user_id bigint not null references users(user_id) on delete cascade,
                    token_hash varchar(64) not null unique,
                    expires_at timestamp not null,
                    used_at timestamp,
                    created_at timestamp not null
                )
                """);

        jdbcTemplate.execute("""
                create table if not exists authentication_audits (
                    authentication_audit_id bigserial primary key,
                    user_id bigint,
                    email varchar(255) not null,
                    successful boolean not null,
                    authentication_source varchar(50),
                    failure_reason varchar(255),
                    ip_address varchar(255),
                    user_agent varchar(512),
                    created_at timestamp not null
                )
                """);
        jdbcTemplate.execute("create index if not exists idx_auth_audits_email_created_at on authentication_audits(email, created_at desc)");
        jdbcTemplate.execute("create index if not exists idx_auth_audits_ip_created_at on authentication_audits(ip_address, created_at desc)");

        executeIfPossible("""
                create table if not exists account_change_tokens (
                    account_change_token_id bigserial primary key,
                    user_id bigint not null references users(user_id) on delete cascade,
                    pending_full_name varchar(150) not null,
                    pending_email varchar(255) not null,
                    pending_phone varchar(30) not null,
                    current_email_token_hash varchar(64) not null unique,
                    new_email_token_hash varchar(64) unique,
                    expires_at timestamp not null,
                    current_email_confirmed_at timestamp,
                    new_email_confirmed_at timestamp,
                    applied_at timestamp,
                    created_at timestamp not null
                )
                """);
        jdbcTemplate.execute("create index if not exists idx_account_change_user_created_at on account_change_tokens(user_id, created_at desc)");

        jdbcTemplate.execute("""
                create table if not exists counter_sessions (
                    counter_session_id bigserial primary key,
                    counter_id bigint not null,
                    staff_id bigint not null,
                    branch_id bigint not null,
                    counter_name varchar(255),
                    staff_name varchar(255),
                    staff_email varchar(255),
                    branch_name varchar(255),
                    started_at timestamp not null,
                    ended_at timestamp,
                    status varchar(255) not null
                )
                """);
        jdbcTemplate.execute("alter table counter_sessions add column if not exists counter_name varchar(255)");
        jdbcTemplate.execute("alter table counter_sessions add column if not exists staff_name varchar(255)");
        jdbcTemplate.execute("alter table counter_sessions add column if not exists staff_email varchar(255)");
        jdbcTemplate.execute("alter table counter_sessions add column if not exists branch_name varchar(255)");
        executeIfPossible("""
                update counter_sessions cs
                set counter_name = coalesce(cs.counter_name, c.counter_name)
                from counters c
                where cs.counter_id = c.counter_id
                """);
        executeIfPossible("""
                update counter_sessions cs
                set staff_name = coalesce(cs.staff_name, u.full_name),
                    staff_email = coalesce(cs.staff_email, u.email)
                from users u
                where cs.staff_id = u.user_id
                """);
        executeIfPossible("""
                update counter_sessions cs
                set branch_name = coalesce(cs.branch_name, b.branch_name)
                from branches b
                where cs.branch_id = b.branch_id
                """);
        executeIfPossible("""
                do $$
                declare
                    constraint_name text;
                begin
                    for constraint_name in
                        select c.conname
                        from pg_constraint c
                        join pg_class t on t.oid = c.conrelid
                        join pg_namespace n on n.oid = t.relnamespace
                        where t.relname = 'counter_sessions'
                        and n.nspname = current_schema()
                        and c.contype = 'f'
                    loop
                        execute format('alter table counter_sessions drop constraint %I', constraint_name);
                    end loop;
                end $$;
                """);

        executeIfPossible("""
                update counters c
                set status = 'INACTIVE'
                where not exists (
                    select 1
                    from counter_sessions cs
                    where cs.counter_id = c.counter_id
                    and cs.status = 'ACTIVE'
                )
                """);

        executeIfPossible("alter table services drop constraint if exists services_service_code_key");
        executeIfPossible("alter table queue_machines drop constraint if exists queue_machines_machine_code_key");
        executeIfPossible("alter table counters drop constraint if exists counters_counter_code_key");

        executeIfPossible("""
                do $$
                begin
                    if not exists (select 1 from pg_constraint where conname = 'uk_services_branch_code') then
                        alter table services add constraint uk_services_branch_code unique (branch_id, service_code);
                    end if;
                end $$;
                """);

        executeIfPossible("""
                do $$
                begin
                    if not exists (select 1 from pg_constraint where conname = 'uk_queue_machines_branch_code') then
                        alter table queue_machines add constraint uk_queue_machines_branch_code unique (branch_id, machine_code);
                    end if;
                end $$;
                """);

        executeIfPossible("""
                do $$
                begin
                    if not exists (select 1 from pg_constraint where conname = 'uk_counters_branch_code') then
                        alter table counters add constraint uk_counters_branch_code unique (branch_id, counter_code);
                    end if;
                end $$;
                """);
    }

    /** Chạy migration tương thích dữ liệu legacy; lỗi do dữ liệu cũ không làm backend dừng khởi động. */
    private void createCoreTablesIfMissing() {
        jdbcTemplate.execute("""
                create table if not exists branches (
                    branch_id bigserial primary key,
                    bank_name varchar(255) not null,
                    branch_code varchar(255) not null unique,
                    branch_name varchar(255) not null,
                    province varchar(255),
                    district varchar(255),
                    ward varchar(255),
                    address varchar(500),
                    phone varchar(30),
                    status varchar(255) not null default 'ACTIVE',
                    created_at timestamp not null default current_timestamp,
                    latitude double precision not null default 0,
                    longitude double precision not null default 0
                )
                """);

        jdbcTemplate.execute("""
                create table if not exists users (
                    user_id bigserial primary key,
                    full_name varchar(255),
                    email varchar(255) unique,
                    password_hash varchar(255),
                    keycloak_user_id varchar(255),
                    phone varchar(30),
                    role varchar(255),
                    status varchar(255) default 'ACTIVE',
                    created_at timestamp,
                    branch_id bigint references branches(branch_id)
                )
                """);

        jdbcTemplate.execute("""
                create table if not exists queue_machines (
                    queue_machine_id bigserial primary key,
                    branch_id bigint not null references branches(branch_id),
                    machine_code varchar(255) not null,
                    machine_name varchar(255) not null,
                    location_note varchar(500),
                    instruction_note varchar(500),
                    status varchar(255) not null default 'ACTIVE',
                    last_ticket_number integer not null default 0,
                    created_at timestamp not null default current_timestamp
                )
                """);

        jdbcTemplate.execute("""
                create table if not exists services (
                    service_id bigserial primary key,
                    branch_id bigint references branches(branch_id),
                    service_code varchar(255) not null,
                    service_name varchar(255) not null,
                    service_type varchar(255) not null default 'BASIC',
                    description text,
                    estimated_time integer not null default 15,
                    status varchar(255) not null default 'ACTIVE',
                    required_customer_fields varchar(1000)
                )
                """);

        jdbcTemplate.execute("""
                create table if not exists counters (
                    counter_id bigserial primary key,
                    branch_id bigint not null references branches(branch_id),
                    queue_machine_id bigint references queue_machines(queue_machine_id),
                    current_ticket_id bigint,
                    counter_code varchar(255) not null,
                    counter_name varchar(255) not null,
                    status varchar(255) not null default 'INACTIVE'
                )
                """);

        jdbcTemplate.execute("""
                create table if not exists appointments (
                    appointment_id bigserial primary key,
                    customer_name varchar(255),
                    customer_phone varchar(30),
                    branch_id bigint not null references branches(branch_id),
                    service_id bigint not null references services(service_id),
                    appointment_date date not null,
                    appointment_time time not null,
                    status varchar(255) not null default 'PENDING',
                    created_at timestamp not null default current_timestamp
                )
                """);

        jdbcTemplate.execute("""
                create table if not exists tickets (
                    ticket_id bigserial primary key,
                    queue_machine_id bigint references queue_machines(queue_machine_id),
                    appointment_id bigint references appointments(appointment_id),
                    branch_id bigint not null references branches(branch_id),
                    service_id bigint references services(service_id),
                    counter_id bigint references counters(counter_id),
                    ticket_number integer not null,
                    customer_email varchar(255),
                    status varchar(255) not null default 'WAITING',
                    serving_started_at timestamp,
                    created_at timestamp not null default current_timestamp
                )
                """);

        jdbcTemplate.execute("""
                create table if not exists queue_machine_services (
                    queue_machine_id bigint not null references queue_machines(queue_machine_id),
                    service_id bigint not null references services(service_id),
                    primary key (queue_machine_id, service_id)
                )
                """);
    }

    private void executeIfPossible(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Existing legacy data may contain duplicates. Service validation still protects new writes.
        }
    }
}
