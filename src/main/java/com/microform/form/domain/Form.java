package com.microform.form.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "forms")
public class Form {

    @Id
    @Column(name = "form_id", length = 100)
    private String formId;

    @Column(name = "latest_version", nullable = false)
    private int latestVersion = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Form() {}

    public Form(String formId) {
        this.formId = formId;
        this.latestVersion = 1;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void incrementVersion() {
        this.latestVersion++;
        this.updatedAt = Instant.now();
    }

    public String getFormId() { return formId; }
    public int getLatestVersion() { return latestVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
