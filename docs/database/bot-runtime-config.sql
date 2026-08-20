create table if not exists bot_runtime_config (
    id bigserial primary key,
    config_key varchar(128) not null,
    config_value text not null,
    updated_by varchar(128),
    create_time timestamp not null default current_timestamp,
    update_time timestamp not null default current_timestamp,
    constraint uk_bot_runtime_config_key unique (config_key)
);

create table if not exists bot_admin_audit_log (
    id bigserial primary key,
    category varchar(32) not null,
    action varchar(64) not null,
    target_key varchar(128),
    actor_openid varchar(128),
    actor_source varchar(32),
    success boolean not null,
    failure_summary varchar(256),
    create_time timestamp not null default current_timestamp
);

create index if not exists idx_bot_admin_audit_log_create_time
    on bot_admin_audit_log (create_time desc);

create index if not exists idx_bot_admin_audit_log_category_action
    on bot_admin_audit_log (category, action);