package com.microform.webhook.persistence;

import com.microform.webhook.domain.DeliveryStatus;
import com.microform.webhook.domain.WebhookDelivery;
import com.microform.webhook.domain.WebhookEventType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class WebhookDeliveryRepository {

    private final JdbcTemplate jdbc;

    public WebhookDeliveryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public WebhookDelivery insert(WebhookDelivery d) {
        if (d.getId() == null) d.setId(UUID.randomUUID());
        Instant now = Instant.now();
        if (d.getCreatedAt() == null) d.setCreatedAt(now);
        d.setUpdatedAt(now);
        jdbc.update(
                "INSERT INTO webhook_deliveries(id, subscription_id, event_type, payload_json, status, " +
                        "attempts, max_attempts, next_attempt_at, last_error, response_status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                d.getId(), d.getSubscriptionId(), d.getEventType().name(), d.getPayloadJson(),
                d.getStatus().name(), d.getAttempts(), d.getMaxAttempts(),
                d.getNextAttemptAt() != null ? Timestamp.from(d.getNextAttemptAt()) : null,
                d.getLastError(), d.getResponseStatus(),
                Timestamp.from(d.getCreatedAt()), Timestamp.from(d.getUpdatedAt())
        );
        return d;
    }

    public void update(WebhookDelivery d) {
        d.setUpdatedAt(Instant.now());
        jdbc.update(
                "UPDATE webhook_deliveries SET status=?, attempts=?, next_attempt_at=?, last_error=?, response_status=?, updated_at=? WHERE id=?",
                d.getStatus().name(), d.getAttempts(),
                d.getNextAttemptAt() != null ? Timestamp.from(d.getNextAttemptAt()) : null,
                d.getLastError(), d.getResponseStatus(),
                Timestamp.from(d.getUpdatedAt()), d.getId()
        );
    }

    public Optional<WebhookDelivery> findById(UUID id) {
        var rows = jdbc.query(baseSelect() + " WHERE id=?", (rs, rn) -> mapRow(rs), id);
        return rows.stream().findFirst();
    }

    public List<WebhookDelivery> findDuePending(Instant now, int limit) {
        return jdbc.query(
                baseSelect() + " WHERE status=? AND (next_attempt_at IS NULL OR next_attempt_at <= ?) " +
                        "ORDER BY next_attempt_at ASC LIMIT ?",
                (rs, rn) -> mapRow(rs),
                DeliveryStatus.PENDING.name(), Timestamp.from(now), limit
        );
    }

    public List<WebhookDelivery> findBySubscription(UUID subscriptionId) {
        return jdbc.query(baseSelect() + " WHERE subscription_id=? ORDER BY created_at DESC",
                (rs, rn) -> mapRow(rs), subscriptionId);
    }

    public long countByStatus(DeliveryStatus status) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM webhook_deliveries WHERE status=?", Long.class, status.name());
        return n == null ? 0 : n;
    }

    private String baseSelect() {
        return "SELECT id, subscription_id, event_type, payload_json, status, attempts, max_attempts, " +
                "next_attempt_at, last_error, response_status, created_at, updated_at FROM webhook_deliveries";
    }

    private WebhookDelivery mapRow(ResultSet rs) throws SQLException {
        WebhookDelivery d = new WebhookDelivery();
        d.setId(UUID.fromString(rs.getString("id")));
        d.setSubscriptionId(UUID.fromString(rs.getString("subscription_id")));
        d.setEventType(WebhookEventType.valueOf(rs.getString("event_type")));
        d.setPayloadJson(rs.getString("payload_json"));
        d.setStatus(DeliveryStatus.valueOf(rs.getString("status")));
        d.setAttempts(rs.getInt("attempts"));
        d.setMaxAttempts(rs.getInt("max_attempts"));
        Timestamp nat = rs.getTimestamp("next_attempt_at");
        d.setNextAttemptAt(nat == null ? null : nat.toInstant());
        d.setLastError(rs.getString("last_error"));
        Object respStatus = rs.getObject("response_status");
        d.setResponseStatus(respStatus == null ? null : ((Number) respStatus).intValue());
        d.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        d.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return d;
    }
}
