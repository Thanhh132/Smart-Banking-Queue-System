package com.sbqs.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseSchemaInitializerTest {

    @Test
    void createsPartialUniqueIndexForActiveCustomerTickets() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class))).thenReturn(List.of());

        new DatabaseSchemaInitializer(jdbcTemplate).ensureSingleActiveTicketPerCustomer();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).execute(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("create unique index if not exists ux_tickets_one_active_customer"));
        assertTrue(sql.contains("on tickets(customer_id)"));
        assertTrue(sql.contains("status in ('WAITING', 'SERVING')"));
    }

    @Test
    void refusesToHideExistingDuplicateActiveTickets() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class))).thenReturn(List.of(12L, 18L));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new DatabaseSchemaInitializer(jdbcTemplate).ensureSingleActiveTicketPerCustomer());

        assertTrue(exception.getMessage().contains("[12, 18]"));
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void addsIdempotencyColumnAndCustomerScopedUniqueIndex() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        new DatabaseSchemaInitializer(jdbcTemplate).ensureTicketIdempotency();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, org.mockito.Mockito.times(2)).execute(sqlCaptor.capture());
        assertTrue(sqlCaptor.getAllValues().get(0).contains("idempotency_key varchar(36)"));
        assertTrue(sqlCaptor.getAllValues().get(1).contains("ux_tickets_customer_idempotency"));
        assertTrue(sqlCaptor.getAllValues().get(1).contains("on tickets(customer_id, idempotency_key)"));
    }

    @Test
    void addsDailyNumberingIndexesAndTicketOutbox() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        new DatabaseSchemaInitializer(jdbcTemplate).ensureTicketOperationalSchema();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, org.mockito.Mockito.times(12)).execute(sqlCaptor.capture());
        String sql = String.join("\n", sqlCaptor.getAllValues());
        assertTrue(sql.contains("business_date"));
        assertTrue(sql.contains("ux_tickets_machine_day_number"));
        assertTrue(sql.contains("idx_tickets_machine_status_number"));
        assertTrue(sql.contains("create table if not exists ticket_outbox_events"));
        assertTrue(sql.contains("idx_ticket_outbox_pending"));
    }
}
