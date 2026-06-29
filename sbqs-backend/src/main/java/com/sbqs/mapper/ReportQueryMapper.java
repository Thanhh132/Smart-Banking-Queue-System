package com.sbqs.mapper;

import com.sbqs.dto.report.query.ServiceReportQueryRow;
import com.sbqs.dto.report.query.TicketReportQueryRow;
import com.sbqs.dto.report.query.UserReportQueryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportQueryMapper {
    List<UserReportQueryRow> findUsersForReport(@Param("branchId") Long branchId);

    List<ServiceReportQueryRow> findServicesForReport(@Param("branchId") Long branchId);

    List<TicketReportQueryRow> findTicketsForReport(@Param("branchId") Long branchId);
}
