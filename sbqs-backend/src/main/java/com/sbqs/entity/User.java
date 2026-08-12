package com.sbqs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String fullName;

    @Column(unique = true)
    private String email;

    @JsonIgnore
    private String passwordHash;

    @JsonIgnore
    @Column(name = "keycloak_user_id")
    private String keycloakUserId;

    @Column(name = "identity_provider")
    private String identityProvider;

    private String phone;

    @Column(name = "gender")
    private String gender;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "passport_number")
    private String passportNumber;

    @Column(name = "visa_number")
    private String visaNumber;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Column(name = "identity_number")
    private String identityNumber;

    @Column(name = "identity_issue_date")
    private String identityIssueDate;

    @Column(name = "identity_issue_place")
    private String identityIssuePlace;

    @Column(name = "permanent_address")
    private String permanentAddress;

    @Column(name = "contact_address")
    private String contactAddress;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "employment_status")
    private String employmentStatus;

    @Column(name = "employer_name")
    private String employerName;

    @Column(name = "work_phone")
    private String workPhone;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "monthly_income")
    private String monthlyIncome;

    @Column(name = "salary_payment_method")
    private String salaryPaymentMethod;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "card_delivery_address")
    private String cardDeliveryAddress;

    private String role;

    private String status;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @JsonIgnore
    @OneToOne(mappedBy = "user")
    private CustomerProfile customerProfile;

    @PrePersist
    public void prePersist() {

        createdAt = LocalDateTime.now();

        if (status == null) {
            status = "ACTIVE";
        }
    }
}
