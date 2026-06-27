package org.rrc.pollution_response_system.controller;

import org.rrc.pollution_response_system.service.ReportExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports/export")
@CrossOrigin(origins = "*")
public class ReportExportController {

    private final ReportExportService exportService;

    public ReportExportController(ReportExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf() throws Exception {
        byte[] pdf = exportService.exportCasesPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cases.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel() throws Exception {
        byte[] csv = exportService.exportCasesExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cases.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }
}
