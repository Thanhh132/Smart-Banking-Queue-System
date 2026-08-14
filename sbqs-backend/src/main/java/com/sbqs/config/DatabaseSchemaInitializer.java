package com.sbqs.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
public class DatabaseSchemaInitializer {
    private static final String SCHEMA_VERSION = "database-schema-v10-delegation-snapshots";
    private static final String PROFILE_REFACTOR_VERSION = "database-schema-v11-customer-profile-ticket-owner";
    private static final String LEGACY_USER_CLEANUP_VERSION = "database-schema-v12-legacy-user-cleanup";
    private static final String TICKET_CUSTOMER_REFACTOR_VERSION = "database-schema-v13-ticket-customer-relations";
    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaInitializer.class);

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
        jdbcTemplate.execute("""
                create table if not exists system_settings (
                    setting_key varchar(100) primary key,
                    setting_value varchar(500) not null
                )
                """);
        createCoreTablesIfMissing();
        applyCustomerProfileMigration();
        applyTicketCustomerRefactorMigration();
        ensureTicketIdempotency();
        ensureTicketOperationalSchema();
        ensureSingleActiveTicketPerCustomer();
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from system_settings where setting_key = ?",
                Integer.class,
                SCHEMA_VERSION);
        if (applied != null && applied > 0) {
            applyLegacyUserCleanup();
            return;
        }
        jdbcTemplate.execute("""
                create table if not exists service_catalog (
                    catalog_id bigserial primary key,
                    service_code varchar(255) not null unique,
                    service_name varchar(255) not null unique,
                    service_type varchar(255) not null default 'BASIC',
                    description text,
                    estimated_time integer not null default 15,
                    status varchar(255) not null default 'ACTIVE',
                    delegatable boolean not null default false,
                    form_schema text not null default '[]'
                )
                """);
        executeIfPossible("alter table service_catalog add column if not exists delegatable boolean not null default false");
        executeIfPossible("alter table services add column if not exists catalog_id bigint references service_catalog(catalog_id)");

        jdbcTemplate.execute("""
                create table if not exists branch_operating_hours (
                    operating_hours_id bigserial primary key,
                    branch_id bigint not null references branches(branch_id) on delete cascade,
                    day_of_week integer not null,
                    closed boolean not null default false,
                    morning_open time,
                    morning_close time,
                    afternoon_open time,
                    afternoon_close time,
                    constraint uk_branch_hours_day unique(branch_id, day_of_week),
                    constraint ck_branch_hours_day check(day_of_week between 1 and 7)
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists digital_delegations (
                    delegation_id bigserial primary key,
                    reference_code varchar(20) not null unique,
                    owner_id bigint not null references users(user_id),
                    branch_id bigint references branches(branch_id),
                    service_id bigint references services(service_id),
                    branch_name_snapshot varchar(255),
                    service_name_snapshot varchar(255),
                    delegate_name varchar(150) not null,
                    delegate_identity_hash varchar(100) not null,
                    delegate_identity_last4 varchar(4) not null,
                    delegate_date_of_birth date,
                    delegate_phone varchar(15),
                    identity_issue_date date,
                    identity_expiry_date date,
                    identity_issue_place varchar(150),
                    relationship varchar(100) not null,
                    transaction_scope varchar(500) not null,
                    valid_from timestamp not null,
                    valid_until timestamp not null,
                    status varchar(30) not null,
                    created_at timestamp not null,
                    verified_at timestamp,
                    used_at timestamp,
                    verified_by bigint references users(user_id) on delete set null
                )
                """);
        jdbcTemplate.execute("create index if not exists idx_delegations_owner_created on digital_delegations(owner_id, created_at desc)");
        executeIfPossible("alter table digital_delegations add column if not exists delegate_date_of_birth date");
        executeIfPossible("alter table digital_delegations add column if not exists delegate_phone varchar(15)");
        executeIfPossible("alter table digital_delegations add column if not exists identity_issue_date date");
        executeIfPossible("alter table digital_delegations add column if not exists identity_expiry_date date");
        executeIfPossible("alter table digital_delegations add column if not exists identity_issue_place varchar(150)");
        jdbcTemplate.execute("alter table digital_delegations add column if not exists branch_name_snapshot varchar(255)");
        jdbcTemplate.execute("alter table digital_delegations add column if not exists service_name_snapshot varchar(255)");
        jdbcTemplate.execute("update digital_delegations d set branch_name_snapshot = b.branch_name from branches b where d.branch_id = b.branch_id and d.branch_name_snapshot is null");
        jdbcTemplate.execute("update digital_delegations d set service_name_snapshot = s.service_name from services s where d.service_id = s.service_id and d.service_name_snapshot is null");
        jdbcTemplate.execute("alter table digital_delegations alter column branch_id drop not null");
        jdbcTemplate.execute("alter table digital_delegations alter column service_id drop not null");
        executeIfPossible("""
                do $$
                declare
                    constraint_name text;
                begin
                    select c.conname into constraint_name
                    from pg_constraint c
                    join pg_class t on t.oid = c.conrelid
                    join pg_attribute a on a.attrelid = t.oid and a.attnum = any(c.conkey)
                    where t.relname = 'digital_delegations'
                      and c.contype = 'f'
                      and a.attname = 'verified_by'
                    limit 1;

                    if constraint_name is not null then
                        execute format(
                            'alter table digital_delegations drop constraint %I',
                            constraint_name);
                    end if;

                    alter table digital_delegations
                        add constraint fk_delegations_verified_by
                        foreign key (verified_by) references users(user_id) on delete set null;
                end $$;
                """);

        jdbcTemplate.execute("""
                create table if not exists web_push_subscriptions (
                    subscription_id bigserial primary key,
                    user_id bigint not null references users(user_id) on delete cascade,
                    endpoint text not null,
                    endpoint_hash varchar(64) not null unique,
                    p256dh varchar(255) not null,
                    auth_secret varchar(255) not null,
                    user_agent varchar(500),
                    active boolean not null default true,
                    failure_count integer not null default 0,
                    created_at timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    last_success_at timestamp
                )
                """);
        jdbcTemplate.execute("create index if not exists idx_web_push_user_active on web_push_subscriptions(user_id, active)");
        jdbcTemplate.execute("""
                create table if not exists web_push_deliveries (
                    delivery_id bigserial primary key,
                    ticket_id bigint not null references tickets(ticket_id) on delete cascade,
                    subscription_id bigint not null references web_push_subscriptions(subscription_id) on delete cascade,
                    notification_type varchar(30) not null,
                    status varchar(20) not null,
                    created_at timestamp not null default current_timestamp,
                    sent_at timestamp,
                    constraint uk_web_push_delivery unique(ticket_id, subscription_id, notification_type)
                )
                """);
        jdbcTemplate.execute("create index if not exists idx_web_push_delivery_ticket on web_push_deliveries(ticket_id, notification_type)");

        executeIfPossible("drop table if exists service_form_revisions");
        executeIfPossible("alter table services drop column if exists form_version");

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
        executeIfPossible("alter table tickets alter column ticket_no drop not null");
        executeIfPossible("alter table tickets add column if not exists serving_started_at timestamp");
        executeIfPossible("alter table tickets add column if not exists created_at timestamp not null default current_timestamp");
        executeIfPossible("alter table tickets alter column appointment_id drop not null");
        executeIfPossible("alter table tickets alter column service_id drop not null");
        executeIfPossible("alter table services add column if not exists required_customer_fields varchar(1000)");
        executeIfPossible("alter table services add column if not exists form_schema text not null default '[]'");
        executeIfPossible("alter table appointments alter column service_id drop not null");
        executeIfPossible("alter table users add column if not exists identity_provider varchar(30)");
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
                    customer_id bigint references users(user_id),
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

        // Các chỉ mục phục vụ trực tiếp màn hình hàng đợi và lịch sử thường xuyên được polling.
        jdbcTemplate.execute("create index if not exists idx_tickets_branch_status on tickets(branch_id, status)");
        jdbcTemplate.execute("create index if not exists idx_tickets_machine_status on tickets(queue_machine_id, status)");
        jdbcTemplate.execute("create index if not exists idx_tickets_customer_status_created on tickets(customer_id, status, created_at desc)");
        jdbcTemplate.execute("create index if not exists idx_histories_branch_completed on service_histories(branch_id, completed_at desc)");
        jdbcTemplate.execute("create index if not exists idx_histories_staff_completed on service_histories(staff_id, completed_at desc)");
        jdbcTemplate.execute("create index if not exists idx_histories_customer_completed on service_histories(customer_id, completed_at desc)");

        jdbcTemplate.execute("""
                create table if not exists transaction_drafts (
                    draft_id bigserial primary key,
                    ticket_id bigint not null unique references tickets(ticket_id) on delete cascade,
                    service_id bigint,
                    service_name varchar(255) not null,
                    schema_snapshot text not null,
                    values_payload text not null,
                    created_by varchar(255) not null,
                    created_at timestamp not null default current_timestamp
                )
                """);
        jdbcTemplate.execute("create index if not exists idx_transaction_drafts_ticket on transaction_drafts(ticket_id)");

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

        jdbcTemplate.update(
                "insert into system_settings(setting_key, setting_value) values (?, 'completed') on conflict (setting_key) do nothing",
                SCHEMA_VERSION);
        applyLegacyUserCleanup();
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
                    last_ticket_date date,
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
                    business_date date not null default current_date,
                    idempotency_key varchar(36),
                    customer_email varchar(255),
                    status varchar(255) not null default 'WAITING',
                    serving_started_at timestamp,
                    cancelled_at timestamp,
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

    /** Non-destructive expand/backfill migration for the customer-profile split. */
    private void applyCustomerProfileMigration() {
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from system_settings where setting_key = ?",
                Integer.class,
                PROFILE_REFACTOR_VERSION);
        if (applied != null && applied > 0) return;

        for (String definition : new String[] {
                "date_of_birth varchar(30)", "gender varchar(30)", "nationality varchar(100)",
                "passport_number varchar(50)", "visa_number varchar(50)", "identity_number varchar(30)",
                "identity_issue_date varchar(30)", "identity_issue_place varchar(255)",
                "permanent_address varchar(500)", "contact_address varchar(500)", "occupation varchar(255)",
                "employment_status varchar(100)", "employer_name varchar(255)", "work_phone varchar(30)",
                "job_title varchar(100)", "monthly_income varchar(50)", "salary_payment_method varchar(255)",
                "identity_provider varchar(30)" }) {
            jdbcTemplate.execute("alter table users add column if not exists " + definition);
        }

        jdbcTemplate.execute("""
                create table if not exists customer_profiles (
                    customer_profile_id bigserial primary key,
                    user_id bigint not null unique references users(user_id) on delete cascade,
                    date_of_birth varchar(30),
                    gender varchar(30),
                    nationality varchar(100),
                    passport_number varchar(50),
                    visa_number varchar(50),
                    identity_number varchar(30),
                    identity_issue_date varchar(30),
                    identity_issue_place varchar(255),
                    permanent_address varchar(500),
                    contact_address varchar(500),
                    occupation varchar(255),
                    employment_status varchar(100),
                    employer_name varchar(255),
                    work_phone varchar(30),
                    job_title varchar(100),
                    monthly_income varchar(50),
                    salary_payment_method varchar(255),
                    created_at timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp
                )
                """);

        jdbcTemplate.update("""
                insert into customer_profiles (
                    user_id, date_of_birth, gender, nationality, passport_number, visa_number,
                    identity_number, identity_issue_date, identity_issue_place,
                    permanent_address, contact_address, occupation, employment_status,
                    employer_name, work_phone, job_title, monthly_income, salary_payment_method)
                select user_id, date_of_birth, gender, nationality, passport_number, visa_number,
                    identity_number, identity_issue_date, identity_issue_place,
                    permanent_address, contact_address, occupation, employment_status,
                    employer_name, work_phone, job_title, monthly_income, salary_payment_method
                from users where upper(role) = 'CUSTOMER'
                on conflict (user_id) do update set
                    date_of_birth = coalesce(customer_profiles.date_of_birth, excluded.date_of_birth),
                    gender = coalesce(customer_profiles.gender, excluded.gender),
                    nationality = coalesce(customer_profiles.nationality, excluded.nationality),
                    passport_number = coalesce(customer_profiles.passport_number, excluded.passport_number),
                    visa_number = coalesce(customer_profiles.visa_number, excluded.visa_number),
                    identity_number = coalesce(customer_profiles.identity_number, excluded.identity_number),
                    identity_issue_date = coalesce(customer_profiles.identity_issue_date, excluded.identity_issue_date),
                    identity_issue_place = coalesce(customer_profiles.identity_issue_place, excluded.identity_issue_place),
                    permanent_address = coalesce(customer_profiles.permanent_address, excluded.permanent_address),
                    contact_address = coalesce(customer_profiles.contact_address, excluded.contact_address),
                    occupation = coalesce(customer_profiles.occupation, excluded.occupation),
                    employment_status = coalesce(customer_profiles.employment_status, excluded.employment_status),
                    employer_name = coalesce(customer_profiles.employer_name, excluded.employer_name),
                    work_phone = coalesce(customer_profiles.work_phone, excluded.work_phone),
                    job_title = coalesce(customer_profiles.job_title, excluded.job_title),
                    monthly_income = coalesce(customer_profiles.monthly_income, excluded.monthly_income),
                    salary_payment_method = coalesce(customer_profiles.salary_payment_method, excluded.salary_payment_method)
                """);

        jdbcTemplate.execute("alter table tickets add column if not exists customer_id bigint");
        executeIfPossible("""
                do $$ begin
                    if not exists (select 1 from pg_constraint where conname = 'fk_tickets_customer') then
                        alter table tickets add constraint fk_tickets_customer
                            foreign key (customer_id) references users(user_id);
                    end if;
                end $$
                """);
        jdbcTemplate.update("""
                update tickets t set customer_id = matched.user_id
                from (
                    select lower(email) normalized_email, min(user_id) user_id
                    from users where email is not null
                    group by lower(email) having count(*) = 1
                ) matched
                where t.customer_id is null
                  and t.customer_email is not null
                  and lower(t.customer_email) = matched.normalized_email
                """);
        jdbcTemplate.execute("create index if not exists idx_tickets_customer_status_created on tickets(customer_id, status, created_at desc)");

        jdbcTemplate.execute("""
                create table if not exists transaction_drafts (
                    draft_id bigserial primary key,
                    ticket_id bigint not null unique references tickets(ticket_id) on delete cascade,
                    service_id bigint,
                    service_name varchar(255) not null,
                    schema_snapshot text not null,
                    values_payload text not null,
                    profile_snapshot text not null default '{}',
                    created_by varchar(255) not null,
                    created_at timestamp not null default current_timestamp
                )
                """);
        jdbcTemplate.execute("alter table transaction_drafts add column if not exists profile_snapshot text not null default '{}'");

        Long totalTickets = jdbcTemplate.queryForObject("select count(*) from tickets", Long.class);
        Long matchedTickets = jdbcTemplate.queryForObject("select count(*) from tickets where customer_id is not null", Long.class);
        long total = totalTickets == null ? 0 : totalTickets;
        long matched = matchedTickets == null ? 0 : matchedTickets;
        log.info("Customer ticket backfill completed: total={}, matched={}, unmatched={}", total, matched, total - matched);

        jdbcTemplate.update(
                "insert into system_settings(setting_key, setting_value) values (?, 'completed') on conflict (setting_key) do nothing",
                PROFILE_REFACTOR_VERSION);
    }

    /** Contract migration: users retains account/authentication data only. */
    private void applyLegacyUserCleanup() {
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from system_settings where setting_key = ?",
                Integer.class,
                LEGACY_USER_CLEANUP_VERSION);
        if (applied != null && applied > 0) return;

        Integer profileMigrationCompleted = jdbcTemplate.queryForObject(
                "select count(*) from system_settings where setting_key = ? and setting_value = 'completed'",
                Integer.class,
                PROFILE_REFACTOR_VERSION);
        if (profileMigrationCompleted == null || profileMigrationCompleted == 0) {
            throw new IllegalStateException("Cannot clean legacy user columns before customer-profile migration completes");
        }

        Long missingProfiles = jdbcTemplate.queryForObject("""
                select count(*)
                from users u
                where upper(u.role) = 'CUSTOMER'
                  and not exists (
                      select 1 from customer_profiles cp where cp.user_id = u.user_id)
                """, Long.class);
        if (missingProfiles != null && missingProfiles > 0) {
            throw new IllegalStateException(
                    "Cannot clean legacy user columns: " + missingProfiles + " customer profile(s) were not backfilled");
        }

        for (String column : new String[] {
                "date_of_birth", "gender", "nationality", "passport_number", "visa_number",
                "identity_number", "identity_issue_date", "identity_issue_place",
                "permanent_address", "contact_address", "occupation", "employment_status",
                "employer_name", "work_phone", "job_title", "monthly_income",
                "salary_payment_method", "account_number", "card_delivery_address" }) {
            jdbcTemplate.execute("alter table users drop column if exists " + column);
        }

        jdbcTemplate.update(
                "insert into system_settings(setting_key, setting_value) values (?, 'completed') on conflict (setting_key) do nothing",
                LEGACY_USER_CLEANUP_VERSION);
        log.info("Legacy user cleanup completed: 19 profile/transaction columns removed; tickets.customer_email retained");
    }

    /** Moves customer relations and indexes to user_id while retaining email snapshot columns. */
    private void applyTicketCustomerRefactorMigration() {
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from system_settings where setting_key = ?",
                Integer.class,
                TICKET_CUSTOMER_REFACTOR_VERSION);
        if (applied != null && applied > 0) return;

        jdbcTemplate.execute("alter table service_histories add column if not exists customer_id bigint");
        executeIfPossible("""
                do $$ begin
                    if not exists (select 1 from pg_constraint where conname = 'fk_histories_customer') then
                        alter table service_histories add constraint fk_histories_customer
                            foreign key (customer_id) references users(user_id);
                    end if;
                end $$
                """);
        jdbcTemplate.update("""
                update service_histories h
                set customer_id = t.customer_id
                from tickets t
                where h.ticket_id = t.ticket_id
                  and h.customer_id is null
                  and t.customer_id is not null
                """);

        // v10 used this name for an email index, so replace it with the customer relation index.
        jdbcTemplate.execute("drop index if exists idx_tickets_customer_status_created");
        jdbcTemplate.execute("create index idx_tickets_customer_status_created on tickets(customer_id, status, created_at desc)");
        jdbcTemplate.execute("drop index if exists idx_histories_customer_completed");
        jdbcTemplate.execute("create index idx_histories_customer_completed on service_histories(customer_id, completed_at desc)");

        jdbcTemplate.update(
                "insert into system_settings(setting_key, setting_value) values (?, 'completed') on conflict (setting_key) do nothing",
                TICKET_CUSTOMER_REFACTOR_VERSION);
    }

    /**
     * Database-level invariant for ticket issuing. The service-side lookup gives a
     * friendly early rejection, while this partial unique index closes the race
     * where concurrent requests from the same customer both pass that lookup.
     */
    void ensureSingleActiveTicketPerCustomer() {
        List<Long> duplicateCustomerIds = jdbcTemplate.queryForList("""
                select customer_id
                from tickets
                where customer_id is not null
                  and status in ('WAITING', 'SERVING')
                group by customer_id
                having count(*) > 1
                order by customer_id
                limit 10
                """, Long.class);

        if (!duplicateCustomerIds.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot enforce one active ticket per customer. Resolve duplicate active tickets for customer IDs: "
                            + duplicateCustomerIds);
        }

        jdbcTemplate.execute("""
                create unique index if not exists ux_tickets_one_active_customer
                on tickets(customer_id)
                where customer_id is not null
                  and status in ('WAITING', 'SERVING')
                """);
    }

    /** Adds durable request deduplication without changing historical tickets. */
    void ensureTicketIdempotency() {
        jdbcTemplate.execute("alter table tickets add column if not exists idempotency_key varchar(36)");
        jdbcTemplate.execute("""
                create unique index if not exists ux_tickets_customer_idempotency
                on tickets(customer_id, idempotency_key)
                where customer_id is not null
                  and idempotency_key is not null
                """);
    }

    /** Daily numbering, cooldown timestamps and hot-path indexes for ticket issuing. */
    void ensureTicketOperationalSchema() {
        jdbcTemplate.execute("alter table tickets add column if not exists business_date date");
        jdbcTemplate.execute("update tickets set business_date = created_at::date where business_date is null");
        jdbcTemplate.execute("alter table tickets alter column business_date set not null");
        jdbcTemplate.execute("alter table tickets add column if not exists cancelled_at timestamp");
        jdbcTemplate.execute("alter table queue_machines add column if not exists last_ticket_date date");
        jdbcTemplate.execute("""
                update queue_machines qm
                set last_ticket_date = latest.business_date,
                    last_ticket_number = latest.last_number
                from (
                    select distinct on (queue_machine_id)
                           queue_machine_id,
                           business_date,
                           max(ticket_number) over (partition by queue_machine_id, business_date) as last_number
                    from tickets
                    where queue_machine_id is not null
                    order by queue_machine_id, business_date desc
                ) latest
                where qm.queue_machine_id = latest.queue_machine_id
                """);
        jdbcTemplate.execute("""
                create unique index if not exists ux_tickets_machine_day_number
                on tickets(queue_machine_id, business_date, ticket_number)
                where queue_machine_id is not null
                """);
        jdbcTemplate.execute("create index if not exists idx_tickets_customer_status on tickets(customer_id, status)");
        jdbcTemplate.execute("create index if not exists idx_tickets_customer_created on tickets(customer_id, created_at desc)");
        jdbcTemplate.execute("create index if not exists idx_tickets_machine_status_number on tickets(queue_machine_id, status, business_date, ticket_number)");
        jdbcTemplate.execute("""
                create table if not exists ticket_outbox_events (
                    outbox_id bigserial primary key,
                    ticket_id bigint not null references tickets(ticket_id) on delete cascade,
                    event_type varchar(50) not null,
                    status varchar(20) not null default 'PENDING',
                    attempts integer not null default 0,
                    available_at timestamp not null default current_timestamp,
                    created_at timestamp not null default current_timestamp,
                    processed_at timestamp,
                    last_error varchar(1000)
                )
                """);
        jdbcTemplate.execute("create index if not exists idx_ticket_outbox_pending on ticket_outbox_events(status, available_at, created_at)");
    }

    private void executeIfPossible(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Existing legacy data may contain duplicates. Service validation still protects new writes.
        }
    }
}
