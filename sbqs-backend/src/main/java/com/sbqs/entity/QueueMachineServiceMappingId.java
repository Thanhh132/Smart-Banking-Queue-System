package com.sbqs.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class QueueMachineServiceMappingId
        implements Serializable {

    private Long queueMachineId;

    private Long serviceId;
}