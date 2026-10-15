-- Keep Business Receipt: end-user accounts and lockout audit log.

create table if not exists te_kbr_users (
    id                        varchar(36) primary key,
    email                     varchar(255) not null unique,
    pin_hash                  varchar(72) not null,
    status                    varchar(20) not null,
    business_name             varchar(255),
    created_at                timestamptz not null default now(),
    approved_at               timestamptz,
    failed_login_attempts     int not null default 0,
    locked_until              timestamptz
);

create index if not exists idx_te_kbr_users_status on te_kbr_users (status);

alter table te_kbr_users enable row level security;


create table if not exists te_kbr_user_lockout_events (
    id                        varchar(36) primary key,
    user_id                   varchar(36) not null references te_kbr_users(id) on delete cascade,
    locked_at                 timestamptz not null default now(),
    failed_attempt_count      int not null,
    ip_address                varchar(45),
    user_agent                varchar(500)
);

create index if not exists idx_te_kbr_user_lockout_events_user_id on te_kbr_user_lockout_events (user_id);

alter table te_kbr_user_lockout_events enable row level security;
