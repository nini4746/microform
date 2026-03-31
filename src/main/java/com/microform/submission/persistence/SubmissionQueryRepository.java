package com.microform.submission.persistence;

import com.microform.submission.domain.Submission;
import com.microform.submission.domain.SubmissionFilter;
import com.microform.submission.domain.SubmissionState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class SubmissionQueryRepository {

    private final JdbcTemplate jdbc;

    public SubmissionQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Submission> findByFilter(SubmissionFilter filter, int limit, int offset) {
        var sql = new StringBuilder(
                "SELECT id, form_id, version, state, submitter_id, data_json, created_at, updated_at " +
                "FROM submissions WHERE 1=1");
        var params = new ArrayList<>();

        appendFilter(sql, params, filter);
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbc.query(sql.toString(), params.toArray(), (rs, rowNum) -> mapRow(rs));
    }

    public long countByFilter(SubmissionFilter filter) {
        var sql = new StringBuilder("SELECT COUNT(*) FROM submissions WHERE 1=1");
        var params = new ArrayList<>();
        appendFilter(sql, params, filter);
        Long count = jdbc.queryForObject(sql.toString(), params.toArray(), Long.class);
        return count != null ? count : 0L;
    }

    private void appendFilter(StringBuilder sql, ArrayList<Object> params, SubmissionFilter filter) {
        if (filter.formId() != null) {
            sql.append(" AND form_id = ?");
            params.add(filter.formId());
        }
        if (filter.version() != null) {
            sql.append(" AND version = ?");
            params.add(filter.version());
        }
        if (filter.state() != null) {
            sql.append(" AND state = ?");
            params.add(filter.state().name());
        }
        if (filter.from() != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.from(filter.from()));
        }
        if (filter.to() != null) {
            sql.append(" AND created_at <= ?");
            params.add(Timestamp.from(filter.to()));
        }
        if (filter.submitterId() != null) {
            sql.append(" AND submitter_id = ?");
            params.add(filter.submitterId());
        }
        if (filter.q() != null && !filter.q().isBlank()) {
            sql.append(" AND data_json LIKE ?");
            params.add("%" + filter.q() + "%");
        }
    }

    private Submission mapRow(ResultSet rs) throws SQLException {
        // Use reflection-free approach: create via package-private constructor using raw fields
        // Since Submission has no default constructor accessible, we use JPA repository for actual mapping
        // and this class purely handles count/exists queries for large-scale filtering
        // For actual object mapping, delegate to SubmissionRepository.findById after getting IDs
        throw new UnsupportedOperationException("Use findIdsByFilter instead");
    }

    public List<UUID> findIdsByFilter(SubmissionFilter filter, int limit, int offset) {
        var sql = new StringBuilder("SELECT id FROM submissions WHERE 1=1");
        var params = new ArrayList<>();

        appendFilter(sql, params, filter);
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbc.query(sql.toString(), params.toArray(),
                (rs, rowNum) -> UUID.fromString(rs.getString("id")));
    }
}
