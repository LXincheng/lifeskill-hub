alter table learning_attempt
    add column attempt_kind varchar(24) not null default 'PROGRESS',
    add column completed_units integer not null default 0,
    add column total_units integer not null default 1;

alter table learning_attempt
    add constraint chk_learning_attempt_kind check (attempt_kind in ('PROGRESS', 'QUIZ')),
    add constraint chk_learning_attempt_status check (status in ('IN_PROGRESS', 'COMPLETED')),
    add constraint chk_learning_attempt_units check (
        total_units > 0 and completed_units >= 0 and completed_units <= total_units
    ),
    add constraint chk_learning_attempt_score check (score is null or (score >= 0 and score <= 100));

create index idx_learning_attempt_content_created
    on learning_attempt (content_item_id, created_at desc);

alter table content_item drop constraint chk_content_verification_status;

alter table content_item
    add constraint chk_content_verification_status
    check (verification_status in ('USER_AUTHORED', 'AI_GENERATED', 'VERIFIED', 'PARTIALLY_VERIFIED'));
