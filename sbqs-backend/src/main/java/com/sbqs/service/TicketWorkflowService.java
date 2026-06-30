package com.sbqs.service;

import com.sbqs.dto.TicketWorkflowTaskResponse;
import com.sbqs.entity.Counter;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.User;
import com.sbqs.repository.TicketRepository;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TicketWorkflowService {

    private static final String PROCESS_KEY = "ticketApprovalProcess";
    private static final String APPROVE_TASK = "Task_ApproveTicket";
    private static final String SERVE_TASK = "Task_ServeTicket";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;

    public TicketWorkflowService(
            RuntimeService runtimeService,
            TaskService taskService,
            TicketRepository ticketRepository,
            CurrentUserService currentUserService) {

        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.ticketRepository = ticketRepository;
        this.currentUserService = currentUserService;
    }

    public void startTicketApproval(Ticket ticket) {
        if (ticket.getTicketId() == null) {
            throw new RuntimeException("Khong the khoi tao workflow cho ticket chua duoc luu");
        }

        if (findActiveProcess(ticket.getTicketId()) != null) {
            return;
        }

        runtimeService.startProcessInstanceByKey(
                PROCESS_KEY,
                ticket.getTicketId().toString(),
                Map.ofEntries(
                        Map.entry("ticketId", ticket.getTicketId()),
                        Map.entry("ticketNumber", ticket.getTicketNumber()),
                        Map.entry("customerEmail", Objects.toString(ticket.getCustomerEmail(), "")),
                        Map.entry("branchId", ticket.getBranch().getBranchId()),
                        Map.entry("branchName", ticket.getBranch().getBranchName()),
                        Map.entry("serviceId", ticket.getService().getServiceId()),
                        Map.entry("serviceName", ticket.getService().getServiceName()),
                        Map.entry("queueMachineId", ticket.getQueueMachine().getQueueMachineId()),
                        Map.entry("queueMachineName", ticket.getQueueMachine().getMachineName()),
                        Map.entry("queueMachineLocationNote", Objects.toString(ticket.getQueueMachine().getLocationNote(), "")),
                        Map.entry("queueMachineInstructionNote", Objects.toString(ticket.getQueueMachine().getInstructionNote(), "")),
                        Map.entry("status", ticket.getStatus())));
    }

    public void approveForServing(Ticket ticket, Counter counter) {
        startTicketApproval(ticket);

        Task approveTask = findActiveTask(ticket.getTicketId(), APPROVE_TASK);
        if (approveTask == null) {
            throw new RuntimeException("Ticket khong o buoc cho nhan vien duyet");
        }

        User staff = currentUserService.requireUser();
        String staffEmail = staff.getEmail();

        if (approveTask.getAssignee() == null) {
            taskService.claim(approveTask.getId(), staffEmail);
        } else if (!approveTask.getAssignee().equalsIgnoreCase(staffEmail)) {
            throw new RuntimeException("Ticket nay dang duoc nhan vien khac xu ly");
        }

        taskService.complete(
                approveTask.getId(),
                Map.ofEntries(
                        Map.entry("staffId", staff.getUserId()),
                        Map.entry("staffEmail", staffEmail),
                        Map.entry("staffName", staff.getFullName()),
                        Map.entry("counterId", counter.getCounterId()),
                        Map.entry("counterName", counter.getCounterName()),
                        Map.entry("queueMachineName", ticket.getQueueMachine().getMachineName()),
                        Map.entry("queueMachineLocationNote", Objects.toString(ticket.getQueueMachine().getLocationNote(), "")),
                        Map.entry("queueMachineInstructionNote", Objects.toString(ticket.getQueueMachine().getInstructionNote(), "")),
                        Map.entry("status", "SERVING")));
    }

    public void completeServing(Ticket ticket) {
        Task servingTask = findActiveTask(ticket.getTicketId(), SERVE_TASK);
        if (servingTask == null) {
            return;
        }

        User staff = currentUserService.requireUser();
        if (servingTask.getAssignee() != null
                && !servingTask.getAssignee().equalsIgnoreCase(staff.getEmail())) {
            throw new RuntimeException("Chi nhan vien dang phuc vu ticket moi duoc hoan thanh workflow");
        }

        taskService.complete(servingTask.getId(), Map.of("status", "COMPLETED"));
    }

    public void cancelTicket(Ticket ticket) {
        ProcessInstance processInstance = findActiveProcess(ticket.getTicketId());
        if (processInstance != null) {
            runtimeService.deleteProcessInstance(
                    processInstance.getId(),
                    "Customer cancelled ticket " + ticket.getTicketId());
        }
    }

    public List<TicketWorkflowTaskResponse> getPendingApprovalTasks() {
        User currentUser = currentUserService.requireUser();

        return taskService.createTaskQuery()
                .processDefinitionKey(PROCESS_KEY)
                .taskDefinitionKey(APPROVE_TASK)
                .active()
                .list()
                .stream()
                .map(task -> mapTask(task, currentUser))
                .filter(Objects::nonNull)
                .toList();
    }

    private TicketWorkflowTaskResponse mapTask(Task task, User currentUser) {
        Object ticketIdVariable = runtimeService.getVariable(
                task.getProcessInstanceId(),
                "ticketId");

        if (!(ticketIdVariable instanceof Number ticketIdNumber)) {
            return null;
        }

        return ticketRepository.findById(ticketIdNumber.longValue())
                .filter(ticket -> ticket.getBranch() != null
                        && currentUser.getBranch() != null
                        && ticket.getBranch().getBranchId().equals(currentUser.getBranch().getBranchId()))
                .map(ticket -> new TicketWorkflowTaskResponse(
                        task.getId(),
                        task.getName(),
                        ticket.getTicketId(),
                        ticket.getTicketNumber(),
                        ticket.getBranch().getBranchName(),
                        ticket.getService().getServiceName(),
                        ticket.getQueueMachine().getMachineName(),
                        ticket.getCustomerEmail(),
                        ticket.getStatus(),
                        ticket.getCreatedAt()))
                .orElse(null);
    }

    private ProcessInstance findActiveProcess(Long ticketId) {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .processInstanceBusinessKey(ticketId.toString())
                .active()
                .singleResult();
    }

    private Task findActiveTask(Long ticketId, String taskDefinitionKey) {
        return taskService.createTaskQuery()
                .processDefinitionKey(PROCESS_KEY)
                .processInstanceBusinessKey(ticketId.toString())
                .taskDefinitionKey(taskDefinitionKey)
                .active()
                .singleResult();
    }
}
