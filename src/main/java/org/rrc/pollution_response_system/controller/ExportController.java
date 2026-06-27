package org.rrc.pollution_response_system.controller;

import org.rrc.pollution_response_system.service.ReportExportService;
import org.rrc.pollution_response_system.service.ReportExportService.LocalDateTimeRange;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ReportExportService exportService;
    public ExportController(ReportExportService exportService){ this.exportService = exportService; }

    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping(value="/reports.pdf", produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@RequestParam(name="days", defaultValue="7") int days) throws Exception {
        var range = LocalDateTimeRange.lastDays(days);
        byte[] bytes = exportService.exportPdf(range);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=case-summary.pdf")
                .body(bytes);
    }

    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_AUTHORITY','ADMIN')")
    @GetMapping(value="/reports.xlsx", produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> xlsx(@RequestParam(name="days", defaultValue="7") int days) throws Exception {
        var range = LocalDateTimeRange.lastDays(days);
        byte[] bytes = exportService.exportXlsx(range);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=case-summary.xlsx")
                .body(bytes);
    }
}
