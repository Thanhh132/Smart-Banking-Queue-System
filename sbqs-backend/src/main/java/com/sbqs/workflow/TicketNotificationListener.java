package com.sbqs.workflow;

import com.sbqs.event.TicketCalledNotification;
import com.sbqs.event.TicketQueueThresholdNotification;
import com.sbqs.service.TicketNotificationMailService;
import com.sbqs.service.WebPushService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TicketNotificationListener {
    private final TicketNotificationMailService mailService;
    private final WebPushService webPushService;

    public TicketNotificationListener(TicketNotificationMailService mailService, WebPushService webPushService) {
        this.mailService = mailService;
        this.webPushService = webPushService;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendCalledEmail(TicketCalledNotification notification) {
        mailService.sendTicketCalled(
                notification.customerEmail(),
                notification.ticketNumber(),
                notification.branchName(),
                notification.serviceName(),
                notification.queueMachineLocationNote(),
                notification.counterName(),
                notification.staffName());
        webPushService.sendTicketNotification(
                notification.ticketId(),
                notification.customerEmail(),
                "CALLED",
                "Đã đến lượt bạn",
                "Phiếu #" + notification.ticketNumber() + " đang được gọi tại "
                        + notification.counterName() + ".");
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendQueueThresholdPush(TicketQueueThresholdNotification notification) {
        webPushService.sendTicketNotification(
                notification.ticketId(),
                notification.customerEmail(),
                "THREE_AHEAD",
                "Sắp đến lượt bạn",
                "Phiếu #" + notification.ticketNumber() + " còn "
                        + notification.peopleAhead() + " người chờ phía trước.");
    }
}
