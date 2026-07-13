package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "transaction_drafts")
@Getter
@Setter
@NoArgsConstructor
public class TransactionDraft {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_id")
    private Long draftId;

    @OneToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Convert(converter = FormSchemaConverter.class)
    @Column(name = "schema_snapshot", nullable = false, columnDefinition = "text")
    private List<FormFieldDefinition> schemaSnapshot = new ArrayList<>();

    @Convert(converter = FormValuesConverter.class)
    @Column(name = "values_payload", nullable = false, columnDefinition = "text")
    private Map<String, Object> values = new LinkedHashMap<>();

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
