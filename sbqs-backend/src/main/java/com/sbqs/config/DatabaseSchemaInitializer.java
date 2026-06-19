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
        jdbcTemplate.execute("alter table tickets add column if not exists customer_email varchar(255)");

        jdbcTemplate.execute("""
                create table if not exists counter_sessions (
                    counter_session_id bigserial primary key,
                    counter_id bigint not null references counters(counter_id),
                    staff_id bigint not null references users(user_id),
                    branch_id bigint not null references branches(branch_id),
                    started_at timestamp not null,
                    ended_at timestamp,
                    status varchar(255) not null
                )
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
