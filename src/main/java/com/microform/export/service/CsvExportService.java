package com.microform.export.service;

import com.microform.export.ExportFilter;
import com.microform.form.domain.FieldDefinition;
import com.microform.form.persistence.FormVersionRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CsvExportService {

    private final JdbcTemplate jdbc;
    private final FormVersionRepository formVersionRepository;

    public CsvExportService(JdbcTemplate jdbc, FormVersionRepository formVersionRepository) {
        this.jdbc = jdbc;
        this.formVersionRepository = formVersionRepository;
    }

    public void streamCsv(ExportFilter filter, OutputStream outputStream) throws IOException {
        var formVersion = formVersionRepository
                .findByIdFormIdAndIdVersion(filter.formId(), filter.version())
                .orElseThrow(() -> new NoSuchElementException(
                        "Form not found: " + filter.formId() + "/v" + filter.version()));

        List<FieldDefinition> schema = formVersion.getSchema();
        String[] headers = buildHeaders(schema);

        try (var writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             var printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers).build())) {

            String sql = buildSql(filter);
            Object[] params = buildParams(filter);

            jdbc.query(con -> {
                PreparedStatement ps = con.prepareStatement(sql,
                        java.sql.ResultSet.TYPE_FORWARD_ONLY,
                        java.sql.ResultSet.CONCUR_READ_ONLY);
                ps.setFetchSize(500);
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                return ps;
            }, rs -> {
                try {
                    List<Object> row = new ArrayList<>();
                    row.add(rs.getString("id"));
                    row.add(rs.getString("state"));
                    row.add(rs.getString("submitter_id"));
                    row.add(rs.getTimestamp("created_at").toInstant().toString());
                    // data_json fields
                    String dataJson = rs.getString("data_json");
                    row.add(dataJson);
                    printer.printRecord(row);
                } catch (IOException e) {
                    throw new RuntimeException("CSV write failed", e);
                }
            });

            writer.flush();
        }
    }

    private String[] buildHeaders(List<FieldDefinition> schema) {
        List<String> headers = new ArrayList<>(List.of("id", "state", "submitter_id", "created_at"));
        schema.forEach(f -> headers.add(f.name()));
        // Simplified: export data_json as raw JSON column
        // In production: parse each field from data_json per FieldDefinition
        return new String[]{"id", "state", "submitter_id", "created_at", "data_json"};
    }

    private String buildSql(ExportFilter filter) {
        var sql = new StringBuilder(
                "SELECT id, state, submitter_id, created_at, data_json " +
                "FROM submissions WHERE form_id = ? AND version = ?");
        if (filter.state() != null) sql.append(" AND state = ?");
        if (filter.from() != null) sql.append(" AND created_at >= ?");
        if (filter.to() != null) sql.append(" AND created_at <= ?");
        sql.append(" ORDER BY created_at ASC");
        return sql.toString();
    }

    private Object[] buildParams(ExportFilter filter) {
        var params = new ArrayList<>();
        params.add(filter.formId());
        params.add(filter.version());
        if (filter.state() != null) params.add(filter.state().name());
        if (filter.from() != null) params.add(Timestamp.from(filter.from()));
        if (filter.to() != null) params.add(Timestamp.from(filter.to()));
        return params.toArray();
    }
}
