package com.sbqs.workflow;

import com.sbqs.event.TicketCalledNotification;
import com.sbqs.service.TicketNotificationMailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TicketNotificationListener {
    private final TicketNotificationMailService mailService;

    public TicketNotificationListener(TicketNotificationMailService mailService) {
        this.mailService = mailService;
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
    }
}
