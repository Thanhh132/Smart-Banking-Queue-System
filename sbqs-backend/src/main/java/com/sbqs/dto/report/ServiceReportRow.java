package com.sbqs.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ServiceReportRow {
    private String serviceCode;
    private String serviceName;
    private String serviceType;
    private Integer estimatedTime;
    private String branchName;
    private String status;
}
