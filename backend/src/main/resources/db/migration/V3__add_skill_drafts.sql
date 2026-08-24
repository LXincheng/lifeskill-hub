create table skill_draft (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid not null references conversation(id) on delete cascade,
    source_message_id uuid not null references message(id) on delete cascade,
    title varchar(120) not null,
    objective text not null,
    schedule_day_of_week varchar(12) not null,
    schedule_time time not null,
    timezone varchar(80) not null,
    status varchar(32) not null,
    prompt_version varchar(40) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_skill_draft_source_message unique (source_message_id),
    constraint chk_skill_draft_title check (char_length(trim(title)) between 1 and 120),
    constraint chk_skill_draft_objective check (char_length(trim(objective)) between 1 and 1000),
    constraint chk_skill_draft_day check (schedule_day_of_week in (
        'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
    )),
    constraint chk_skill_draft_status check (status in ('PENDING_CONFIRMATION'))
);

create index idx_skill_draft_conversation_created
    on skill_draft (conversation_id, created_at, id);
