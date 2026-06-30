package com.sbqs.workflow;

import com.sbqs.event.TicketCalledNotification;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component("ticketNotificationDelegate")
public class TicketNotificationDelegate implements JavaDelegate {
    private final ApplicationEventPublisher eventPublisher;

    public TicketNotificationDelegate(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void execute(DelegateExecution execution) {
        eventPublisher.publishEvent(new TicketCalledNotification(
                text(execution, "customerEmail"),
                text(execution, "ticketNumber"),
                text(execution, "branchName"),
                text(execution, "serviceName"),
                text(execution, "queueMachineLocationNote"),
                text(execution, "counterName"),
                text(execution, "staffName")));
    }

    private String text(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);
        return value == null ? "" : String.valueOf(value);
    }
}
