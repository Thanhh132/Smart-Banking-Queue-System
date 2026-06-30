package com.sbqs.workflow;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TicketApprovalProcessTest {

    @Test
    void containsMakerCheckerAndEmailServiceTask() {
        try (InputStream input = getClass().getResourceAsStream("/processes/ticket-approval.bpmn")) {
            assertNotNull(input);
            BpmnModelInstance model = Bpmn.readModelFromStream(input);

            UserTask approveTask = model.getModelElementById("Task_ApproveTicket");
            ServiceTask emailTask = model.getModelElementById("Task_SendCalledEmail");
            UserTask serveTask = model.getModelElementById("Task_ServeTicket");

            assertNotNull(approveTask);
            assertNotNull(emailTask);
            assertNotNull(serveTask);
            assertEquals("STAFF", approveTask.getCamundaCandidateGroups());
            assertEquals("${ticketNotificationDelegate}", emailTask.getCamundaDelegateExpression());
            assertEquals("${staffEmail}", serveTask.getCamundaAssignee());
        } catch (Exception ex) {
            throw new AssertionError("Không đọc được quy trình ticket approval", ex);
        }
    }
}
