CREATE TABLE IF NOT EXISTS group_daily_push_field (
    id BIGSERIAL PRIMARY KEY,
    group_open_id VARCHAR(128) NOT NULL,
    mongo_field_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 100,
    updated_by VARCHAR(128),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_group_daily_push_field UNIQUE (group_open_id, mongo_field_name),
    CONSTRAINT ck_group_daily_push_field_type
        CHECK (value_type IN ('TEXT', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATETIME'))
);

CREATE INDEX IF NOT EXISTS idx_group_daily_push_field_enabled
    ON group_daily_push_field (group_open_id, enabled, sort_order);