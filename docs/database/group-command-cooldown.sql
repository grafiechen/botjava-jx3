CREATE TABLE IF NOT EXISTS group_command_cooldown (
    id BIGSERIAL PRIMARY KEY,
    group_open_id VARCHAR(128) NOT NULL,
    command_name VARCHAR(64) NOT NULL,
    reservation_id VARCHAR(36),
    reserved_at TIMESTAMP WITH TIME ZONE,
    lease_expires_at TIMESTAMP WITH TIME ZONE,
    available_at TIMESTAMP WITH TIME ZONE,
    create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_group_command_cooldown_group_command UNIQUE (group_open_id, command_name)
);

CREATE INDEX IF NOT EXISTS idx_group_command_cooldown_available_at
    ON group_command_cooldown (available_at);

CREATE INDEX IF NOT EXISTS idx_group_command_cooldown_lease_expires_at
    ON group_command_cooldown (lease_expires_at);
