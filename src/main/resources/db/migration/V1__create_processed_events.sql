create table processed_events (
    event_id varchar(128) primary key,
    event_type varchar(128) not null,
    event_version integer not null,
    processed_at timestamp with time zone not null default now()
);

create index idx_processed_events_processed_at
    on processed_events (processed_at);
