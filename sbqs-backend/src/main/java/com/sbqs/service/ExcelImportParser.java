package com.sbqs.service;

import com.sbqs.dto.bulkimport.ServiceImportRow;
import com.sbqs.dto.bulkimport.ServiceCatalogImportRow;
import com.sbqs.dto.bulkimport.StaffImportRow;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelImportParser {
    private static final int MAX_DATA_ROWS = 500;

    public List<StaffImportRow> parseStaff(InputStream inputStream) {
        return readRows(inputStream, (row, formatter) -> new StaffImportRow(
                row.getRowNum() + 1,
                cell(row, 0, formatter),
                cell(row, 1, formatter),
                cell(row, 2, formatter),
                cell(row, 3, formatter)));
    }

    public List<ServiceImportRow> parseServices(InputStream inputStream) {
        return readRows(inputStream, (row, formatter) -> new ServiceImportRow(
                row.getRowNum() + 1,
                cell(row, 0, formatter),
                cell(row, 1, formatter),
                cell(row, 2, formatter),
                cell(row, 3, formatter),
                cell(row, 4, formatter),
                cell(row, 5, formatter)));
    }

    public List<ServiceCatalogImportRow> parseServiceCatalog(InputStream inputStream) {
        return readRows(inputStream, (row, formatter) -> new ServiceCatalogImportRow(
                row.getRowNum() + 1,
                cell(row, 0, formatter),
                cell(row, 1, formatter),
                cell(row, 2, formatter),
                cell(row, 3, formatter),
                cell(row, 4, formatter),
                cell(row, 5, formatter)));
    }

    public byte[] createStaffTemplate() {
        return createTemplate(
                "NHAN_VIEN",
                List.of("Họ tên", "Email", "Số điện thoại", "Mật khẩu"),
                new int[] { 28, 32, 18, 24 });
    }

    public byte[] createServiceTemplate() {
        return createTemplate(
                "DICH_VU",
                List.of("Mã dịch vụ", "Tên dịch vụ", "Loại dịch vụ", "Mô tả", "Thời gian (phút)", "Trạng thái"),
                new int[] { 18, 28, 18, 38, 20, 18 });
    }

    public byte[] createServiceCatalogTemplate() {
        return createTemplate(
                "DANH_MUC_DICH_VU",
                List.of("Mã dịch vụ", "Tên dịch vụ", "Nhóm dịch vụ", "Mô tả",
                        "Thời gian (phút)", "Cho phép ủy quyền (CÓ/KHÔNG)"),
                new int[] { 22, 32, 22, 42, 20, 30 });
    }

    private <T> List<T> readRows(InputStream inputStream, RowMapper<T> mapper) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new RuntimeException("File Excel không có trang dữ liệu");
            }

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<T> rows = new ArrayList<>();

            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || isBlank(row, formatter)) {
                    continue;
                }
                if (rows.size() >= MAX_DATA_ROWS) {
                    throw new RuntimeException("Mỗi lần chỉ được nhập tối đa 500 dòng dữ liệu");
                }
                rows.add(mapper.map(row, formatter));
            }

            if (rows.isEmpty()) {
                throw new RuntimeException("File Excel chưa có dữ liệu để nhập");
            }
            return rows;
        } catch (IOException | IllegalArgumentException ex) {
            throw new RuntimeException("Không đọc được file Excel. Vui lòng sử dụng đúng file mẫu .xlsx", ex);
        }
    }

    private byte[] createTemplate(String sheetName, List<String> headers, int[] widths) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            sheet.createFreezePane(0, 1);

            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));

            Row header = sheet.createRow(0);
            for (int index = 0; index < headers.size(); index++) {
                header.createCell(index).setCellValue(headers.get(index));
                header.getCell(index).setCellStyle(headerStyle);
                sheet.setColumnWidth(index, widths[index] * 256);
            }

            if ("NHAN_VIEN".equals(sheetName)) {
                sheet.setDefaultColumnStyle(1, textStyle);
                sheet.setDefaultColumnStyle(2, textStyle);
                sheet.setDefaultColumnStyle(3, textStyle);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Không tạo được file Excel mẫu", ex);
        }
    }

    private boolean isBlank(Row row, DataFormatter formatter) {
        for (int index = 0; index < row.getLastCellNum(); index++) {
            if (!cell(row, index, formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String cell(Row row, int index, DataFormatter formatter) {
        if (row.getCell(index) == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(index)).trim();
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(Row row, DataFormatter formatter);
    }
}
