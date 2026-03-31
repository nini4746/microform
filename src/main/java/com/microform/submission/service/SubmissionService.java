package com.microform.submission.service;

import com.microform.common.audit.AuditEvent;
import com.microform.common.audit.AuditEventType;
import com.microform.common.audit.AuditLogRepository;
import com.microform.form.persistence.FormVersionRepository;
import com.microform.submission.domain.Submission;
import com.microform.submission.domain.SubmissionState;
import com.microform.submission.persistence.SubmissionRepository;
import com.microform.validation.NormalizationService;
import com.microform.validation.SchemaValidatorService;
import com.microform.validation.ValidationResult;
import com.microform.workflow.WorkflowEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class SubmissionService {

    private final FormVersionRepository formVersionRepository;
    private final SubmissionRepository submissionRepository;
    private final SchemaValidatorService validatorService;
    private final NormalizationService normalizationService;
    private final AuditLogRepository auditLogRepository;
    private final WorkflowEngine workflowEngine;

    public SubmissionService(FormVersionRepository formVersionRepository,
                             SubmissionRepository submissionRepository,
                             SchemaValidatorService validatorService,
                             NormalizationService normalizationService,
                             AuditLogRepository auditLogRepository,
                             WorkflowEngine workflowEngine) {
        this.formVersionRepository = formVersionRepository;
        this.submissionRepository = submissionRepository;
        this.validatorService = validatorService;
        this.normalizationService = normalizationService;
        this.auditLogRepository = auditLogRepository;
        this.workflowEngine = workflowEngine;
    }

    public Submission createSubmission(String formId, int version,
                                       Map<String, Object> data,
                                       SubmissionState initialState,
                                       String submitterId) {
        var formVersion = formVersionRepository.findByIdFormIdAndIdVersion(formId, version)
                .orElseThrow(() -> new NoSuchElementException("Form not found: " + formId + "/v" + version));

        var schema = formVersion.getSchema();
        ValidationResult result = validatorService.validate(schema, data);
        if (!result.valid()) {
            var messages = result.errors().stream()
                    .map(e -> e.field() + ": " + e.message())
                    .toList();
            throw new ValidationException(messages);
        }

        normalizationService.normalize(schema, data);

        SubmissionState state = (initialState != null) ? initialState : SubmissionState.SUBMITTED;
        Submission submission = new Submission(formId, version, state, submitterId, data);
        submissionRepository.save(submission);

        auditLogRepository.append(submission.getId(), AuditEventType.CREATED, data, submitterId);
        return submission;
    }

    public Submission transition(UUID submissionId, String targetStateName, String actorRole, String actorId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NoSuchElementException("Submission not found: " + submissionId));

        var formVersion = formVersionRepository
                .findByIdFormIdAndIdVersion(submission.getFormId(), submission.getVersion())
                .orElseThrow(() -> new NoSuchElementException("Form version not found"));

        var workflow = formVersion.getWorkflow();
        String currentState = submission.getState().name();

        workflowEngine.assertTransitionAllowed(workflow, currentState, targetStateName, actorRole);

        SubmissionState newState = SubmissionState.valueOf(targetStateName);
        submission.transitionTo(newState);
        submissionRepository.save(submission);

        auditLogRepository.append(submissionId, AuditEventType.STATE_CHANGED,
                Map.of("from", currentState, "to", targetStateName), actorId);
        return submission;
    }

    @Transactional(readOnly = true)
    public Submission getSubmission(UUID submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NoSuchElementException("Submission not found: " + submissionId));
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getAuditLog(UUID submissionId) {
        return auditLogRepository.findBySubmissionId(submissionId);
    }
}
