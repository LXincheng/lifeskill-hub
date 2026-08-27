create unique index uq_content_item_generated_run_type
    on content_item (source_skill_run_id, type)
    where source_skill_run_id is not null;
