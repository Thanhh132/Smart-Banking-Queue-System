package com.sbqs.service;

import com.sbqs.dto.bulkimport.ServiceImportRow;
import com.sbqs.dto.bulkimport.ServiceCatalogImportRow;
import com.sbqs.dto.bulkimport.StaffImportRow;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelImportParserTest {
    private final ExcelImportParser parser = new ExcelImportParser();

    @Test
    void parsesStaffRowsAndKeepsPhoneAsText() throws Exception {
        byte[] content = workbook(
                List.of("Họ tên", "Email", "Số điện thoại", "Mật khẩu"),
                List.of("Nguyễn Văn A", "staff@sbqs.vn", "0901234567", "Staff@123"));

        List<StaffImportRow> rows = parser.parseStaff(new ByteArrayInputStream(content));

        assertEquals(1, rows.size());
        assertEquals(2, rows.getFirst().rowNumber());
        assertEquals("0901234567", rows.getFirst().phone());
        assertEquals("staff@sbqs.vn", rows.getFirst().email());
    }

    @Test
    void parsesServiceRows() throws Exception {
        byte[] content = workbook(
                List.of("Mã dịch vụ", "Tên dịch vụ", "Loại dịch vụ", "Mô tả", "Thời gian (phút)", "Trạng thái"),
                List.of("CASH-01", "Rút tiền", "BASIC", "Rút tiền tại quầy", "7", "ACTIVE"));

        List<ServiceImportRow> rows = parser.parseServices(new ByteArrayInputStream(content));

        assertEquals(1, rows.size());
        assertEquals("CASH-01", rows.getFirst().serviceCode());
        assertEquals("7", rows.getFirst().estimatedTime());
    }

    @Test
    void parsesGlobalServiceCatalogRows() throws Exception {
        byte[] content = workbook(
                List.of("Mã dịch vụ", "Tên dịch vụ", "Nhóm dịch vụ", "Mô tả", "Thời gian", "Ủy quyền"),
                List.of("PHYSICAL_CARD", "Làm thẻ vật lý", "THẺ", "Đăng ký tại quầy", "15", "CÓ"));

        List<ServiceCatalogImportRow> rows = parser.parseServiceCatalog(new ByteArrayInputStream(content));

        assertEquals(1, rows.size());
        assertEquals("PHYSICAL_CARD", rows.getFirst().serviceCode());
        assertEquals("CÓ", rows.getFirst().delegatable());
    }

    @Test
    void createsTemplatesWithExpectedHeaders() throws Exception {
        try (Workbook staffWorkbook = WorkbookFactory.create(
                new ByteArrayInputStream(parser.createStaffTemplate()));
             Workbook serviceWorkbook = WorkbookFactory.create(
                     new ByteArrayInputStream(parser.createServiceTemplate()))) {

            assertEquals("Họ tên", staffWorkbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("Mã dịch vụ", serviceWorkbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("Trạng thái", serviceWorkbook.getSheetAt(0).getRow(0).getCell(5).getStringCellValue());
        }
    }

    private byte[] workbook(List<String> headers, List<String> values) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DATA");
            Row header = sheet.createRow(0);
            Row data = sheet.createRow(1);
            for (int index = 0; index < headers.size(); index++) {
                header.createCell(index).setCellValue(headers.get(index));
            }
            for (int index = 0; index < values.size(); index++) {
                data.createCell(index).setCellValue(values.get(index));
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
