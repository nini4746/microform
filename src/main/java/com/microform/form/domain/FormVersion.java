package com.microform.form.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "form_versions")
public class FormVersion {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @EmbeddedId
    private FormVersionId id;

    @Column(name = "schema_json", nullable = false, columnDefinition = "text")
    private String schemaJson;

    @Column(name = "workflow_json", nullable = false, columnDefinition = "text")
    private String workflowJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FormVersion() {}

    public FormVersion(String formId, int version, List<FieldDefinition> schema, WorkflowDefinition workflow) {
        this.id = new FormVersionId(formId, version);
        this.createdAt = Instant.now();
        try {
            this.schemaJson = MAPPER.writeValueAsString(schema);
            this.workflowJson = MAPPER.writeValueAsString(workflow);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize form version", e);
        }
    }

    public List<FieldDefinition> getSchema() {
        try {
            return MAPPER.readValue(schemaJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize schema", e);
        }
    }

    public WorkflowDefinition getWorkflow() {
        try {
            return MAPPER.readValue(workflowJson, WorkflowDefinition.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize workflow", e);
        }
    }

    public FormVersionId getId() { return id; }
    public String getFormId() { return id.getFormId(); }
    public int getVersion() { return id.getVersion(); }
    public String getSchemaJson() { return schemaJson; }
    public String getWorkflowJson() { return workflowJson; }
    public Instant getCreatedAt() { return createdAt; }
}
