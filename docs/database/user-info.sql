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

-- Hibernate ddl-auto=update 不会可靠删除旧唯一约束。按约束包含的列识别并移除
-- 所有旧的“用户 + 区服 + 角色名”唯一约束，避免同一成员在不同群绑定同一角色时冲突。
DO $$
DECLARE
    legacy_constraint_name TEXT;
BEGIN
    FOR legacy_constraint_name IN
        SELECT constraint_row.conname
        FROM pg_constraint constraint_row
        WHERE constraint_row.conrelid = 'user_role_binding'::regclass
          AND constraint_row.contype = 'u'
          AND ARRAY(
                SELECT attribute.attname::TEXT
                FROM unnest(constraint_row.conkey) WITH ORDINALITY AS key_column(attnum, position)
                JOIN pg_attribute attribute
                  ON attribute.attrelid = constraint_row.conrelid
                 AND attribute.attnum = key_column.attnum
                ORDER BY key_column.position
          ) = ARRAY['member_openid', 'server', 'role_name']::TEXT[]
    LOOP
        EXECUTE format(
                'ALTER TABLE user_role_binding DROP CONSTRAINT %I',
                legacy_constraint_name);
    END LOOP;
END $$;

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
-- 对应用户在目标群重新执行“绑定角色 区服 角色名”即可生成群级记录。