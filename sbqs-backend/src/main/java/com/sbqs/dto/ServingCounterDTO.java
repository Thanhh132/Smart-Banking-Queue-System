package com.sbqs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServingCounterDTO {

    private String counterName;

    private Integer ticketNumber;
}