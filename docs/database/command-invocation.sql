CREATE TABLE IF NOT EXISTS command_invocation (
    id BIGSERIAL PRIMARY KEY,
    invocation_id VARCHAR(16) NOT NULL UNIQUE,
    group_openid VARCHAR(128) NOT NULL,
    member_openid VARCHAR(128),
    command_name VARCHAR(64) NOT NULL,
    command_group VARCHAR(32) NOT NULL,
    external_call BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL,
    response_type VARCHAR(32),
    elapsed_ms BIGINT NOT NULL,
    failure_summary VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_command_invocation_group_time
    ON command_invocation (group_openid, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_command_invocation_command_time
    ON command_invocation (command_name, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_command_invocation_status_time
    ON command_invocation (status, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_command_invocation_create_time
    ON command_invocation (create_time);
