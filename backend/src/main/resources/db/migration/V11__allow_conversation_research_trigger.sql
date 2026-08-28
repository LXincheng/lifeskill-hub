alter table skill_run drop constraint chk_skill_run_trigger;

alter table skill_run
    add constraint chk_skill_run_trigger
    check (trigger_type in ('MANUAL', 'SCHEDULED', 'CONVERSATION_RESEARCH'));
