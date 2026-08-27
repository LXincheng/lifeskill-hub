alter table message
    add column processing_steps jsonb not null default '[]'::jsonb,
    add column duration_ms bigint;

alter table message
    add constraint chk_message_duration_non_negative
    check (duration_ms is null or duration_ms >= 0);
