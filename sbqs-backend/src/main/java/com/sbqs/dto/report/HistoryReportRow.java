package com.sbqs.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HistoryReportRow {
    private Integer ticketNumber;
    private String customerEmail;
    private String staffName;
    private String serviceName;
    private String counterName;
    private String branchName;
    private String status;
    private String startedAt;
    private String completedAt;
}
