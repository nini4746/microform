package com.microform.webhook.persistence;

import com.microform.webhook.domain.WebhookEventType;
import com.microform.webhook.domain.WebhookSubscription;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class WebhookSubscriptionRepository {

    private final JdbcTemplate jdbc;

    public WebhookSubscriptionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public WebhookSubscription save(WebhookSubscription sub) {
        if (sub.getId() == null) {
            sub.setId(UUID.randomUUID());
            sub.setCreatedAt(Instant.now());
            sub.setUpdatedAt(sub.getCreatedAt());
            jdbc.update(
                    "INSERT INTO webhook_subscriptions(id, name, url, secret, event_types, active, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    sub.getId(), sub.getName(), sub.getUrl(), sub.getSecret(),
                    sub.getEventTypesRaw(), sub.isActive(),
                    java.sql.Timestamp.from(sub.getCreatedAt()),
                    java.sql.Timestamp.from(sub.getUpdatedAt())
            );
        } else {
            sub.setUpdatedAt(Instant.now());
            jdbc.update(
                    "UPDATE webhook_subscriptions SET name=?, url=?, secret=?, event_types=?, active=?, updated_at=? WHERE id=?",
                    sub.getName(), sub.getUrl(), sub.getSecret(),
                    sub.getEventTypesRaw(), sub.isActive(),
                    java.sql.Timestamp.from(sub.getUpdatedAt()), sub.getId()
            );
        }
        return sub;
    }

    public Optional<WebhookSubscription> findById(UUID id) {
        var rows = jdbc.query(
                "SELECT id, name, url, secret, event_types, active, created_at, updated_at FROM webhook_subscriptions WHERE id=?",
                (rs, rn) -> mapRow(rs), id);
        return rows.stream().findFirst();
    }

    public List<WebhookSubscription> findAll() {
        return jdbc.query(
                "SELECT id, name, url, secret, event_types, active, created_at, updated_at FROM webhook_subscriptions ORDER BY created_at ASC",
                (rs, rn) -> mapRow(rs));
    }

    public List<WebhookSubscription> findActiveForEvent(WebhookEventType type) {
        return jdbc.query(
                "SELECT id, name, url, secret, event_types, active, created_at, updated_at FROM webhook_subscriptions " +
                        "WHERE active = TRUE AND event_types LIKE ?",
                (rs, rn) -> mapRow(rs),
                "%" + type.name() + "%"
        ).stream().filter(s -> s.handles(type)).toList();
    }

    public void delete(UUID id) {
        jdbc.update("DELETE FROM webhook_subscriptions WHERE id=?", id);
    }

    private WebhookSubscription mapRow(ResultSet rs) throws SQLException {
        WebhookSubscription s = new WebhookSubscription();
        s.setId(UUID.fromString(rs.getString("id")));
        s.setName(rs.getString("name"));
        s.setUrl(rs.getString("url"));
        s.setSecret(rs.getString("secret"));
        s.setEventTypesRaw(rs.getString("event_types"));
        s.setActive(rs.getBoolean("active"));
        s.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        s.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return s;
    }
}
