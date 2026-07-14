package com.sbqs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Mẫu dịch vụ toàn hệ thống do Super Admin quản lý. */
@Entity
@Table(name = "service_catalog")
@Getter
@Setter
@NoArgsConstructor
public class ServiceCatalog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "catalog_id")
    private Long catalogId;

    @Column(name = "service_code", nullable = false, unique = true)
    private String serviceCode;

    @Column(name = "service_name", nullable = false, unique = true)
    private String serviceName;

    @Column(name = "service_type", nullable = false)
    private String serviceType = "BASIC";

    @Column(name = "description")
    private String description;

    @Column(name = "estimated_time", nullable = false)
    private Integer estimatedTime = 15;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Convert(converter = FormSchemaConverter.class)
    @Column(name = "form_schema", nullable = false, columnDefinition = "text")
    private List<FormFieldDefinition> formSchema = new ArrayList<>();
}
