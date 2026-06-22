package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "bank_name", nullable = false)
    private String bankName;  // Ví dụ: BIDV, Vietcombank

    @Column(name = "branch_code", nullable = false, unique = true)
    private String branchCode; // Mã chi nhánh, duy nhất

    @Column(name = "branch_name", nullable = false)
    private String branchName; // Tên chi nhánh

    @Column(name = "province")
    private String province;

    @Column(name = "district")
    private String district;

    @Column(name = "ward")
    private String ward;

    @Column(name = "address")
    private String address;

    @Column(name = "phone")
    private String phone;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE"; // ACTIVE / INACTIVE

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "latitude", nullable = false)
    private Double latitude; // Vĩ độ, để tính toán khoảng cách

    @Column(name = "longitude", nullable = false)
    private Double longitude; // Kinh độ, để tính toán khoảng cách
}
