package com.sbqs.dto;

import lombok.Data;

import java.util.List;

@Data
public class QueueMonitorResponse {

    private String branchName;

    private List<ServingCounterDTO> servingCounters;

    private Long waitingCount;
}