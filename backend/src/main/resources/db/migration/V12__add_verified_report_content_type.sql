alter table content_item drop constraint chk_content_item_type;

alter table content_item
    add constraint chk_content_item_type
    check (type in ('REPORT', 'LEARNING_PATH', 'ARTICLE', 'NOTE', 'QUIZ', 'CHECKLIST'));
