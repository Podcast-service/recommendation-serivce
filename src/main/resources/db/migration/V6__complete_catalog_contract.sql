alter table podcast_catalog_snapshot add column if not exists is_explicit boolean;

create index if not exists idx_recommendation_cache_type_item_id
    on recommendation_cache (recommendation_type, item_id);

create index if not exists idx_global_recommendation_cache_type_item_id
    on global_recommendation_cache (recommendation_type, item_id);
