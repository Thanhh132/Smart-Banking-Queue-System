package com.sbqs.service;

import com.sbqs.entity.Branch;
import com.sbqs.entity.History;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.User;
import com.sbqs.repository.HistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HistoryCustomerRelationTest {
    @Test
    void customerHistoryScopeUsesCustomerId() {
        HistoryRepository histories = mock(HistoryRepository.class);
        HistoryService service = new HistoryService(histories, mock(CurrentUserService.class));
        User customer = customer(31L, "current@example.com");
        when(histories.findByCustomerUserId(31L)).thenReturn(List.of());

        service.findScopedHistory(customer);

        verify(histories).findByCustomerUserId(31L);
    }

    @Test
    void historyStoresCustomerRelationAndEmailSnapshotAtHistoryCreation() {
        HistoryRepository histories = mock(HistoryRepository.class);
        HistoryService service = new HistoryService(histories, mock(CurrentUserService.class));
        User customer = customer(32L, "snapshot@example.com");
        Ticket ticket = new Ticket();
        ticket.setTicketId(90L);
        ticket.setTicketNumber(8);
        ticket.setCustomer(customer);
        Branch branch = new Branch();
        branch.setBranchId(2L);
        branch.setBranchName("Branch");
        ticket.setBranch(branch);
        Services bankingService = new Services();
        bankingService.setServiceId(3L);
        bankingService.setServiceName("Service");
        ticket.setService(bankingService);

        service.recordCancelled(ticket);

        ArgumentCaptor<History> captor = ArgumentCaptor.forClass(History.class);
        verify(histories).save(captor.capture());
        assertEquals(32L, captor.getValue().getCustomer().getUserId());
        assertEquals("snapshot@example.com", captor.getValue().getCustomerEmail());
    }

    private User customer(Long id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setRole("CUSTOMER");
        user.setEmail(email);
        return user;
    }
}
