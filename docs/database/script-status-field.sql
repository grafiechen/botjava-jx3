CREATE TABLE IF NOT EXISTS script_status_field (
    id BIGSERIAL PRIMARY KEY,
    mongo_field_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    group_name VARCHAR(100) NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    writable BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    updated_by VARCHAR(128),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_script_status_field_mongo_name UNIQUE (mongo_field_name)
);