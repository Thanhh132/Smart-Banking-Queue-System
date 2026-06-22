package com.sbqs.service;

import com.sbqs.dto.report.ReportDocument;
import com.sbqs.dto.report.ReportFormat;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JasperReportService {
    private final Map<String, JasperReport> compiledReports = new ConcurrentHashMap<>();

    public ReportDocument export(
            String templateName,
            String fileNamePrefix,
            Map<String, Object> parameters,
            Collection<?> rows,
            ReportFormat format) {

        try {
            JasperReport report = compiledReports.computeIfAbsent(templateName, this::compileReport);
            JasperPrint print = JasperFillManager.fillReport(
                    report,
                    new HashMap<>(parameters),
                    rows.isEmpty() ? new JREmptyDataSource() : new JRBeanCollectionDataSource(rows));

            byte[] content = format == ReportFormat.PDF
                    ? JasperExportManager.exportReportToPdf(print)
                    : exportXlsx(print);
            return new ReportDocument(
                    content,
                    format.getContentType(),
                    fileNamePrefix + "." + format.getExtension());
        } catch (JRException ex) {
            throw new RuntimeException("Khong tao duoc report Jasper", ex);
        }
    }

    private JasperReport compileReport(String templateName) {
        String path = "/reports/" + templateName + ".jrxml";
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new RuntimeException("Khong tim thay template report: " + path);
            }
            return JasperCompileManager.compileReport(inputStream);
        } catch (Exception ex) {
            throw new RuntimeException("Khong compile duoc template report: " + templateName, ex);
        }
    }

    private byte[] exportXlsx(JasperPrint print) throws JRException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

            SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
            configuration.setOnePagePerSheet(false);
            configuration.setRemoveEmptySpaceBetweenRows(true);
            configuration.setRemoveEmptySpaceBetweenColumns(true);
            configuration.setDetectCellType(true);
            configuration.setCollapseRowSpan(false);
            configuration.setWhitePageBackground(false);
            exporter.setConfiguration(configuration);
            exporter.exportReport();
            return outputStream.toByteArray();
        } catch (java.io.IOException ex) {
            throw new RuntimeException("Khong dong duoc file XLSX", ex);
        }
    }
}
