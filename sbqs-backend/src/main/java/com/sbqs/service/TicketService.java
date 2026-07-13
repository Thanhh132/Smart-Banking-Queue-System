package com.sbqs.service;

import com.sbqs.entity.Counter;
import com.sbqs.entity.History;
import com.sbqs.entity.QueueMachine;
import com.sbqs.entity.QueueMachineServiceMapping;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.User;
import com.sbqs.entity.FormFieldDefinition;
import com.sbqs.entity.TransactionDraft;
import com.sbqs.dto.CreatePreparedTicketRequest;
import com.sbqs.dto.TicketPaperlessFieldResponse;
import com.sbqs.dto.TicketStaffViewResponse;
import com.sbqs.dto.TicketTrackingResponse;
import com.sbqs.event.DomainEventPublisher;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.CounterSessionRepository;
import com.sbqs.repository.HistoryRepository;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.QueueMachineRepository;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.TicketRepository;
import com.sbqs.repository.UserRepository;
import com.sbqs.repository.TransactionDraftRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final QueueMachineServiceMappingRepository mappingRepository;
    private final QueueMachineRepository queueMachineRepository;
    private final CounterRepository counterRepository;
    private final HistoryRepository historyRepository;
    private final BranchRepository branchRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final CounterSessionRepository counterSessionRepository;
    private final TicketWorkflowService ticketWorkflowService;
    private final DomainEventPublisher eventPublisher;
    private final TransactionDraftRepository transactionDraftRepository;

    public TicketService(
            TicketRepository ticketRepository,
            QueueMachineServiceMappingRepository mappingRepository,
            QueueMachineRepository queueMachineRepository,
            CounterRepository counterRepository,
            HistoryRepository historyRepository,
            BranchRepository branchRepository,
            ServiceRepository serviceRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            CounterSessionRepository counterSessionRepository,
            TicketWorkflowService ticketWorkflowService,
            DomainEventPublisher eventPublisher,
            TransactionDraftRepository transactionDraftRepository) {

        this.ticketRepository = ticketRepository;
        this.mappingRepository = mappingRepository;
        this.queueMachineRepository = queueMachineRepository;
        this.counterRepository = counterRepository;
        this.historyRepository = historyRepository;
        this.branchRepository = branchRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.counterSessionRepository = counterSessionRepository;
        this.ticketWorkflowService = ticketWorkflowService;
        this.eventPublisher = eventPublisher;
        this.transactionDraftRepository = transactionDraftRepository;
    }

    @Transactional
    public Ticket createPreparedTicket(CreatePreparedTicketRequest request) {
        Services service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu"));
        Map<String, Object> sanitizedValues = validateAndSanitizeForm(service, request.values());

        Ticket ticket = new Ticket();
        Branch branch = new Branch();
        branch.setBranchId(request.branchId());
        ticket.setBranch(branch);
        ticket.setService(service);
        Ticket savedTicket = createTicket(ticket);

        TransactionDraft draft = new TransactionDraft();
        draft.setTicket(savedTicket);
        draft.setServiceId(service.getServiceId());
        draft.setServiceName(service.getServiceName());
        draft.setSchemaSnapshot(new java.util.ArrayList<>(service.getFormSchema()));
        draft.setValues(sanitizedValues);
        draft.setCreatedBy(savedTicket.getCustomerEmail());
        transactionDraftRepository.save(draft);
        return savedTicket;
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findByBranch(currentUserService.requireUser().getBranch());
    }

    @Transactional
    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    /**
     * Cấp số cho CUSTOMER: kiểm tra dịch vụ được map vào máy, chống lấy nhiều phiếu
     * đang hoạt động và tăng số thứ tự của đúng máy bốc số trong transaction.
     */
    public Ticket createTicket(Ticket ticket) {
        String customerEmail = getCurrentEmail();
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new RuntimeException("Khong xac dinh duoc khach hang dang dang nhap");
        }

        List<Ticket> activeTickets = ticketRepository.findByCustomerEmailAndStatusIn(
                customerEmail,
                List.of("WAITING", "SERVING"));

        if (!activeTickets.isEmpty()) {
            throw new RuntimeException("Ban dang co ticket chua hoan thanh. Hay cho hoan thanh hoac huy ticket truoc.");
        }

        if (ticket.getBranch() == null || ticket.getBranch().getBranchId() == null) {
            throw new RuntimeException("Chua chon chi nhanh");
        }

        if (ticket.getService() == null || ticket.getService().getServiceId() == null) {
            throw new RuntimeException("Chua chon dich vu");
        }

        Branch branch = branchRepository.findById(ticket.getBranch().getBranchId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay chi nhanh"));
        Services service = serviceRepository.findById(ticket.getService().getServiceId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu"));
        User customer = currentUserService.requireUser();
        requireCompletePaperlessProfile(customer, service);

        if (service.getBranch() == null
                || !service.getBranch().getBranchId().equals(branch.getBranchId())) {
            throw new RuntimeException("Dich vu khong thuoc chi nhanh da chon");
        }

        ticket.setBranch(branch);
        ticket.setService(service);

        QueueMachineServiceMapping mapping = mappingRepository
                .findFirstByQueueMachineBranchAndService(
                        branch,
                        service)
                .orElseThrow(() -> new RuntimeException(
                        "Dich vu nay chua duoc cau hinh cho may boc so cua chi nhanh"));

        QueueMachine queueMachine = queueMachineRepository
                .findByIdForTicketIssuing(mapping.getQueueMachine().getQueueMachineId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay may boc so"));
        ticket.setQueueMachine(queueMachine);

        int nextTicketNumber = queueMachine.getLastTicketNumber() + 1;
        queueMachine.setLastTicketNumber(nextTicketNumber);
        queueMachineRepository.save(queueMachine);

        ticket.setTicketNumber(nextTicketNumber);
        ticket.setStatus("WAITING");
        ticket.setCustomerEmail(customerEmail);

        Ticket savedTicket = ticketRepository.save(ticket);
        ticketWorkflowService.startTicketApproval(savedTicket);
        eventPublisher.publish(
                "TICKET_CREATED",
                "TICKET",
                savedTicket.getTicketId().toString(),
                savedTicket.getBranch().getBranchId(),
                Map.of(
                        "ticketNumber", savedTicket.getTicketNumber(),
                        "customerEmail", savedTicket.getCustomerEmail(),
                        "serviceName", savedTicket.getService().getServiceName(),
                        "queueMachineName", savedTicket.getQueueMachine().getMachineName()));

        return savedTicket;
    }

    public List<Ticket> getTicketsByStatus(String status) {
        return ticketRepository.findByStatus(status);
    }

    /** Lấy phiếu đang hoạt động của chính email trong JWT, không cho xem phiếu người khác. */
    public Ticket getCurrentCustomerTicket() {
        String customerEmail = getCurrentEmail();
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new RuntimeException("Khong xac dinh duoc khach hang dang dang nhap");
        }

        return ticketRepository
                .findFirstByCustomerEmailAndStatusInOrderByCreatedAtDesc(
                        customerEmail,
                        List.of("WAITING", "SERVING"))
                .orElse(null);
    }

    /** Trả trạng thái realtime và số người đang chờ phía trước cho màn hình theo dõi của khách hàng. */
    public TicketTrackingResponse trackCustomerTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu"));
        String customerEmail = getCurrentEmail();

        if (customerEmail == null
                || ticket.getCustomerEmail() == null
                || !ticket.getCustomerEmail().equalsIgnoreCase(customerEmail)) {
            throw new RuntimeException("Bạn không có quyền theo dõi phiếu này");
        }

        LocalDate today = LocalDate.now();
        long peopleAhead = "WAITING".equals(ticket.getStatus())
                ? ticketRepository.countWaitingAhead(
                        ticket.getQueueMachine(),
                        ticket.getTicketNumber(),
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay())
                : 0;
        String counterName = counterRepository.findFirstByCurrentTicketTicketId(ticketId)
                .map(Counter::getCounterName)
                .orElse(null);

        return new TicketTrackingResponse(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                ticket.getStatus(),
                peopleAhead,
                counterName,
                ticket.getBranch() == null ? null : ticket.getBranch().getBranchName(),
                ticket.getService() == null ? null : ticket.getService().getServiceName(),
                ticket.getQueueMachine() == null ? null : ticket.getQueueMachine().getQueueMachineId(),
                ticket.getQueueMachine() == null ? null : ticket.getQueueMachine().getLocationNote(),
                ticket.getServingStartedAt());
    }

    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    @Transactional
    /**
     * Nhân viên gọi phiếu WAITING tiếp theo phù hợp dịch vụ của quầy; đồng thời chuyển
     * phiếu sang SERVING, gắn quầy/nhân viên và cập nhật workflow Camunda.
     */
    public TicketStaffViewResponse callNextTicket(Long counterId) {
        Counter counter = counterRepository.findByIdForUpdate(counterId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay quay"));

        requireCurrentStaffOwnsCounter(counter);

        if (counter.getCurrentTicket() != null
                && "SERVING".equals(counter.getCurrentTicket().getStatus())) {
            throw new RuntimeException("Quay dang phuc vu khach, hay hoan thanh truoc khi goi so moi");
        }

        if (counter.getQueueMachine() == null) {
            throw new RuntimeException("Quay chua duoc gan may boc so");
        }

        if (!"ACTIVE".equalsIgnoreCase(counter.getStatus())) {
            throw new RuntimeException("Quay chua duoc nhan vien assign nen chua the goi so");
        }

        Ticket nextTicket = ticketRepository
                .findFirstByQueueMachineAndStatusOrderByTicketNumberAsc(
                        counter.getQueueMachine(),
                        "WAITING")
                .orElseThrow(() -> new RuntimeException("Không còn khách đang chờ"));

        ticketWorkflowService.approveForServing(nextTicket, counter);

        nextTicket.setStatus("SERVING");
        nextTicket.setServingStartedAt(LocalDateTime.now());
        Ticket savedTicket = ticketRepository.save(nextTicket);

        counter.setCurrentTicket(savedTicket);
        counterRepository.save(counter);
        eventPublisher.publish(
                "TICKET_CALLED",
                "TICKET",
                savedTicket.getTicketId().toString(),
                savedTicket.getBranch().getBranchId(),
                Map.of(
                        "ticketNumber", savedTicket.getTicketNumber(),
                        "counterName", counter.getCounterName(),
                        "serviceName", savedTicket.getService().getServiceName()));

        return toStaffView(savedTicket);
    }

    @Transactional(readOnly = true)
    /** Chi nhan vien dang giu quay phuc vu moi duoc xem ho so giay to cua phieu da goi. */
    public TicketStaffViewResponse getServingTicketForStaff(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay ticket"));

        if (!"SERVING".equals(ticket.getStatus())) {
            throw new RuntimeException("Chi hien ho so giay to sau khi phieu da duoc goi");
        }

        Counter counter = counterRepository.findFirstByCurrentTicketTicketId(ticketId)
                .orElseThrow(() -> new RuntimeException("Phieu nay chua duoc goi vao quay nao"));
        requireCurrentStaffOwnsCounter(counter);

        return toStaffView(ticket);
    }

    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    /** Hoàn tất phiếu đang phục vụ và ghi snapshot vào lịch sử để báo cáo không phụ thuộc dữ liệu sau này. */
    public Ticket completeTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay ticket"));

        if (!"SERVING".equals(ticket.getStatus())) {
            throw new RuntimeException("Chi ticket dang phuc vu moi duoc hoan thanh");
        }

        Counter counter = counterRepository.findAll()
                .stream()
                .filter(c -> c.getCurrentTicket() != null
                        && c.getCurrentTicket().getTicketId().equals(ticketId))
                .findFirst()
                .orElse(null);

        if (counter == null) {
            throw new RuntimeException("Ticket khong duoc phuc vu tai quay nao");
        }

        requireCurrentStaffOwnsCounter(counter);
        User currentStaff = currentUserService.requireUser();
        ticketWorkflowService.completeServing(ticket);

        ticket.setStatus("COMPLETED");

        History history = new History();
        history.setTicketId(ticket.getTicketId());
        history.setBranchId(ticket.getBranch().getBranchId());
        history.setBranchName(ticket.getBranch().getBranchName());
        history.setQueueMachineId(ticket.getQueueMachine() == null ? null : ticket.getQueueMachine().getQueueMachineId());
        history.setQueueMachineName(ticket.getQueueMachine() == null ? null : ticket.getQueueMachine().getMachineName());
        history.setCounterId(counter.getCounterId());
        history.setCounterName(counter.getCounterName());
        history.setServiceId(ticket.getService().getServiceId());
        history.setServiceName(ticket.getService().getServiceName());
        history.setStaffId(currentStaff.getUserId());
        history.setStaffName(currentStaff.getFullName());
        history.setCustomerEmail(ticket.getCustomerEmail());
        history.setTicketNumber(ticket.getTicketNumber());
        history.setStartedAt(ticket.getServingStartedAt());
        history.setCompletedAt(LocalDateTime.now());
        history.setStatus("COMPLETED");
        history.setStaffNote("Hoàn thành phục vụ khách hàng");

        historyRepository.save(history);

        if (counter != null) {
            counter.setCurrentTicket(null);
            counterRepository.save(counter);
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        eventPublisher.publish(
                "TICKET_COMPLETED",
                "TICKET",
                savedTicket.getTicketId().toString(),
                savedTicket.getBranch().getBranchId(),
                Map.of(
                        "ticketNumber", savedTicket.getTicketNumber(),
                        "counterName", counter.getCounterName(),
                        "serviceName", savedTicket.getService().getServiceName()));

        return savedTicket;
    }

    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    /** Chỉ CUSTOMER sở hữu phiếu mới được hủy phiếu chưa hoàn tất của mình. */
    public Ticket cancelTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay ticket"));

        String customerEmail = getCurrentEmail();
        if (ticket.getCustomerEmail() != null
                && customerEmail != null
                && !ticket.getCustomerEmail().equalsIgnoreCase(customerEmail)) {
            throw new RuntimeException("Ban khong co quyen huy ticket nay");
        }

        if (!"WAITING".equals(ticket.getStatus())) {
            throw new RuntimeException("Chi ticket dang cho moi duoc huy");
        }

        ticket.setStatus("CANCELLED");
        ticketWorkflowService.cancelTicket(ticket);

        Ticket savedTicket = ticketRepository.save(ticket);
        History history = new History();
        history.setTicketId(savedTicket.getTicketId());
        history.setBranchId(savedTicket.getBranch().getBranchId());
        history.setBranchName(savedTicket.getBranch().getBranchName());
        history.setQueueMachineId(savedTicket.getQueueMachine() == null ? null : savedTicket.getQueueMachine().getQueueMachineId());
        history.setQueueMachineName(savedTicket.getQueueMachine() == null ? null : savedTicket.getQueueMachine().getMachineName());
        history.setServiceId(savedTicket.getService().getServiceId());
        history.setServiceName(savedTicket.getService().getServiceName());
        history.setCustomerEmail(savedTicket.getCustomerEmail());
        history.setTicketNumber(savedTicket.getTicketNumber());
        history.setStartedAt(savedTicket.getCreatedAt());
        history.setCompletedAt(LocalDateTime.now());
        history.setStatus("CANCELLED");
        history.setStaffNote("Khách hàng hủy phiếu trước khi được phục vụ");
        historyRepository.save(history);

        eventPublisher.publish(
                "TICKET_CANCELLED",
                "TICKET",
                savedTicket.getTicketId().toString(),
                savedTicket.getBranch().getBranchId(),
                Map.of(
                        "ticketNumber", savedTicket.getTicketNumber(),
                        "customerEmail", savedTicket.getCustomerEmail(),
                        "serviceName", savedTicket.getService().getServiceName()));

        return savedTicket;
    }

    private String getCurrentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }

        return email;
    }

    /** Bảo đảm nhân viên chỉ thao tác trên quầy đang được chính mình nhận trong ca hiện tại. */
    /** Chan cap so cho dich vu can giay to online khi customer chua khai bao du thong tin bat buoc. */
    private void requireCompletePaperlessProfile(User customer, Services service) {
        List<String> requiredFields = service.getRequiredCustomerFields();
        if (requiredFields == null || requiredFields.isEmpty()) {
            return;
        }

        boolean incomplete = requiredFields.stream()
                .anyMatch(field -> isBlank(getPaperlessValue(customer, field)));
        if (incomplete) {
            throw new RuntimeException("Vui long bo sung day du ho so giay to truoc khi lay so dich vu nay");
        }
    }

    private String getPaperlessValue(User user, String field) {
        return switch (field) {
            case "FULL_NAME" -> user.getFullName();
            case "DATE_OF_BIRTH" -> user.getDateOfBirth();
            case "GENDER" -> user.getGender();
            case "NATIONALITY" -> user.getNationality();
            case "IDENTITY_NUMBER" -> user.getIdentityNumber();
            case "IDENTITY_ISSUE_DATE" -> user.getIdentityIssueDate();
            case "IDENTITY_ISSUE_PLACE" -> user.getIdentityIssuePlace();
            case "PASSPORT_NUMBER" -> user.getPassportNumber();
            case "VISA_NUMBER" -> user.getVisaNumber();
            case "MOBILE_PHONE" -> user.getPhone();
            case "EMAIL_ADDRESS" -> user.getEmail();
            case "PERMANENT_ADDRESS" -> user.getPermanentAddress();
            case "CONTACT_ADDRESS" -> user.getContactAddress();
            case "OCCUPATION" -> user.getOccupation();
            case "EMPLOYMENT_STATUS" -> user.getEmploymentStatus();
            case "EMPLOYER_NAME" -> user.getEmployerName();
            case "WORK_PHONE" -> user.getWorkPhone();
            case "JOB_TITLE" -> user.getJobTitle();
            case "MONTHLY_INCOME" -> user.getMonthlyIncome();
            case "SALARY_PAYMENT_METHOD" -> user.getSalaryPaymentMethod();
            case "ACCOUNT_NUMBER" -> user.getAccountNumber();
            case "CARD_DELIVERY_ADDRESS" -> user.getCardDeliveryAddress();
            default -> "";
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private TicketStaffViewResponse toStaffView(Ticket ticket) {
        Services service = ticket.getService();
        TransactionDraft draft = transactionDraftRepository.findByTicketTicketId(ticket.getTicketId()).orElse(null);
        if (draft != null) {
            List<TicketPaperlessFieldResponse> draftFields = draft.getSchemaSnapshot().stream()
                    .map(field -> new TicketPaperlessFieldResponse(
                            field.key(), field.label(), displayDraftValue(draft.getValues().get(field.key()))))
                    .toList();
            return new TicketStaffViewResponse(
                    ticket.getTicketId(), ticket.getTicketNumber(), ticket.getStatus(),
                    ticket.getCustomerEmail(), ticket.getServingStartedAt(),
                    service == null ? null : new TicketStaffViewResponse.ServiceSummary(
                            service.getServiceId(), service.getServiceCode(), service.getServiceName(), service.getServiceType()),
                    draftFields, !draftFields.isEmpty());
        }
        List<String> requiredFields = service == null || service.getRequiredCustomerFields() == null
                ? List.of()
                : service.getRequiredCustomerFields();
        User customer = ticket.getCustomerEmail() == null
                ? null
                : userRepository.findByEmailIgnoreCase(ticket.getCustomerEmail()).orElse(null);
        List<TicketPaperlessFieldResponse> paperlessFields = customer == null
                ? List.of()
                : requiredFields.stream()
                        .map(key -> new TicketPaperlessFieldResponse(
                                key,
                                getPaperlessLabel(key),
                                getPaperlessValue(customer, key)))
                        .toList();

        return new TicketStaffViewResponse(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                ticket.getStatus(),
                ticket.getCustomerEmail(),
                ticket.getServingStartedAt(),
                service == null ? null : new TicketStaffViewResponse.ServiceSummary(
                        service.getServiceId(),
                        service.getServiceCode(),
                        service.getServiceName(),
                        service.getServiceType()),
                paperlessFields,
                !paperlessFields.isEmpty());
    }

    private Map<String, Object> validateAndSanitizeForm(Services service, Map<String, Object> submitted) {
        List<FormFieldDefinition> schema = service.getFormSchema() == null ? List.of() : service.getFormSchema();
        Set<String> allowedKeys = schema.stream().map(FormFieldDefinition::key).collect(Collectors.toSet());
        if (!allowedKeys.containsAll(submitted.keySet())) {
            throw new RuntimeException("Bieu mau chua truong du lieu khong duoc phep");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (FormFieldDefinition field : schema) {
            Object raw = submitted.get(field.key());
            String value = raw == null ? "" : String.valueOf(raw).trim();
            if (field.required() && value.isBlank()) {
                throw new RuntimeException("Vui long nhap: " + field.label());
            }
            if (value.length() > 500) {
                throw new RuntimeException("Du lieu qua dai tai truong: " + field.label());
            }
            if (!value.isBlank() && List.of("SELECT", "RADIO").contains(field.type())
                    && (field.options() == null || !field.options().contains(value))) {
                throw new RuntimeException("Gia tri khong hop le tai truong: " + field.label());
            }
            if (!value.isBlank() && "NUMBER".equals(field.type()) && !value.matches("^\\d{1,18}$")) {
                throw new RuntimeException("Gia tri so khong hop le tai truong: " + field.label());
            }
            result.put(field.key(), "CHECKBOX".equals(field.type()) ? Boolean.parseBoolean(value) : value);
        }
        return result;
    }

    private String displayDraftValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return "-";
        if (value instanceof Boolean flag) return flag ? "Có" : "Không";
        return String.valueOf(value);
    }

    private String getPaperlessLabel(String field) {
        return switch (field) {
            case "FULL_NAME" -> "Họ và tên";
            case "DATE_OF_BIRTH" -> "Ngày sinh";
            case "GENDER" -> "Giới tính";
            case "NATIONALITY" -> "Quốc tịch";
            case "IDENTITY_NUMBER" -> "Số CCCD/CMND";
            case "IDENTITY_ISSUE_DATE" -> "Ngày cấp CCCD";
            case "IDENTITY_ISSUE_PLACE" -> "Nơi cấp CCCD";
            case "PASSPORT_NUMBER" -> "Số hộ chiếu";
            case "VISA_NUMBER" -> "Số thị thực";
            case "MOBILE_PHONE" -> "Số điện thoại di động";
            case "EMAIL_ADDRESS" -> "Địa chỉ email";
            case "PERMANENT_ADDRESS" -> "Địa chỉ thường trú";
            case "CONTACT_ADDRESS" -> "Địa chỉ cư trú hiện tại";
            case "OCCUPATION" -> "Nghề nghiệp";
            case "EMPLOYMENT_STATUS" -> "Tình trạng việc làm";
            case "EMPLOYER_NAME" -> "Tên công ty/Cơ quan";
            case "WORK_PHONE" -> "Số điện thoại nơi làm việc";
            case "JOB_TITLE" -> "Chức vụ/Vị trí";
            case "MONTHLY_INCOME" -> "Thu nhập trung bình hàng tháng";
            case "SALARY_PAYMENT_METHOD" -> "Hình thức nhận lương";
            case "ACCOUNT_NUMBER" -> "Số tài khoản liên kết";
            case "CARD_DELIVERY_ADDRESS" -> "Địa chỉ nhận thẻ";
            default -> field;
        };
    }

    /** Bao dam nhan vien chi thao tac tren quay dang duoc chinh minh nhan trong ca hien tai. */
    private void requireCurrentStaffOwnsCounter(Counter counter) {
        User currentStaff = currentUserService.requireUser();
        counterSessionRepository
                .findFirstByCounterIdAndStatusOrderByStartedAtDesc(counter.getCounterId(), "ACTIVE")
                .filter(session -> session.getStaffId().equals(currentStaff.getUserId()))
                .orElseThrow(() -> new RuntimeException(
                        "Ban phai assign vao quay nay truoc khi thao tac ticket"));
    }
}
