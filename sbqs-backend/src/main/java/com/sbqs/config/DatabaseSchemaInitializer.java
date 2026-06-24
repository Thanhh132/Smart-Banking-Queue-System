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
    public void initialize() {
        jdbcTemplate.execute("alter table branches add column if not exists province varchar(255)");
        jdbcTemplate.execute("alter table branches add column if not exists district varchar(255)");
        jdbcTemplate.execute("alter table branches add column if not exists ward varchar(255)");
        jdbcTemplate.execute("alter table tickets add column if not exists customer_email varchar(255)");
        jdbcTemplate.execute("alter table tickets alter column service_id drop not null");
        jdbcTemplate.execute("alter table tickets drop constraint if exists tickets_ticket_number_key");
        jdbcTemplate.execute("alter table tickets drop constraint if exists uk_tickets_ticket_number");
        jdbcTemplate.execute("alter table tickets drop constraint if exists unique_ticket_per_machine");
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

        jdbcTemplate.execute("""
                create table if not exists password_reset_tokens (
                    password_reset_token_id bigserial primary key,
                    user_id bigint not null references users(user_id) on delete cascade,
                    token_hash varchar(64) not null unique,
                    expires_at timestamp not null,
                    used_at timestamp,
                    created_at timestamp not null
                )
                """);

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

        jdbcTemplate.execute("""
                update counters c
                set status = 'INACTIVE'
                where not exists (
                    select 1
                    from counter_sessions cs
                    where cs.counter_id = c.counter_id
                    and cs.status = 'ACTIVE'
                )
                """);

        jdbcTemplate.execute("alter table services drop constraint if exists services_service_code_key");
        jdbcTemplate.execute("alter table queue_machines drop constraint if exists queue_machines_machine_code_key");
        jdbcTemplate.execute("alter table counters drop constraint if exists counters_counter_code_key");

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

    private void executeIfPossible(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Existing legacy data may contain duplicates. Service validation still protects new writes.
        }
    }
}
