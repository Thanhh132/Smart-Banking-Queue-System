package com.sbqs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "queue_machine_services")
@Getter
@Setter
@NoArgsConstructor
public class QueueMachineServiceMapping {

    @EmbeddedId
    private QueueMachineServiceMappingId id;

    @ManyToOne
    @MapsId("queueMachineId")
    @JoinColumn(name = "queue_machine_id")
    private QueueMachine queueMachine;

    @ManyToOne
    @MapsId("serviceId")
    @JoinColumn(name = "service_id")
    private Services service;
}