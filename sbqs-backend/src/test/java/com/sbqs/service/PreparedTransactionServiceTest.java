package com.sbqs.service;

import com.sbqs.dto.TicketStaffViewResponse;
import com.sbqs.entity.*;
import com.sbqs.repository.TransactionDraftRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PreparedTransactionServiceTest {
    @Test
    void storesSchemaProfileAndTransactionValuesAsIndependentSnapshots() {
        TransactionDraftRepository drafts = mock(TransactionDraftRepository.class);
        CustomerProfileService profiles = mock(CustomerProfileService.class);
        PreparedTransactionService service = new PreparedTransactionService(drafts, profiles);
        User customer = customer();
        Services bankingService = bankingService();
        Ticket ticket = ticket(customer, bankingService);
        Map<String, Object> profileSnapshot = Map.of("PERMANENT_ADDRESS", "Address A");
        when(profiles.snapshot(eq(customer), anyList())).thenReturn(profileSnapshot);

        service.saveDraft(ticket, bankingService, customer,
                Map.of("deliveryMethod", "HOME", "deliveryAddress", "123 Street"));

        ArgumentCaptor<TransactionDraft> captor = ArgumentCaptor.forClass(TransactionDraft.class);
        verify(drafts).save(captor.capture());
        TransactionDraft saved = captor.getValue();
        assertEquals(profileSnapshot, saved.getProfileSnapshot());
        assertEquals("123 Street", saved.getValues().get("deliveryAddress"));
        assertEquals("deliveryAddress", saved.getSchemaSnapshot().get(1).key());
    }

    @Test
    void staffViewUsesSnapshotInsteadOfLiveProfile() {
        TransactionDraftRepository drafts = mock(TransactionDraftRepository.class);
        CustomerProfileService profiles = mock(CustomerProfileService.class);
        PreparedTransactionService service = new PreparedTransactionService(drafts, profiles);
        User customer = customer();
        Services bankingService = bankingService();
        Ticket ticket = ticket(customer, bankingService);
        TransactionDraft draft = new TransactionDraft();
        draft.setProfileSnapshot(Map.of("PERMANENT_ADDRESS", "Address at 10:00"));
        draft.setSchemaSnapshot(bankingService.getFormSchema());
        draft.setValues(Map.of("deliveryMethod", "HOME", "deliveryAddress", "Delivery A"));
        when(drafts.findByTicketTicketId(99L)).thenReturn(Optional.of(draft));

        TicketStaffViewResponse view = service.toStaffView(ticket);

        assertTrue(view.paperlessFields().stream().anyMatch(field -> "Address at 10:00".equals(field.value())));
        assertTrue(view.paperlessFields().stream().anyMatch(field -> "Delivery A".equals(field.value())));
        assertEquals(customer.getUserId(), view.customer().userId());
        assertEquals(customer.getEmail(), view.customer().email());
        assertEquals(customer.getEmail(), view.customerEmail());
        verify(profiles, never()).value(any(), anyString());
    }

    private Services bankingService() {
        Services service = new Services();
        service.setServiceId(3L);
        service.setServiceCode("CARD");
        service.setServiceName("Card service");
        service.setServiceType("BASIC");
        service.setRequiredCustomerFields(List.of("PERMANENT_ADDRESS"));
        service.setFormSchema(List.of(
                new FormFieldDefinition("deliveryMethod", "Method", "SELECT", true, "", "Card", List.of("HOME", "BRANCH")),
                new FormFieldDefinition("deliveryAddress", "Address", "TEXT", true, "", "Card", List.of())));
        return service;
    }

    private User customer() {
        User user = new User();
        user.setUserId(7L);
        user.setRole("CUSTOMER");
        user.setFullName("Customer A");
        user.setEmail("a@example.com");
        user.setPhone("0900000000");
        return user;
    }

    private Ticket ticket(User customer, Services service) {
        Ticket ticket = new Ticket();
        ticket.setTicketId(99L);
        ticket.setTicketNumber(15);
        ticket.setStatus("SERVING");
        ticket.setCustomer(customer);
        ticket.setService(service);
        return ticket;
    }
}
