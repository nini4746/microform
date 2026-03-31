package com.microform.common.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        UUID submissionId,
        AuditEventType type,
        String payloadJson,
        String actorId,
        Instant createdAt
) {}
