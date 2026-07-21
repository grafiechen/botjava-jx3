CREATE TABLE IF NOT EXISTS jx3_api_cache (
    id BIGSERIAL PRIMARY KEY,
    cache_key VARCHAR(64) NOT NULL,
    request_path VARCHAR(160) NOT NULL,
    response_json TEXT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    create_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_jx3_api_cache_cache_key UNIQUE (cache_key)
);

CREATE INDEX IF NOT EXISTS idx_jx3_api_cache_expires_at
    ON jx3_api_cache (expires_at);
