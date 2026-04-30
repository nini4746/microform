CREATE TABLE webhook_subscriptions (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(200) NOT NULL,
    url          VARCHAR(500) NOT NULL,
    secret       VARCHAR(200),
    event_types  VARCHAR(500) NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_subs_active ON webhook_subscriptions(active);

CREATE TABLE webhook_deliveries (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID         NOT NULL REFERENCES webhook_subscriptions(id),
    event_type      VARCHAR(50)  NOT NULL,
    payload_json    TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    max_attempts    INT          NOT NULL DEFAULT 5,
    next_attempt_at TIMESTAMPTZ,
    last_error      VARCHAR(1000),
    response_status INT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_deliveries_status ON webhook_deliveries(status);
CREATE INDEX idx_webhook_deliveries_next_attempt ON webhook_deliveries(next_attempt_at);
CREATE INDEX idx_webhook_deliveries_sub ON webhook_deliveries(subscription_id);
