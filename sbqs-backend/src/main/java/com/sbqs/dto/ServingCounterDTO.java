package com.sbqs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServingCounterDTO {

    private String counterName;

    private Integer ticketNumber;

    private String status;

    private String queueMachineName;
}
