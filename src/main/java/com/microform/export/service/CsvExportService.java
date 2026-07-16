package com.microform.export.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class CsvExportService {

    private static final List<String> BASE_HEADERS =
            List.of("id", "state", "submitter_id", "created_at");

    private final JdbcTemplate jdbc;
    private final FormVersionRepository formVersionRepository;
    private final ObjectMapper objectMapper;

    public CsvExportService(JdbcTemplate jdbc, FormVersionRepository formVersionRepository,
                            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.formVersionRepository = formVersionRepository;
        this.objectMapper = objectMapper;
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
                    // Expand data_json into one column per schema field (schema order).
                    Map<String, Object> data = parseData(rs.getString("data_json"));
                    for (FieldDefinition field : schema) {
                        row.add(formatValue(data.get(field.name())));
                    }
                    printer.printRecord(row);
                } catch (IOException e) {
                    throw new RuntimeException("CSV write failed", e);
                }
            });

            writer.flush();
        }
    }

    /** Base columns plus one column per schema field, in schema order. */
    private String[] buildHeaders(List<FieldDefinition> schema) {
        List<String> headers = new ArrayList<>(BASE_HEADERS);
        schema.forEach(f -> headers.add(f.name()));
        return headers.toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseData(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(dataJson, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    /** Scalars render as their string form; missing = empty; nested values as JSON. */
    private String formatValue(Object value) {
        if (value == null) return "";
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
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
