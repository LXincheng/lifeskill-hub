create index idx_content_item_folder_updated
    on content_item (folder_id, updated_at desc);

alter table content_item
    add constraint chk_content_item_type
        check (type in ('ARTICLE', 'NOTE', 'CHECKLIST'));

alter table content_item
    add constraint chk_content_verification_status
        check (verification_status in ('USER_AUTHORED', 'VERIFIED', 'PARTIALLY_VERIFIED'));
