CREATE TABLE IF NOT EXISTS user_info (
    id BIGSERIAL PRIMARY KEY,
    member_openid VARCHAR(128) NOT NULL UNIQUE,
    server VARCHAR(64),
    role_name VARCHAR(64),
    school VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE user_info
    ADD COLUMN IF NOT EXISTS school VARCHAR(64);

CREATE TABLE IF NOT EXISTS user_role_binding (
    id BIGSERIAL PRIMARY KEY,
    member_openid VARCHAR(128) NOT NULL,
    server VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_role_binding_account_role UNIQUE (member_openid, server, role_name)
);

CREATE INDEX IF NOT EXISTS idx_user_role_binding_member_openid
    ON user_role_binding (member_openid);

-- 兼容旧版单角色数据：user_info 继续作为当前默认角色指针。
INSERT INTO user_role_binding (member_openid, server, role_name)
SELECT member_openid, server, role_name
FROM user_info
WHERE server IS NOT NULL
  AND role_name IS NOT NULL
ON CONFLICT (member_openid, server, role_name) DO NOTHING;
