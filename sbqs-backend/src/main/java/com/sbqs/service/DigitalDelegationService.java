package com.sbqs.service;

import com.sbqs.dto.delegation.CreateDelegationRequest;
import com.sbqs.dto.delegation.DelegationResponse;
import com.sbqs.dto.delegation.VerifyDelegationRequest;
import com.sbqs.entity.DigitalDelegation;
import com.sbqs.entity.Services;
import com.sbqs.entity.User;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.DigitalDelegationRepository;
import com.sbqs.repository.ServiceRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DigitalDelegationService {
    private static final java.util.Set<String> NON_DELEGATABLE_CODES = java.util.Set.of(
            "ACCOUNT_OPEN", "DEBIT_CARD_NEW", "CREDIT_CARD", "DIGITAL_BANKING",
            "IDENTITY_UPDATE", "SIGNATURE_UPDATE");
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom random = new SecureRandom();
    private final DigitalDelegationRepository repository;
    private final BranchRepository branchRepository;
    private final ServiceRepository serviceRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public DigitalDelegationService(DigitalDelegationRepository repository, BranchRepository branchRepository,
                                    ServiceRepository serviceRepository, CurrentUserService currentUserService,
                                    PasswordEncoder passwordEncoder) {
        this.repository = repository; this.branchRepository = branchRepository;
        this.serviceRepository = serviceRepository; this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public DelegationResponse create(CreateDelegationRequest request) {
        User owner = currentUserService.requireUser();
        if (!"CUSTOMER".equals(owner.getRole())) throw new RuntimeException("Chỉ khách hàng được tạo ủy quyền");
        var branch = branchRepository.findById(request.branchId()).orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
        Services service = serviceRepository.findById(request.serviceId()).orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));
        if (service.getBranch() == null || !service.getBranch().getBranchId().equals(branch.getBranchId())) {
            throw new RuntimeException("Dịch vụ không thuộc chi nhánh đã chọn");
        }
        if (NON_DELEGATABLE_CODES.contains(service.getServiceCode().toUpperCase())) {
            throw new RuntimeException("Nghiệp vụ này yêu cầu chính chủ hoặc người đại diện hợp pháp trực tiếp xác minh, không thể dùng ủy quyền thông thường");
        }
        LocalDateTime now = LocalDateTime.now();
        if (request.validUntil().isAfter(now.plusDays(30))) throw new RuntimeException("Ủy quyền chỉ được có hiệu lực tối đa 30 ngày");

        DigitalDelegation value = new DigitalDelegation();
        value.setReferenceCode(newReferenceCode()); value.setOwner(owner); value.setBranch(branch); value.setService(service);
        value.setDelegateName(request.delegateName().trim());
        value.setDelegateIdentityHash(passwordEncoder.encode(request.delegateIdentityNumber()));
        value.setDelegateIdentityLast4(request.delegateIdentityNumber().substring(request.delegateIdentityNumber().length() - 4));
        value.setDelegateDateOfBirth(request.delegateDateOfBirth()); value.setDelegatePhone(request.delegatePhone().trim());
        if (request.identityExpiryDate().isBefore(request.identityIssueDate())) {
            throw new RuntimeException("Ngày hết hạn CCCD phải sau ngày cấp");
        }
        value.setIdentityIssueDate(request.identityIssueDate()); value.setIdentityExpiryDate(request.identityExpiryDate());
        value.setIdentityIssuePlace(request.identityIssuePlace().trim());
        value.setRelationship(request.relationship().trim()); value.setTransactionScope(request.transactionScope().trim());
        value.setValidFrom(now); value.setValidUntil(request.validUntil()); value.setStatus("ACTIVE");
        return toResponse(repository.save(value));
    }

    public List<DelegationResponse> getMine() {
        User owner = currentUserService.requireUser();
        return repository.findByOwnerOrderByCreatedAtDesc(owner).stream().map(this::normalizeStatus).map(this::toResponse).toList();
    }

    @Transactional
    public DelegationResponse cancel(Long id) {
        DigitalDelegation value = owned(id);
        if (!"ACTIVE".equals(normalizeStatus(value).getStatus())) throw new RuntimeException("Chỉ ủy quyền đang hiệu lực mới được hủy");
        value.setStatus("CANCELLED"); return toResponse(repository.save(value));
    }

    @Transactional
    public DelegationResponse verify(VerifyDelegationRequest request) {
        User staff = requireStaff();
        DigitalDelegation value = repository.findByReferenceCodeIgnoreCase(request.referenceCode().trim())
                .orElseThrow(() -> new RuntimeException("Mã ủy quyền hoặc CCCD không chính xác"));
        normalizeStatus(value);
        if (!"ACTIVE".equals(value.getStatus())
                || !passwordEncoder.matches(request.delegateIdentityNumber(), value.getDelegateIdentityHash())) {
            throw new RuntimeException("Mã ủy quyền hoặc CCCD không chính xác, đã hết hạn hoặc đã sử dụng");
        }
        if (value.getIdentityExpiryDate() != null && value.getIdentityExpiryDate().isBefore(java.time.LocalDate.now())) {
            throw new RuntimeException("CCCD của người được ủy quyền đã hết hạn");
        }
        if (staff.getBranch() == null || !staff.getBranch().getBranchId().equals(value.getBranch().getBranchId())) {
            throw new RuntimeException("Ủy quyền không áp dụng tại chi nhánh của nhân viên hiện tại");
        }
        value.setStatus("VERIFIED"); value.setVerifiedAt(LocalDateTime.now()); value.setVerifiedBy(staff);
        return toResponse(repository.save(value));
    }

    @Transactional
    public DelegationResponse markUsed(Long id) {
        User staff = requireStaff();
        DigitalDelegation value = repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy ủy quyền"));
        if (!"VERIFIED".equals(value.getStatus()) || value.getVerifiedBy() == null
                || !value.getVerifiedBy().getUserId().equals(staff.getUserId())) {
            throw new RuntimeException("Nhân viên phải xác minh ủy quyền trước khi tiếp nhận");
        }
        value.setStatus("USED"); value.setUsedAt(LocalDateTime.now());
        return toResponse(repository.save(value));
    }

    private DigitalDelegation owned(Long id) {
        DigitalDelegation value = repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy ủy quyền"));
        if (!value.getOwner().getUserId().equals(currentUserService.requireUser().getUserId())) throw new RuntimeException("Bạn không có quyền thao tác ủy quyền này");
        return value;
    }
    private User requireStaff() {
        User user = currentUserService.requireUser();
        if (!"STAFF".equals(user.getRole())) throw new RuntimeException("Chỉ giao dịch viên được xác minh ủy quyền");
        return user;
    }
    private DigitalDelegation normalizeStatus(DigitalDelegation value) {
        if ("ACTIVE".equals(value.getStatus()) && value.getValidUntil().isBefore(LocalDateTime.now())) {
            value.setStatus("EXPIRED"); repository.save(value);
        }
        return value;
    }
    private String newReferenceCode() {
        String code;
        do { StringBuilder out = new StringBuilder("UQ-"); for (int i = 0; i < 8; i++) out.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]); code = out.toString(); }
        while (repository.existsByReferenceCode(code));
        return code;
    }
    private DelegationResponse toResponse(DigitalDelegation value) {
        return new DelegationResponse(value.getDelegationId(), value.getReferenceCode(), value.getDelegateName(),
                "********" + value.getDelegateIdentityLast4(), value.getDelegateDateOfBirth(), value.getDelegatePhone(),
                value.getIdentityIssueDate(), value.getIdentityExpiryDate(), value.getIdentityIssuePlace(), value.getRelationship(), value.getTransactionScope(), value.getStatus(),
                value.getOwner().getFullName(), maskEmail(value.getOwner().getEmail()),
                value.getBranch().getBranchId(), value.getBranch().getBranchName(), value.getService().getServiceId(), value.getService().getServiceName(),
                value.getValidFrom(), value.getValidUntil(), value.getCreatedAt(), value.getVerifiedAt(), value.getUsedAt());
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "-";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        return local.substring(0, Math.min(2, local.length())) + "***" + email.substring(at);
    }
}
