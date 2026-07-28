alter table public.profiles
    add column if not exists login_streak int not null default 0,
    add column if not exists last_login_date date,
    add column if not exists longest_login_streak int not null default 0,
    add column if not exists checkout_count int not null default 0;

create table if not exists public.study_sessions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    spot_id text not null,
    spot_name text not null,
    duration_seconds int not null,
    finished_at timestamptz not null default now()
);

alter table public.study_sessions enable row level security;

create policy "Users manage own study sessions"
on public.study_sessions
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

grant select, insert on public.study_sessions to authenticated;
revoke all on public.study_sessions from anon;

alter table public.reviews
    add column if not exists quality_score int not null default 0;

create table if not exists public.user_badges (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    badge_id text not null,
    earned_at timestamptz not null default now(),
    unique(user_id, badge_id)
);

alter table public.user_badges enable row level security;

create policy "Users manage own badges"
on public.user_badges
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

grant select, insert on public.user_badges to authenticated;
revoke all on public.user_badges from anon;
