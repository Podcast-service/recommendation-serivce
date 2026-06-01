create table podcast_catalog_snapshot (
    podcast_id varchar(128) primary key,
    author_id varchar(128),
    category_id varchar(128),
    title varchar(512) not null,
    description text,
    language varchar(32),
    status varchar(64) not null,
    published_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create index idx_podcast_catalog_snapshot_author_id
    on podcast_catalog_snapshot (author_id);

create index idx_podcast_catalog_snapshot_category_id
    on podcast_catalog_snapshot (category_id);

create index idx_podcast_catalog_snapshot_status
    on podcast_catalog_snapshot (status);

create index idx_podcast_catalog_snapshot_updated_at
    on podcast_catalog_snapshot (updated_at);

create table author_catalog_snapshot (
    author_id varchar(128) primary key,
    display_name varchar(512) not null,
    description text,
    status varchar(64) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create index idx_author_catalog_snapshot_status
    on author_catalog_snapshot (status);

create index idx_author_catalog_snapshot_updated_at
    on author_catalog_snapshot (updated_at);

create table playlist_catalog_snapshot (
    playlist_id varchar(128) primary key,
    owner_user_id varchar(128),
    title varchar(512) not null,
    description text,
    visibility varchar(64) not null,
    status varchar(64) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create index idx_playlist_catalog_snapshot_owner_user_id
    on playlist_catalog_snapshot (owner_user_id);

create index idx_playlist_catalog_snapshot_visibility
    on playlist_catalog_snapshot (visibility);

create index idx_playlist_catalog_snapshot_status
    on playlist_catalog_snapshot (status);

create index idx_playlist_catalog_snapshot_updated_at
    on playlist_catalog_snapshot (updated_at);

create table playlist_item_snapshot (
    playlist_id varchar(128) not null,
    podcast_id varchar(128) not null,
    item_position integer not null,
    added_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    primary key (playlist_id, podcast_id)
);

create index idx_playlist_item_snapshot_playlist_position
    on playlist_item_snapshot (playlist_id, item_position);

create index idx_playlist_item_snapshot_podcast_id
    on playlist_item_snapshot (podcast_id);

create table user_category_interest (
    user_id varchar(128) not null,
    category_id varchar(128) not null,
    interest_score numeric(12, 6) not null default 0,
    signal_count bigint not null default 0,
    last_signal_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    primary key (user_id, category_id)
);

create index idx_user_category_interest_category_id
    on user_category_interest (category_id);

create index idx_user_category_interest_user_score
    on user_category_interest (user_id, interest_score desc);

create index idx_user_category_interest_updated_at
    on user_category_interest (updated_at);

create table user_author_interest (
    user_id varchar(128) not null,
    author_id varchar(128) not null,
    interest_score numeric(12, 6) not null default 0,
    signal_count bigint not null default 0,
    last_signal_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    primary key (user_id, author_id)
);

create index idx_user_author_interest_author_id
    on user_author_interest (author_id);

create index idx_user_author_interest_user_score
    on user_author_interest (user_id, interest_score desc);

create index idx_user_author_interest_updated_at
    on user_author_interest (updated_at);

create table user_podcast_interaction (
    interaction_id varchar(128) primary key,
    user_id varchar(128) not null,
    podcast_id varchar(128) not null,
    interaction_type varchar(64) not null,
    interaction_value numeric(12, 6),
    occurred_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp
);

create index idx_user_podcast_interaction_user_podcast
    on user_podcast_interaction (user_id, podcast_id);

create index idx_user_podcast_interaction_podcast_occurred
    on user_podcast_interaction (podcast_id, occurred_at);

create index idx_user_podcast_interaction_type_occurred
    on user_podcast_interaction (interaction_type, occurred_at);

create table podcast_daily_stats (
    podcast_id varchar(128) not null,
    stat_date date not null,
    play_count bigint not null default 0,
    completion_count bigint not null default 0,
    like_count bigint not null default 0,
    share_count bigint not null default 0,
    rating_count bigint not null default 0,
    rating_sum numeric(14, 4) not null default 0,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    primary key (podcast_id, stat_date)
);

create index idx_podcast_daily_stats_stat_date
    on podcast_daily_stats (stat_date);

create index idx_podcast_daily_stats_updated_at
    on podcast_daily_stats (updated_at);

create table author_daily_stats (
    author_id varchar(128) not null,
    stat_date date not null,
    podcast_count bigint not null default 0,
    play_count bigint not null default 0,
    completion_count bigint not null default 0,
    follower_count bigint not null default 0,
    rating_count bigint not null default 0,
    rating_sum numeric(14, 4) not null default 0,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    primary key (author_id, stat_date)
);

create index idx_author_daily_stats_stat_date
    on author_daily_stats (stat_date);

create index idx_author_daily_stats_updated_at
    on author_daily_stats (updated_at);

create table playlist_daily_stats (
    playlist_id varchar(128) not null,
    stat_date date not null,
    view_count bigint not null default 0,
    play_count bigint not null default 0,
    follower_count bigint not null default 0,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    primary key (playlist_id, stat_date)
);

create index idx_playlist_daily_stats_stat_date
    on playlist_daily_stats (stat_date);

create index idx_playlist_daily_stats_updated_at
    on playlist_daily_stats (updated_at);

create table recommendation_cache (
    user_id varchar(128) not null,
    recommendation_type varchar(64) not null,
    cache_key varchar(256) not null,
    payload text not null,
    generated_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    primary key (user_id, recommendation_type, cache_key)
);

create index idx_recommendation_cache_expires_at
    on recommendation_cache (expires_at);

create index idx_recommendation_cache_generated_at
    on recommendation_cache (generated_at);

create table global_recommendation_cache (
    recommendation_type varchar(64) not null,
    cache_key varchar(256) not null,
    payload text not null,
    generated_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    primary key (recommendation_type, cache_key)
);

create index idx_global_recommendation_cache_expires_at
    on global_recommendation_cache (expires_at);

create index idx_global_recommendation_cache_generated_at
    on global_recommendation_cache (generated_at);
