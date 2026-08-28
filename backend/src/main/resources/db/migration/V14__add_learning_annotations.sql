create table learning_annotation (
    id uuid primary key,
    content_item_id uuid not null references content_item(id) on delete cascade,
    kind varchar(24) not null,
    selected_text varchar(2000),
    note varchar(2000),
    created_at timestamptz not null,
    constraint chk_learning_annotation_kind check (kind in ('HIGHLIGHT', 'FEEDBACK')),
    constraint chk_learning_annotation_value check (
        (kind = 'HIGHLIGHT' and selected_text is not null and length(trim(selected_text)) > 0)
        or (kind = 'FEEDBACK' and note is not null and length(trim(note)) > 0)
    )
);

create index idx_learning_annotation_content_created
    on learning_annotation (content_item_id, created_at desc);
