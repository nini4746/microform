package com.microform.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class AuditLogRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AuditLogRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void append(UUID submissionId, AuditEventType type, Object payload, String actorId) {
        String payloadJson = null;
        if (payload != null) {
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize audit payload", e);
            }
        }
        jdbc.update(
                "INSERT INTO submission_events(submission_id, type, payload_json, actor_id) VALUES (?, ?, ?, ?)",
                submissionId, type.name(), payloadJson, actorId
        );
    }

    public List<AuditEvent> findBySubmissionId(UUID submissionId) {
        return jdbc.query(
                "SELECT id, submission_id, type, payload_json, actor_id, created_at " +
                "FROM submission_events WHERE submission_id = ? ORDER BY created_at ASC",
                (rs, rowNum) -> mapRow(rs),
                submissionId
        );
    }

    private AuditEvent mapRow(ResultSet rs) throws SQLException {
        return new AuditEvent(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("submission_id")),
                AuditEventType.valueOf(rs.getString("type")),
                rs.getString("payload_json"),
                rs.getString("actor_id"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
