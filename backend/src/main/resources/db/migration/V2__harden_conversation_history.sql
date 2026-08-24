alter table conversation
    add column version bigint not null default 0,
    add constraint chk_conversation_title_not_blank
        check (char_length(trim(title)) > 0);

alter table message
    add constraint chk_message_role
        check (role in ('USER', 'ASSISTANT', 'SYSTEM')),
    add constraint chk_message_content_length
        check (char_length(trim(content)) between 1 and 4000);

drop index idx_message_conversation_created;

create index idx_message_conversation_created
    on message (conversation_id, created_at, id);
