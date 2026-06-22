package com.sbqs.service;

import com.sbqs.dto.report.ReportDocument;
import com.sbqs.dto.report.ReportFormat;
import com.sbqs.dto.report.ServiceReportRow;
import com.sbqs.dto.report.TicketReportRow;
import com.sbqs.dto.report.UserReportRow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JasperReportServiceTest {
    private final JasperReportService reportService = new JasperReportService();
    private final Map<String, Object> parameters = Map.of(
            "REPORT_TITLE", "BÁO CÁO KIỂM THỬ",
            "REPORT_SCOPE", "Chi nhánh kiểm thử",
            "GENERATED_AT", "22/06/2026 14:00",
            "TOTAL_RECORDS", 1);

    @Test
    void exportsAllTemplatesToPdfAndXlsx() {
        assertExport("users-report", List.of(new UserReportRow(
                "Nguyễn Văn A", "user@sbqs.com", "0900000000", "Nhân viên",
                "BIDV Thủ Dầu Một", "Hoạt động", "22/06/2026 14:00")));
        assertExport("services-report", List.of(new ServiceReportRow(
                "S-01", "Mở tài khoản", "BASIC", 15,
                "BIDV Thủ Dầu Một", "Hoạt động")));
        assertExport("tickets-report", List.of(new TicketReportRow(
                12, "customer@sbqs.com", "Mở tài khoản", "Máy bốc số 1",
                "BIDV Thủ Dầu Một", "Đang chờ", "22/06/2026 14:00")));
    }

    private void assertExport(String template, List<?> rows) {
        for (ReportFormat format : ReportFormat.values()) {
            ReportDocument document = reportService.export(
                    template, "test-report", parameters, rows, format);
            assertThat(document.content()).isNotEmpty();
            assertThat(document.fileName()).endsWith("." + format.getExtension());
        }
    }
}
