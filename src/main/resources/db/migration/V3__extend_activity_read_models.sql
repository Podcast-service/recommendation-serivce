alter table user_podcast_interaction add column if not exists play_finished_count bigint not null default 0;
alter table user_podcast_interaction add column if not exists liked boolean not null default false;
alter table user_podcast_interaction add column if not exists disliked boolean not null default false;
alter table user_podcast_interaction add column if not exists max_progress_percent numeric(5, 2);
alter table user_podcast_interaction add column if not exists last_interaction_at timestamp with time zone;

alter table user_category_interest add column if not exists last_event_at timestamp with time zone;

alter table user_author_interest add column if not exists last_event_at timestamp with time zone;

alter table podcast_daily_stats add column if not exists dislike_count bigint not null default 0;
alter table podcast_daily_stats add column if not exists play_finished_count bigint not null default 0;

alter table author_daily_stats add column if not exists followed_count bigint not null default 0;
alter table author_daily_stats add column if not exists unfollowed_count bigint not null default 0;
alter table author_daily_stats add column if not exists like_count bigint not null default 0;
alter table author_daily_stats add column if not exists dislike_count bigint not null default 0;
alter table author_daily_stats add column if not exists play_finished_count bigint not null default 0;

create index if not exists idx_user_podcast_interaction_last_interaction_at
    on user_podcast_interaction (last_interaction_at);

create index if not exists idx_user_category_interest_last_event_at
    on user_category_interest (last_event_at);

create index if not exists idx_user_author_interest_last_event_at
    on user_author_interest (last_event_at);
