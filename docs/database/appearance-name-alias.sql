-- group_open_id 保留用于兼容现有表结构；当前服务统一写入 __GLOBAL__，外观别名不按 QQ 群隔离。
CREATE TABLE IF NOT EXISTS appearance_name_alias (
    id BIGSERIAL PRIMARY KEY,
    group_open_id VARCHAR(128) NOT NULL,
    canonical_name VARCHAR(100) NOT NULL,
    normalized_canonical_name VARCHAR(100) NOT NULL,
    alias_name VARCHAR(100) NOT NULL,
    normalized_alias_name VARCHAR(100) NOT NULL,
    review_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    creator_member_openid VARCHAR(128),
    creator_name VARCHAR(100),
    reviewer_member_openid VARCHAR(128),
    reviewer_name VARCHAR(100),
    review_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE appearance_name_alias
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';

ALTER TABLE appearance_name_alias
    ADD COLUMN IF NOT EXISTS reviewer_member_openid VARCHAR(128);

ALTER TABLE appearance_name_alias
    ADD COLUMN IF NOT EXISTS reviewer_name VARCHAR(100);

ALTER TABLE appearance_name_alias
    ADD COLUMN IF NOT EXISTS review_time TIMESTAMP;

UPDATE appearance_name_alias
SET review_status = 'APPROVED'
WHERE review_status IS NULL OR review_status = '';

CREATE UNIQUE INDEX IF NOT EXISTS uk_appearance_name_alias_group_alias
    ON appearance_name_alias (group_open_id, normalized_alias_name);

CREATE INDEX IF NOT EXISTS idx_appearance_name_alias_group_canonical
    ON appearance_name_alias (group_open_id, normalized_canonical_name);

CREATE INDEX IF NOT EXISTS idx_appearance_name_alias_group_status
    ON appearance_name_alias (group_open_id, review_status, normalized_canonical_name, alias_name);
