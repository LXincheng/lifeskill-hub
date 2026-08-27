alter table skill_run
    add column audit_id uuid not null default gen_random_uuid(),
    add column trigger_type varchar(24) not null default 'MANUAL',
    add column schedule_slot varchar(80),
    add column max_steps integer not null default 8,
    add column step_count integer not null default 0,
    add column timeout_at timestamptz,
    add column duration_ms bigint,
    add column failure_summary text;

alter table skill_run
    add constraint chk_skill_run_limits
        check (max_steps > 0 and step_count >= 0 and step_count <= max_steps),
    add constraint chk_skill_run_duration
        check (duration_ms is null or duration_ms >= 0),
    add constraint chk_skill_run_trigger
        check (trigger_type in ('MANUAL', 'SCHEDULED'));

create unique index uq_skill_run_schedule_slot
    on skill_run (skill_id, schedule_slot)
    where schedule_slot is not null;

alter table agent_step
    add column event_type varchar(48) not null default 'STEP',
    add column source_url text,
    add column error_summary text,
    add column completed_at timestamptz;

alter table evidence
    add column raw_content text,
    add column official_source boolean not null default false;

alter table claim
    add column verification_summary text,
    add column verified_at timestamptz;

alter table pulse_item
    add column source_count integer not null default 0,
    add column recommendation_reason text not null default '',
    add column primary_claim_id uuid references claim(id);

alter table pulse_item
    add constraint chk_pulse_source_count check (source_count >= 0);

create index idx_claim_evidence_evidence on claim_evidence (evidence_id);
create index idx_evidence_skill_run on evidence (skill_run_id, fetched_at desc);
