package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Business category that customers select when taking a queue ticket.
 * See docs/SERVICE_CATEGORY.md for the category-module mapping.
 */
@Entity
@Table(
        name = "services",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_services_branch_code",
                columnNames = {"branch_id", "service_code"}))
@Getter
@Setter
@NoArgsConstructor
public class Services {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Long serviceId;

    @NotBlank(message = "Mã dịch vụ không được để trống")
    @Column(name = "service_code", nullable = false)
    private String serviceCode;

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @NotBlank(message = "Loại dịch vụ không được để trống")
    @Column(name = "service_type", nullable = false)
    private String serviceType = "BASIC";

    @Column(name = "description")
    private String description;

    @NotNull(message = "Thời gian xử lý không được để trống")
    @Positive(message = "Thời gian xử lý phải lớn hơn 0")
    @Column(name = "estimated_time", nullable = false)
    private Integer estimatedTime = 15;
    
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";
}
