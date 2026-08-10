package com.sbqs.workflow;

import com.sbqs.event.TicketCalledNotification;
import com.sbqs.event.TicketQueueThresholdNotification;
import com.sbqs.service.TicketNotificationMailService;
import com.sbqs.service.WebPushService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TicketNotificationListenerTest {

    @Test
    void sendsEmailAndPushFromCommittedWorkflowEvent() {
        TicketNotificationMailService mailService = mock(TicketNotificationMailService.class);
        WebPushService webPushService = mock(WebPushService.class);
        TicketNotificationListener listener = new TicketNotificationListener(mailService, webPushService);
        TicketCalledNotification notification = new TicketCalledNotification(
                42L,
                "customer@sbqs.vn",
                "12",
                "Thu Duc branch",
                "Cash withdrawal",
                "Second floor",
                "Counter 202",
                "Staff A");

        listener.sendCalledEmail(notification);

        verify(mailService).sendTicketCalled(
                "customer@sbqs.vn",
                "12",
                "Thu Duc branch",
                "Cash withdrawal",
                "Second floor",
                "Counter 202",
                "Staff A");
        verify(webPushService).sendTicketNotification(
                eq(42L), eq("customer@sbqs.vn"), eq("CALLED"), anyString(), anyString());
    }

    @Test
    void sendsPushWhenOnlyThreeTicketsRemainAhead() {
        TicketNotificationMailService mailService = mock(TicketNotificationMailService.class);
        WebPushService webPushService = mock(WebPushService.class);
        TicketNotificationListener listener = new TicketNotificationListener(mailService, webPushService);

        listener.sendQueueThresholdPush(new TicketQueueThresholdNotification(
                43L, "customer@sbqs.vn", 13, 3));

        verify(webPushService).sendTicketNotification(
                eq(43L), eq("customer@sbqs.vn"), eq("THREE_AHEAD"), anyString(), anyString());
    }
}
