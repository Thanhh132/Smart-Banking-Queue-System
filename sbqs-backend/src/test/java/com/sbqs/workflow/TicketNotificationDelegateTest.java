package com.sbqs.workflow;

import com.sbqs.event.TicketCalledNotification;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketNotificationDelegateTest {

    @Test
    void sendsWorkflowVariablesToMailService() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        DelegateExecution execution = mock(DelegateExecution.class);
        TicketNotificationDelegate delegate = new TicketNotificationDelegate(eventPublisher);

        when(execution.getVariable("ticketId")).thenReturn(42L);
        when(execution.getVariable("customerEmail")).thenReturn("customer@sbqs.vn");
        when(execution.getVariable("ticketNumber")).thenReturn(12);
        when(execution.getVariable("branchName")).thenReturn("SBQS Thủ Đức");
        when(execution.getVariable("serviceName")).thenReturn("Rút tiền");
        when(execution.getVariable("queueMachineLocationNote")).thenReturn("Tầng 2");
        when(execution.getVariable("counterName")).thenReturn("Quầy 202");
        when(execution.getVariable("staffName")).thenReturn("Nguyễn Văn A");

        delegate.execute(execution);

        ArgumentCaptor<TicketCalledNotification> event = ArgumentCaptor.forClass(TicketCalledNotification.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(42L, event.getValue().ticketId());
        assertEquals("customer@sbqs.vn", event.getValue().customerEmail());
        assertEquals("12", event.getValue().ticketNumber());
        assertEquals("Quầy 202", event.getValue().counterName());
        assertEquals("Tầng 2", event.getValue().queueMachineLocationNote());
    }
}
