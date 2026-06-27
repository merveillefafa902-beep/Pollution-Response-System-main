package org.rrc.pollution_response_system.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rrc.pollution_response_system.entity.PollutionCase;
import org.rrc.pollution_response_system.repository.PollutionCaseRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportExportService {

    private final PollutionCaseRepository reportRepository;

    public ReportExportService(PollutionCaseRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /**
     * Legacy full export (no date filter) retained for backward compatibility.
     */
    public byte[] exportCasesPdf() throws JRException {
        return exportPdf(LocalDateTimeRange.lastDays(365));
    }

    /**
     * Legacy CSV-style Excel export retained.
     */
    public byte[] exportCasesExcel() throws Exception {
        return exportXlsx(LocalDateTimeRange.lastDays(365));
    }

    /**
     * Export a filtered range of cases to PDF using JasperReports template case_summary.jrxml.
     */
    public byte[] exportPdf(LocalDateTimeRange range) throws JRException {
        List<PollutionCase> all = reportRepository.findAll();
        List<PollutionCase> filtered = all.stream()
                .filter(r -> r.getReportedAt() != null && range.inRange(r.getReportedAt()))
                .collect(Collectors.toList());

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(filtered);
        Map<String, Object> params = new HashMap<>();
        params.put("REPORT_TITLE", "Pollution Case Summary");
        params.put("GENERATED_AT", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(LocalDateTime.now()));
        params.put("RANGE", range.label());  // Must match the parameter name in .jrxml

        InputStream templateStream = getClass().getResourceAsStream("/reports/case_summary.jrxml");
        JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    /**
     * Export a filtered range of cases to XLSX using Apache POI for richer formatting.
     */
    public byte[] exportXlsx(LocalDateTimeRange range) throws Exception {
        List<PollutionCase> all = reportRepository.findAll();
        List<PollutionCase> filtered = all.stream()
                .filter(r -> r.getReportedAt() != null && range.inRange(r.getReportedAt()))
                .collect(Collectors.toList());

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Cases");
            int rowIdx = 0;
            Row header = sheet.createRow(rowIdx++);
            String[] cols = {"ID", "Type", "Severity", "status", "Region", "Latitude", "Longitude", "ReportedAt"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (PollutionCase r : filtered) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getId());
                row.createCell(1).setCellValue(safe(r.getPollutionCategory()));
                row.createCell(2).setCellValue(r.getSeverity() != null ? r.getSeverity().name() : "");
                row.createCell(3).setCellValue(r.getInvestigationStatus() != null ? r.getInvestigationStatus().name() : "");
                row.createCell(4).setCellValue(r.getRegion() != null ? r.getRegion().getName() : "");
                row.createCell(5).setCellValue(r.getLatitude() != null ? r.getLatitude() : 0);
                row.createCell(6).setCellValue(r.getLongitude() != null ? r.getLongitude() : 0);
                row.createCell(7).setCellValue(r.getReportedAt() != null ? fmt.format(r.getReportedAt()) : "");
            }
            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private String safe(String v) { return v == null ? "" : v.replaceAll(",", " "); }

    /**
     * Date-time range helper used by ExportController for filtering.
     */
    public static class LocalDateTimeRange {
        private final LocalDateTime from;
        private final LocalDateTime to;

        public LocalDateTimeRange(LocalDateTime from, LocalDateTime to) {
            this.from = from;
            this.to = to;
        }

        public boolean inRange(LocalDateTime t) {
            return t != null && !t.isBefore(from) && !t.isAfter(to);
        }

        public String label() {
            return from.toLocalDate() + " to " + to.toLocalDate();
        }

        public static LocalDateTimeRange lastDays(int days) {
            LocalDateTime to = LocalDateTime.now();
            LocalDateTime from = to.minusDays(days);
            return new LocalDateTimeRange(from, to);
        }
    }
}
