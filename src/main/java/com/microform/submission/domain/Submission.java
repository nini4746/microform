package com.microform.submission.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "submissions")
public class Submission {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "form_id", nullable = false)
    private String formId;

    @Column(name = "version", nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private SubmissionState state;

    @Column(name = "submitter_id", nullable = false)
    private String submitterId;

    @Column(name = "data_json", nullable = false, columnDefinition = "text")
    private String dataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Submission() {}

    public Submission(String formId, int version, SubmissionState state,
                      String submitterId, Map<String, Object> data) {
        this.formId = formId;
        this.version = version;
        this.state = state;
        this.submitterId = submitterId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        try {
            this.dataJson = MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize submission data", e);
        }
    }

    public Map<String, Object> getData() {
        try {
            return MAPPER.readValue(dataJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize submission data", e);
        }
    }

    public void transitionTo(SubmissionState newState) {
        this.state = newState;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getFormId() { return formId; }
    public int getVersion() { return version; }
    public SubmissionState getState() { return state; }
    public String getSubmitterId() { return submitterId; }
    public String getDataJson() { return dataJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
