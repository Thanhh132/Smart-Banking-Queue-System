package com.sbqs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_profiles")
@Getter
@Setter
@NoArgsConstructor
public class CustomerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_profile_id")
    private Long customerProfileId;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(name = "date_of_birth") private String dateOfBirth;
    @Column(name = "gender") private String gender;
    @Column(name = "nationality") private String nationality;
    @Column(name = "passport_number") private String passportNumber;
    @Column(name = "visa_number") private String visaNumber;
    @Column(name = "identity_number") private String identityNumber;
    @Column(name = "identity_issue_date") private String identityIssueDate;
    @Column(name = "identity_issue_place") private String identityIssuePlace;
    @Column(name = "permanent_address") private String permanentAddress;
    @Column(name = "contact_address") private String contactAddress;
    @Column(name = "occupation") private String occupation;
    @Column(name = "employment_status") private String employmentStatus;
    @Column(name = "employer_name") private String employerName;
    @Column(name = "work_phone") private String workPhone;
    @Column(name = "job_title") private String jobTitle;
    @Column(name = "monthly_income") private String monthlyIncome;
    @Column(name = "salary_payment_method") private String salaryPaymentMethod;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
