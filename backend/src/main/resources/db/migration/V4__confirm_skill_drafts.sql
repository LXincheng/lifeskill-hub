alter table skill
    add column source_draft_id uuid not null;

alter table skill
    add constraint uq_skill_source_draft unique (source_draft_id);

alter table skill_draft
    add column confirmed_skill_id uuid references skill(id),
    add column confirmation_key varchar(120),
    add column confirmed_at timestamptz;

alter table skill_draft
    add constraint uq_skill_draft_confirmation_key unique (confirmation_key);

alter table skill_draft
    drop constraint chk_skill_draft_status;

alter table skill_draft
    add constraint chk_skill_draft_status
        check (status in ('PENDING_CONFIRMATION', 'CONFIRMED'));

alter table skill_draft
    add constraint chk_skill_draft_confirmation
        check (
            (status = 'PENDING_CONFIRMATION'
                and confirmed_skill_id is null
                and confirmation_key is null
                and confirmed_at is null)
            or
            (status = 'CONFIRMED'
                and confirmed_skill_id is not null
                and confirmation_key is not null
                and confirmed_at is not null)
        );
