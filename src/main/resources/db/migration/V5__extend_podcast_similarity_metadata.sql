alter table podcast_catalog_snapshot add column if not exists tags text;
alter table podcast_catalog_snapshot add column if not exists duration_seconds integer;

create index if not exists idx_podcast_catalog_snapshot_duration_seconds
    on podcast_catalog_snapshot (duration_seconds);
