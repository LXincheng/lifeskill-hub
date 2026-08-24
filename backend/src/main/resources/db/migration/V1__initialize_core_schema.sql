create table conversation (
    id uuid primary key default gen_random_uuid(),
    title varchar(160) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table message (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid not null references conversation(id) on delete cascade,
    role varchar(24) not null,
    content text not null,
    created_at timestamptz not null default now()
);

create index idx_message_conversation_created
    on message (conversation_id, created_at);

create table skill (
    id uuid primary key default gen_random_uuid(),
    name varchar(120) not null,
    description text,
    status varchar(24) not null,
    current_version integer not null default 1,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table skill_version (
    id uuid primary key default gen_random_uuid(),
    skill_id uuid not null references skill(id) on delete cascade,
    version integer not null,
    config_json jsonb not null,
    created_at timestamptz not null default now(),
    unique (skill_id, version)
);

create table skill_run (
    id uuid primary key default gen_random_uuid(),
    skill_id uuid not null references skill(id),
    skill_version integer not null,
    status varchar(24) not null,
    started_at timestamptz,
    completed_at timestamptz,
    error_code varchar(80),
    created_at timestamptz not null default now()
);

create index idx_skill_run_skill_created
    on skill_run (skill_id, created_at desc);

create table agent_step (
    id uuid primary key default gen_random_uuid(),
    skill_run_id uuid not null references skill_run(id) on delete cascade,
    step_order integer not null,
    role varchar(32) not null,
    status varchar(24) not null,
    input_summary text,
    output_summary text,
    tool_name varchar(100),
    duration_ms bigint,
    created_at timestamptz not null default now(),
    unique (skill_run_id, step_order)
);

create table evidence (
    id uuid primary key default gen_random_uuid(),
    skill_run_id uuid references skill_run(id) on delete cascade,
    source_type varchar(40) not null,
    source_name varchar(160) not null,
    source_url text not null,
    external_id varchar(240),
    title text,
    excerpt text,
    published_at timestamptz,
    fetched_at timestamptz not null default now(),
    content_hash varchar(64) not null,
    metadata_json jsonb not null default '{}'::jsonb
);

create unique index uq_evidence_source_hash
    on evidence (source_url, content_hash);

create table claim (
    id uuid primary key default gen_random_uuid(),
    skill_run_id uuid not null references skill_run(id) on delete cascade,
    statement text not null,
    verification_status varchar(32) not null,
    confidence numeric(4, 3),
    created_at timestamptz not null default now()
);

create table claim_evidence (
    claim_id uuid not null references claim(id) on delete cascade,
    evidence_id uuid not null references evidence(id) on delete cascade,
    primary key (claim_id, evidence_id)
);

create table learning_folder (
    id uuid primary key default gen_random_uuid(),
    parent_id uuid references learning_folder(id) on delete cascade,
    name varchar(160) not null,
    description text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table content_item (
    id uuid primary key default gen_random_uuid(),
    folder_id uuid not null references learning_folder(id) on delete cascade,
    source_skill_run_id uuid references skill_run(id),
    type varchar(32) not null,
    title varchar(240) not null,
    payload_json jsonb not null,
    verification_status varchar(32) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table learning_attempt (
    id uuid primary key default gen_random_uuid(),
    content_item_id uuid not null references content_item(id) on delete cascade,
    status varchar(24) not null,
    score numeric(5, 2),
    result_json jsonb not null default '{}'::jsonb,
    completed_at timestamptz,
    created_at timestamptz not null default now()
);

create table pulse_item (
    id uuid primary key default gen_random_uuid(),
    skill_run_id uuid references skill_run(id),
    content_item_id uuid references content_item(id),
    category varchar(80) not null,
    title varchar(240) not null,
    summary text not null,
    verification_status varchar(32) not null,
    published_at timestamptz not null default now(),
    read_at timestamptz
);

create index idx_pulse_item_published
    on pulse_item (published_at desc);

create table notification_rule (
    id uuid primary key default gen_random_uuid(),
    skill_id uuid not null references skill(id) on delete cascade,
    channel varchar(32) not null,
    schedule_expression varchar(120),
    quiet_hours_json jsonb not null default '{}'::jsonb,
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
