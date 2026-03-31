package com.microform.export.api;

import com.microform.export.ExportFilter;
import com.microform.export.service.CsvExportService;
import com.microform.submission.domain.SubmissionState;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Instant;

@RestController
@RequestMapping("/exports")
public class ExportController {

    private final CsvExportService csvExportService;

    public ExportController(CsvExportService csvExportService) {
        this.csvExportService = csvExportService;
    }

    @GetMapping(value = "/forms/{formId}/versions/{version}.csv", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportCsv(
            @PathVariable String formId,
            @PathVariable int version,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) SubmissionState state) {

        var filter = new ExportFilter(formId, version, from, to, state);
        StreamingResponseBody body = outputStream -> csvExportService.streamCsv(filter, outputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + formId + "-v" + version + ".csv\"")
                .body(body);
    }
}
