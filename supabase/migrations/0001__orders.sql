create table if not exists te_orders (
    id                        varchar(36) primary key,
    course_slug               varchar(100) not null,
    buyer_email               varchar(255) not null,
    price_usd                 numeric(10, 2) not null,
    status                    varchar(20) not null,
    nowpayments_invoice_id    varchar(100),
    nowpayments_payment_id    varchar(100),
    created_at                timestamptz not null default now(),
    updated_at                timestamptz not null default now()
);

create index if not exists idx_te_orders_nowpayments_invoice_id on te_orders (nowpayments_invoice_id);
create index if not exists idx_te_orders_status on te_orders (status);

alter table te_orders enable row level security;
