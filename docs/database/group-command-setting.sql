CREATE TABLE IF NOT EXISTS group_command_setting (
    id BIGSERIAL PRIMARY KEY,
    group_open_id VARCHAR(128) NOT NULL,
    command_name VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_group_command_setting_group_command UNIQUE (group_open_id, command_name)
);

CREATE INDEX IF NOT EXISTS idx_group_command_setting_group_open_id
    ON group_command_setting (group_open_id);
