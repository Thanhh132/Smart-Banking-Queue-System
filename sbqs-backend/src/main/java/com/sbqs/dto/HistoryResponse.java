package com.sbqs.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class HistoryResponse {

    private Long historyId;

    private Integer ticketNumber;

    private String serviceName;

    private String counterName;

    private String branchName;

    private String queueMachineName;

    private String customerEmail;

    private String staffName;

    private Long staffId;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String staffNote;
}
