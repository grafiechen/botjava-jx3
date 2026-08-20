CREATE TABLE IF NOT EXISTS group_command_permission_grant (
    id BIGSERIAL PRIMARY KEY,
    group_open_id VARCHAR(128) NOT NULL,
    member_openid VARCHAR(128) NOT NULL,
    permission_key VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_group_command_permission_grant
        UNIQUE (group_open_id, member_openid, permission_key)
);

CREATE INDEX IF NOT EXISTS idx_group_command_permission_lookup
    ON group_command_permission_grant (group_open_id, member_openid, permission_key, enabled);

-- 日常推送字段配置授权示例：group_open_id 可写具体群，也可写 * 表示该成员对所有群有效。
-- INSERT INTO group_command_permission_grant(group_open_id, member_openid, permission_key)
-- VALUES ('群 openid', '成员 member_openid', 'DAILY_PUSH_FIELD_CONFIG');