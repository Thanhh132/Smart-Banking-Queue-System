package com.sbqs.workflow;

import com.sbqs.event.TicketCalledNotification;
import com.sbqs.service.TicketNotificationMailService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TicketNotificationListenerTest {

    @Test
    void sendsEmailFromCommittedWorkflowEvent() {
        TicketNotificationMailService mailService = mock(TicketNotificationMailService.class);
        TicketNotificationListener listener = new TicketNotificationListener(mailService);
        TicketCalledNotification notification = new TicketCalledNotification(
                "customer@sbqs.vn",
                "12",
                "SBQS Thủ Đức",
                "Rút tiền",
                "Tầng 2",
                "Quầy 202",
                "Nguyễn Văn A");

        listener.sendCalledEmail(notification);

        verify(mailService).sendTicketCalled(
                "customer@sbqs.vn",
                "12",
                "SBQS Thủ Đức",
                "Rút tiền",
                "Tầng 2",
                "Quầy 202",
                "Nguyễn Văn A");
    }
}
