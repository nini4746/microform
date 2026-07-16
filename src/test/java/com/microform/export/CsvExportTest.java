package com.microform.export;

import com.microform.export.service.CsvExportService;
import com.microform.form.domain.*;
import com.microform.form.persistence.FormVersionRepository;
import com.microform.submission.domain.Submission;
import com.microform.submission.domain.SubmissionState;
import com.microform.submission.persistence.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

/**
 * Drives the real CsvExportService against an H2 DB. Verifies the export is
 * schema-typed: one column per form field, values expanded from data_json - not
 * a single raw-JSON blob column (the previously-disclosed limitation).
 */
@SpringBootTest
@ActiveProfiles("test")
class CsvExportTest {

    @Autowired private FormVersionRepository formVersions;
    @Autowired private SubmissionRepository submissions;
    @Autowired private CsvExportService csv;

    private static FieldDefinition field(String name) {
        return new FieldDefinition(name, FieldType.STRING, false, FieldConstraints.empty(), name, null);
    }

    @Test
    void csv_export_is_schema_typed_with_one_column_per_field() throws Exception {
        List<FieldDefinition> schema = List.of(field("email"), field("score"));
        WorkflowDefinition workflow = new WorkflowDefinition(List.of("DRAFT", "SUBMITTED"), List.of());
        formVersions.save(new FormVersion("csvform", 1, schema, workflow));
        submissions.save(new Submission("csvform", 1, SubmissionState.SUBMITTED, "u1",
                Map.of("email", "a@b.com", "score", 30)));
        submissions.save(new Submission("csvform", 1, SubmissionState.APPROVED, "u2",
                Map.of("email", "c@d.com", "score", 25)));

        var out = new ByteArrayOutputStream();
        csv.streamCsv(new ExportFilter("csvform", 1, null, null, null), out);
        String csvText = out.toString(StandardCharsets.UTF_8);
        String[] lines = csvText.strip().split("\r?\n");

        // Header carries the base columns plus one per schema field, in order.
        assertThat(lines[0]).isEqualTo("id,state,submitter_id,created_at,email,score");
        // Two data rows.
        assertThat(lines).hasSize(3);
        // Field values are expanded into their own columns...
        assertThat(csvText).contains("a@b.com").contains("c@d.com").contains("30").contains("25");
        // ...and the raw data_json blob is NOT dumped.
        assertThat(csvText).doesNotContain("data_json").doesNotContain("{\"email\"");
    }

    @Test
    void missing_field_becomes_an_empty_cell() throws Exception {
        List<FieldDefinition> schema = List.of(field("email"), field("phone"));
        WorkflowDefinition workflow = new WorkflowDefinition(List.of("DRAFT"), List.of());
        formVersions.save(new FormVersion("sparse", 1, schema, workflow));
        submissions.save(new Submission("sparse", 1, SubmissionState.SUBMITTED, "u1",
                Map.of("email", "only@email.com")));   // no phone

        var out = new ByteArrayOutputStream();
        csv.streamCsv(new ExportFilter("sparse", 1, null, null, null), out);
        String[] lines = out.toString(StandardCharsets.UTF_8).strip().split("\r?\n");
        assertThat(lines[0]).endsWith("email,phone");
        // row ends with the email then an empty phone cell
        assertThat(lines[1]).endsWith("only@email.com,");
    }

    @Test
    void unknown_form_is_rejected() {
        assertThatThrownBy(() -> csv.streamCsv(
                new ExportFilter("nope", 1, null, null, null), new ByteArrayOutputStream()))
                .isInstanceOf(NoSuchElementException.class);
    }
}
