package com.sbqs.dto.report.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ServiceReportQueryRow {
    private String serviceCode;
    private String serviceName;
    private String serviceType;
    private Integer estimatedTime;
    private String branchName;
    private String status;
}
