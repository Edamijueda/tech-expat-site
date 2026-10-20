-- Keep Business Receipt: stored receipts belonging to end-user accounts.

create table if not exists te_kbr_receipts (
    id                        varchar(36) primary key,
    user_id                   varchar(36) not null references te_kbr_users(id) on delete cascade,
    description               varchar(500) not null,
    storage_key               varchar(500) not null,
    original_filename         varchar(255),
    content_type              varchar(100) not null,
    size_bytes                bigint not null,
    uploaded_at               timestamptz not null default now()
);

create index if not exists idx_te_kbr_receipts_user_id_uploaded_at
    on te_kbr_receipts (user_id, uploaded_at desc);

alter table te_kbr_receipts enable row level security;
