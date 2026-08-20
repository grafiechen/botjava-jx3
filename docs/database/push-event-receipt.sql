CREATE TABLE IF NOT EXISTS push_event_receipt (
    id BIGSERIAL PRIMARY KEY,
    task_code VARCHAR(64) NOT NULL,
    event_fingerprint VARCHAR(64) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_push_event_receipt_task_fingerprint
        UNIQUE (task_code, event_fingerprint)
);

CREATE INDEX IF NOT EXISTS idx_push_event_receipt_create_time
    ON push_event_receipt (create_time);