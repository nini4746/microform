CREATE TABLE submissions (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id      VARCHAR(100) NOT NULL,
    version      INT          NOT NULL,
    state        VARCHAR(50)  NOT NULL,
    submitter_id VARCHAR(100) NOT NULL,
    data_json    TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    FOREIGN KEY (form_id, version) REFERENCES form_versions(form_id, version)
);

CREATE INDEX idx_submissions_form_version  ON submissions(form_id, version);
CREATE INDEX idx_submissions_state         ON submissions(state);
CREATE INDEX idx_submissions_created_at    ON submissions(created_at);
CREATE INDEX idx_submissions_submitter     ON submissions(submitter_id);

CREATE TABLE submission_events (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID        NOT NULL REFERENCES submissions(id),
    type          VARCHAR(50) NOT NULL,
    payload_json  TEXT,
    actor_id      VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_submission_events_sub_id ON submission_events(submission_id);
