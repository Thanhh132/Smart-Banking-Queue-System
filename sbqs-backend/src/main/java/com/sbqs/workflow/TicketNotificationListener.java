package com.sbqs.workflow;

import com.sbqs.event.TicketCalledNotification;
import com.sbqs.event.TicketQueueThresholdNotification;
import com.sbqs.service.TicketNotificationMailService;
import com.sbqs.service.WebPushService;
import com.sbqs.entity.Ticket;
import com.sbqs.repository.TicketRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TicketNotificationListener {
    private final TicketNotificationMailService mailService;
    private final WebPushService webPushService;
    private final TicketRepository ticketRepository;

    public TicketNotificationListener(
            TicketNotificationMailService mailService,
            WebPushService webPushService,
            TicketRepository ticketRepository) {
        this.mailService = mailService;
        this.webPushService = webPushService;
        this.ticketRepository = ticketRepository;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendCalledEmail(TicketCalledNotification notification) {
        Ticket ticket = ticketRepository.findById(notification.ticketId()).orElse(null);
        if (ticket == null || ticket.getCustomer() == null
                || ticket.getCustomer().getEmail() == null
                || ticket.getCustomer().getEmail().isBlank()) return;
        mailService.sendTicketCalled(
                ticket.getCustomer().getEmail(),
                notification.ticketNumber(),
                notification.branchName(),
                notification.serviceName(),
                notification.queueMachineLocationNote(),
                notification.counterName(),
                notification.staffName());
        webPushService.sendTicketNotification(
                notification.ticketId(),
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
                "THREE_AHEAD",
                "Sắp đến lượt bạn",
                "Phiếu #" + notification.ticketNumber() + " còn "
                        + notification.peopleAhead() + " người chờ phía trước.");
    }
}
