package com.microform.webhook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "webhook_subscriptions")
public class WebhookSubscription {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "secret", length = 200)
    private String secret;

    @Column(name = "event_types", nullable = false, length = 500)
    private String eventTypesRaw;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WebhookSubscription() {
    }

    public WebhookSubscription(UUID id, String name, String url, String secret,
                               Set<WebhookEventType> eventTypes, boolean active,
                               Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.secret = secret;
        setEventTypes(eventTypes);
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getEventTypesRaw() { return eventTypesRaw; }
    public void setEventTypesRaw(String s) { this.eventTypesRaw = s; }

    public Set<WebhookEventType> getEventTypes() {
        Set<WebhookEventType> out = new LinkedHashSet<>();
        if (eventTypesRaw == null || eventTypesRaw.isBlank()) return out;
        for (String t : eventTypesRaw.split(",")) {
            String s = t.trim();
            if (!s.isEmpty()) out.add(WebhookEventType.valueOf(s));
        }
        return out;
    }

    public void setEventTypes(Set<WebhookEventType> types) {
        if (types == null || types.isEmpty()) {
            this.eventTypesRaw = "";
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (WebhookEventType t : types) {
            if (sb.length() > 0) sb.append(',');
            sb.append(t.name());
        }
        this.eventTypesRaw = sb.toString();
    }

    public boolean handles(WebhookEventType type) {
        return active && getEventTypes().contains(type);
    }
}
