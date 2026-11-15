-- Keep Business Docs: request-access attempts, one row per submission, so the per-IP
-- daily limit survives a deploy. The limiter held these in memory, where a Railway
-- restart handed everyone a fresh allowance.

create table if not exists te_kbd_request_attempts (
    id                        varchar(36) primary key,
    ip_address                varchar(45) not null,
    attempted_at              timestamptz not null default now()
);

-- Serves the only query there is: how many attempts this address has made since the
-- window opened.
create index if not exists idx_te_kbd_request_attempts_ip_address_attempted_at
    on te_kbd_request_attempts (ip_address, attempted_at desc);

alter table te_kbd_request_attempts enable row level security;
