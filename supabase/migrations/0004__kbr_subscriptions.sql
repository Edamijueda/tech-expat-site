-- Keep Business Receipt: free trial, admin-granted extension, and recorded paid periods.

alter table te_kbr_users
    add column if not exists trial_started_at   timestamptz,
    add column if not exists trial_days         int not null default 30,
    add column if not exists trial_extended_at  timestamptz;


-- One row per payment collected by hand. period_start is the first day of the first paid
-- month; period_end is the first day of the month after the last, exclusive — so January
-- to March 2027 is stored as 2027-01-01 / 2027-04-01.
create table if not exists te_kbr_subscription_payments (
    id                        varchar(36) primary key,
    user_id                   varchar(36) not null references te_kbr_users(id) on delete cascade,
    period_start              date not null,
    period_end                date not null,
    months_count              int not null,
    note                      varchar(500),
    recorded_at               timestamptz not null default now(),
    constraint te_kbr_subscription_payments_period_order check (period_end > period_start)
);

create index if not exists idx_te_kbr_subscription_payments_user_id_period_start
    on te_kbr_subscription_payments (user_id, period_start desc);

alter table te_kbr_subscription_payments enable row level security;
