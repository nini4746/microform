package com.microform.export;

import com.microform.export.service.CsvExportService;
import com.microform.form.domain.*;
import com.microform.form.persistence.FormVersionRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Streaming/OOM guard for CSV export (spec/README M6). Runs against a real
 * PostgreSQL (server-side cursor is a Postgres feature H2 cannot emulate),
 * inserts many fat rows, and streams the whole export to a byte-counting sink
 * that never retains the payload. With CsvExportService using a forward-only
 * cursor at fetchSize=500 this completes with bounded memory; a buffering
 * regression blows the heap the csvStreamingTest task caps at -Xmx128m.
 *
 * The local Docker Engine here is too new for the bundled docker-java, so the
 * test targets an externally-started Postgres via {@code mf.test.pg.url}
 * (started manually) and is skipped when that property is absent. Run with
 * {@code ./gradlew csvStreamingTest -Dmf.test.pg.url=...}.
 */
@Tag("streaming")
@EnabledIfSystemProperty(named = "mf.test.pg.url", matches = ".+")
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CsvStreamingIT {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getProperty("mf.test.pg.url"));
        registry.add("spring.datasource.username", () -> System.getProperty("mf.test.pg.user", "microform"));
        registry.add("spring.datasource.password", () -> System.getProperty("mf.test.pg.pass", "microform"));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired private FormVersionRepository formVersions;
    @Autowired private CsvExportService csv;
    @Autowired private JdbcTemplate jdbc;

    private static final int ROWS = 150_000;

    @Test
    void streams_150k_fat_rows_without_buffering() throws Exception {
        jdbc.update("DELETE FROM submissions WHERE form_id = 'big'");
        List<FieldDefinition> schema = List.of(
                new FieldDefinition("email", FieldType.STRING, false, FieldConstraints.empty(), "email", null),
                new FieldDefinition("blob", FieldType.STRING, false, FieldConstraints.empty(), "blob", null));
        formVersions.save(new FormVersion("big", 1, schema,
                new WorkflowDefinition(List.of("DRAFT"), List.of())));

        // Insert entirely server-side (generate_series) so test-JVM setup memory
        // stays ~0. ~1 KB/row * 150k => ~150 MB, well above the -Xmx128m cap: if
        // the export buffered the result set instead of streaming, it would OOM.
        jdbc.update(
                "INSERT INTO submissions (id, form_id, version, state, submitter_id, data_json, created_at, updated_at) " +
                        "SELECT gen_random_uuid(), 'big', 1, 'SUBMITTED', 'u'||g, " +
                        "'{\"email\":\"u'||g||'@x.com\",\"blob\":\"'||repeat('x',900)||'\"}', now(), now() " +
                        "FROM generate_series(1, ?) g", ROWS);

        AtomicLong bytes = new AtomicLong();
        AtomicLong newlines = new AtomicLong();
        OutputStream sink = new OutputStream() {
            @Override public void write(int b) {
                bytes.incrementAndGet();
                if (b == '\n') newlines.incrementAndGet();
            }
            @Override public void write(byte[] b, int off, int len) {
                bytes.addAndGet(len);
                for (int i = off; i < off + len; i++) if (b[i] == '\n') newlines.incrementAndGet();
            }
        };

        csv.streamCsv(new ExportFilter("big", 1, null, null, null), sink);

        // header + ROWS data rows, and a payload far bigger than the heap cap.
        assertThat(newlines.get()).isEqualTo(ROWS + 1L);
        assertThat(bytes.get()).isGreaterThan(140_000_000L);
    }
}
