-- Keep Business Docs (was Keep Business Receipt): rename every kbr-era object to the kbd
-- prefix. Renames only, no data movement — column names are untouched, storage_key
-- included, so the stored objects stay reachable.

alter table if exists te_kbr_users                 rename to te_kbd_users;
alter table if exists te_kbr_user_lockout_events   rename to te_kbd_user_lockout_events;
alter table if exists te_kbr_receipts              rename to te_kbd_docs;
alter table if exists te_kbr_subscription_payments rename to te_kbd_subscription_payments;


-- Renaming a table leaves its indexes behind under their old names.
alter index if exists idx_te_kbr_users_status
    rename to idx_te_kbd_users_status;
alter index if exists idx_te_kbr_user_lockout_events_user_id
    rename to idx_te_kbd_user_lockout_events_user_id;
alter index if exists idx_te_kbr_receipts_user_id_uploaded_at
    rename to idx_te_kbd_docs_user_id_uploaded_at;
alter index if exists idx_te_kbr_subscription_payments_user_id_period_start
    rename to idx_te_kbd_subscription_payments_user_id_period_start;


-- Its constraints keep their old names too: the primary keys, the unique email, the three
-- foreign keys and the period_order check. ALTER TABLE ... RENAME CONSTRAINT has no
-- IF EXISTS clause, so this runs over whatever is actually still named te_kbr_*, which
-- also makes the file safe to run twice.
do $$
declare
    con record;
    new_name text;
begin
    for con in
        select c.conname, cl.relname as table_name
        from pg_constraint c
        join pg_class cl on cl.oid = c.conrelid
        join pg_namespace n on n.oid = cl.relnamespace
        where n.nspname = 'public'
          and c.conname like 'te_kbr%'
    loop
        new_name := replace(replace(con.conname, 'te_kbr_receipts', 'te_kbd_docs'),
                            'te_kbr_', 'te_kbd_');
        execute format('alter table %I rename constraint %I to %I',
                       con.table_name, con.conname, new_name);
    end loop;
end $$;
