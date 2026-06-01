alter table recommendation_cache add column if not exists item_id varchar(128);
alter table recommendation_cache add column if not exists item_rank integer;
alter table recommendation_cache add column if not exists score numeric(12, 4);
alter table recommendation_cache add column if not exists reason_code varchar(64);
alter table recommendation_cache add column if not exists reason_text varchar(512);

create index if not exists idx_recommendation_cache_user_type_rank
    on recommendation_cache (user_id, recommendation_type, item_rank);

create index if not exists idx_recommendation_cache_item_id
    on recommendation_cache (item_id);

alter table global_recommendation_cache add column if not exists item_id varchar(128);
alter table global_recommendation_cache add column if not exists item_rank integer;
alter table global_recommendation_cache add column if not exists score numeric(12, 4);
alter table global_recommendation_cache add column if not exists reason_code varchar(64);
alter table global_recommendation_cache add column if not exists reason_text varchar(512);

create index if not exists idx_global_recommendation_cache_type_rank
    on global_recommendation_cache (recommendation_type, item_rank);

create index if not exists idx_global_recommendation_cache_item_id
    on global_recommendation_cache (item_id);
