CREATE TABLE IF NOT EXISTS group_info (
    id BIGSERIAL PRIMARY KEY,
    group_id VARCHAR(128),
    open_group_id VARCHAR(128),
    server VARCHAR(64),
    commands_enabled BOOLEAN,
    experimental_enabled BOOLEAN,
    active_messages_enabled BOOLEAN,
    active_messages_platform_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    active_message_reserved_at TIMESTAMP WITH TIME ZONE,
    active_message_reservation_id VARCHAR(36),
    ws_server INTEGER,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE group_info ADD COLUMN IF NOT EXISTS commands_enabled BOOLEAN;
ALTER TABLE group_info ADD COLUMN IF NOT EXISTS experimental_enabled BOOLEAN;
ALTER TABLE group_info ADD COLUMN IF NOT EXISTS active_messages_enabled BOOLEAN;
ALTER TABLE group_info ADD COLUMN IF NOT EXISTS active_messages_platform_allowed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE group_info ADD COLUMN IF NOT EXISTS active_message_reserved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE group_info ADD COLUMN IF NOT EXISTS active_message_reservation_id VARCHAR(36);

CREATE INDEX IF NOT EXISTS idx_group_info_open_group_id
    ON group_info (open_group_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_group_info_open_group_id
    ON group_info (open_group_id)
    WHERE open_group_id IS NOT NULL;
