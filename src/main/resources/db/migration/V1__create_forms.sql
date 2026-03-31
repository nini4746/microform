CREATE TABLE forms (
    form_id        VARCHAR(100) PRIMARY KEY,
    latest_version INT          NOT NULL DEFAULT 1,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE form_versions (
    form_id       VARCHAR(100) NOT NULL REFERENCES forms(form_id),
    version       INT          NOT NULL,
    schema_json   JSONB        NOT NULL,
    workflow_json JSONB        NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (form_id, version)
);
