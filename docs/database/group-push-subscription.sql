CREATE TABLE IF NOT EXISTS group_push_subscription (
    id BIGSERIAL PRIMARY KEY,
    group_open_id VARCHAR(128) NOT NULL,
    task_code VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by VARCHAR(128),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_group_push_subscription_group_task
        UNIQUE (group_open_id, task_code)
);

CREATE INDEX IF NOT EXISTS idx_group_push_subscription_task_enabled
    ON group_push_subscription (task_code, enabled);
