CREATE TABLE IF NOT EXISTS user_info (
    id BIGSERIAL PRIMARY KEY,
    member_openid VARCHAR(128) NOT NULL UNIQUE,
    server VARCHAR(64),
    role_name VARCHAR(64),
    school VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE user_info ADD COLUMN IF NOT EXISTS school VARCHAR(64);

CREATE TABLE IF NOT EXISTS user_role_binding (
    id BIGSERIAL PRIMARY KEY,
    group_open_id VARCHAR(128),
    member_openid VARCHAR(128) NOT NULL,
    server VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    school VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE user_role_binding ADD COLUMN IF NOT EXISTS group_open_id VARCHAR(128);
ALTER TABLE user_role_binding ADD COLUMN IF NOT EXISTS school VARCHAR(64);
ALTER TABLE user_role_binding DROP CONSTRAINT IF EXISTS uk_user_role_binding_account_role;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_user_role_binding_group_account_role') THEN
        ALTER TABLE user_role_binding ADD CONSTRAINT uk_user_role_binding_group_account_role
            UNIQUE (group_open_id, member_openid, server, role_name);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_user_role_binding_group_member
    ON user_role_binding (group_open_id, member_openid);
CREATE INDEX IF NOT EXISTS idx_user_role_binding_group
    ON user_role_binding (group_open_id);

-- 旧记录没有 group_open_id，无法可靠判断所属群，不参与群查询或定时推送。
-- 对应用户在目标群重新执行“绑定角色 区服 角色名 门派”即可生成群级记录。