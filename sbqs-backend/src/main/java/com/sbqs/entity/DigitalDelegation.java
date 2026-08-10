package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "digital_delegations")
@Getter @Setter @NoArgsConstructor
public class DigitalDelegation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delegation_id") private Long delegationId;

    @Column(name = "reference_code", nullable = false, unique = true, length = 20)
    private String referenceCode;

    @ManyToOne(optional = false) @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne @JoinColumn(name = "service_id")
    private Services service;

    @Column(name = "branch_name_snapshot", length = 255)
    private String branchNameSnapshot;

    @Column(name = "service_name_snapshot", length = 255)
    private String serviceNameSnapshot;

    @Column(name = "delegate_name", nullable = false, length = 150) private String delegateName;
    @Column(name = "delegate_identity_hash", nullable = false, length = 100) private String delegateIdentityHash;
    @Column(name = "delegate_identity_last4", nullable = false, length = 4) private String delegateIdentityLast4;
    @Column(name = "delegate_date_of_birth") private LocalDate delegateDateOfBirth;
    @Column(name = "delegate_phone", length = 15) private String delegatePhone;
    @Column(name = "identity_issue_date") private LocalDate identityIssueDate;
    @Column(name = "identity_expiry_date") private LocalDate identityExpiryDate;
    @Column(name = "identity_issue_place", length = 150) private String identityIssuePlace;
    @Column(name = "relationship", nullable = false, length = 100) private String relationship;
    @Column(name = "transaction_scope", nullable = false, length = 500) private String transactionScope;
    @Column(name = "valid_from", nullable = false) private LocalDateTime validFrom;
    @Column(name = "valid_until", nullable = false) private LocalDateTime validUntil;
    @Column(name = "status", nullable = false, length = 30) private String status = "ACTIVE";
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "verified_at") private LocalDateTime verifiedAt;
    @Column(name = "used_at") private LocalDateTime usedAt;
    @ManyToOne @JoinColumn(name = "verified_by") private User verifiedBy;

    @PrePersist void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
