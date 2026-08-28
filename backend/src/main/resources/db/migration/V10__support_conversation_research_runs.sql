alter table skill_run
    alter column skill_id drop not null,
    alter column skill_version set default 0,
    add column conversation_id uuid references conversation(id) on delete set null,
    add column source_message_id uuid references message(id) on delete set null,
    add column objective text,
    add column capability varchar(48),
    add column result_content_id uuid references content_item(id) on delete set null;

alter table message
    add column agent_run_id uuid references skill_run(id) on delete set null;

alter table skill_run
    add constraint ck_skill_run_origin check (
        (skill_id is not null and skill_version >= 1)
        or
        (skill_id is null and conversation_id is not null and source_message_id is not null
            and skill_version = 0 and objective is not null and capability is not null)
    );

create index idx_skill_run_conversation_created
    on skill_run (conversation_id, created_at desc)
    where conversation_id is not null;
