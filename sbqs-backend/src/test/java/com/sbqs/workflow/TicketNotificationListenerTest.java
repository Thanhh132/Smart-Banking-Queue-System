package com.sbqs.workflow;

import com.sbqs.event.TicketCalledNotification;
import com.sbqs.event.TicketQueueThresholdNotification;
import com.sbqs.service.TicketNotificationMailService;
import com.sbqs.service.WebPushService;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.User;
import com.sbqs.repository.TicketRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;

class TicketNotificationListenerTest {

    @Test
    void sendsEmailAndPushFromCommittedWorkflowEvent() {
        TicketNotificationMailService mailService = mock(TicketNotificationMailService.class);
        WebPushService webPushService = mock(WebPushService.class);
        TicketRepository tickets = mock(TicketRepository.class);
        Ticket ticket = new Ticket();
        User customer = new User();
        customer.setUserId(7L);
        customer.setEmail("current@sbqs.vn");
        ticket.setCustomer(customer);
        when(tickets.findById(42L)).thenReturn(Optional.of(ticket));
        TicketNotificationListener listener = new TicketNotificationListener(mailService, webPushService, tickets);
        TicketCalledNotification notification = new TicketCalledNotification(
                42L,
                "12",
                "Thu Duc branch",
                "Cash withdrawal",
                "Second floor",
                "Counter 202",
                "Staff A");

        listener.sendCalledEmail(notification);

        verify(mailService).sendTicketCalled(
                "current@sbqs.vn",
                "12",
                "Thu Duc branch",
                "Cash withdrawal",
                "Second floor",
                "Counter 202",
                "Staff A");
        verify(webPushService).sendTicketNotification(
                eq(42L), eq("CALLED"), anyString(), anyString());
    }

    @Test
    void sendsPushWhenOnlyThreeTicketsRemainAhead() {
        TicketNotificationMailService mailService = mock(TicketNotificationMailService.class);
        WebPushService webPushService = mock(WebPushService.class);
        TicketNotificationListener listener = new TicketNotificationListener(
                mailService, webPushService, mock(TicketRepository.class));

        listener.sendQueueThresholdPush(new TicketQueueThresholdNotification(
                43L, 13, 3));

        verify(webPushService).sendTicketNotification(
                eq(43L), eq("THREE_AHEAD"), anyString(), anyString());
    }
}
