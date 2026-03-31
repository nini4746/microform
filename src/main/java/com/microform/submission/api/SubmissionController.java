package com.microform.submission.api;

import com.microform.common.pagination.PageRequest;
import com.microform.common.pagination.PageResponse;
import com.microform.submission.domain.SubmissionFilter;
import com.microform.submission.domain.SubmissionState;
import com.microform.submission.service.SubmissionQueryService;
import com.microform.submission.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SubmissionQueryService queryService;

    public SubmissionController(SubmissionService submissionService, SubmissionQueryService queryService) {
        this.submissionService = submissionService;
        this.queryService = queryService;
    }

    @PostMapping("/forms/{formId}/versions/{version}/submissions")
    public ResponseEntity<SubmissionResponse> createSubmission(
            @PathVariable String formId,
            @PathVariable int version,
            @Valid @RequestBody CreateSubmissionRequest request,
            Authentication auth) {
        var submission = submissionService.createSubmission(
                formId, version, request.data(), request.initialState(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(SubmissionResponse.from(submission));
    }

    @GetMapping("/submissions")
    public ResponseEntity<PageResponse<SubmissionResponse>> listSubmissions(
            @RequestParam(required = false) String formId,
            @RequestParam(required = false) Integer version,
            @RequestParam(required = false) SubmissionState state,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String submitterId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        var filter = new SubmissionFilter(formId, version, state, from, to, q, submitterId);
        var pageRequest = PageRequest.of(page, size);
        return ResponseEntity.ok(queryService.list(filter, pageRequest));
    }

    @GetMapping("/submissions/{id}")
    public ResponseEntity<SubmissionDetailResponse> getSubmission(@PathVariable UUID id) {
        var submission = submissionService.getSubmission(id);
        var events = submissionService.getAuditLog(id);
        return ResponseEntity.ok(SubmissionDetailResponse.from(submission, events));
    }

    @PostMapping("/submissions/{id}/transition")
    public ResponseEntity<SubmissionResponse> transition(
            @PathVariable UUID id,
            @Valid @RequestBody TransitionRequest request,
            Authentication auth) {
        String role = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse("");
        var submission = submissionService.transition(id, request.to(), role, auth.getName());
        return ResponseEntity.ok(SubmissionResponse.from(submission));
    }
}
