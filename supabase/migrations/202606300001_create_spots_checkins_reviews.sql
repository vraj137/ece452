-- Phase 1: real study spots, check-ins, and reviews.
-- Spots are the source of truth for the map/explore screens. A review can only be
-- created for a spot the user has physically checked into — that gate is enforced
-- here (RLS) as both the harm-mitigation feature and an anti-spam control.

-- ── Spots ────────────────────────────────────────────────────────────────────
-- Managed by admins (seeded below / approved from user_spot_submissions in Phase 5).
-- Regular users get read-only access; there is no insert/update policy for them.

create table if not exists public.spots (
    id uuid primary key default gen_random_uuid(),
    slug text unique not null,
    name text not null,
    building text not null,
    floor text,
    latitude double precision not null,
    longitude double precision not null,
    solo_friendly boolean not null default true,
    group_friendly boolean not null default true,
    amenities text[] not null default '{}',
    created_at timestamptz not null default now()
);

alter table public.spots enable row level security;

create policy "Spots are readable by signed-in users"
on public.spots for select
to authenticated
using (true);

grant select on public.spots to authenticated;
revoke all on public.spots from anon;

-- ── Check-ins ────────────────────────────────────────────────────────────────
-- One row per visit; ended_at null means the user is currently there.
-- Privacy: a user can only read their OWN check-in rows. Aggregate occupancy is
-- exposed separately through the spot_occupancy view (counts only, no identities).

create table if not exists public.check_ins (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    spot_slug text not null references public.spots(slug) on delete cascade,
    mode text not null default 'solo' check (mode in ('solo', 'group')),
    group_session_id uuid,
    started_at timestamptz not null default now(),
    ended_at timestamptz
);

alter table public.check_ins enable row level security;

create policy "Users can read their own check-ins"
on public.check_ins for select
to authenticated
using ((select auth.uid()) = user_id);

create policy "Users can create their own check-ins"
on public.check_ins for insert
to authenticated
with check ((select auth.uid()) = user_id);

create policy "Users can update their own check-ins"
on public.check_ins for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

grant select, insert, update on public.check_ins to authenticated;
revoke all on public.check_ins from anon;

-- Occupancy as counts only. A view (security definer by default) aggregates over
-- all check-ins without exposing individual rows, so privacy is preserved.
create or replace view public.spot_occupancy as
select spot_slug, count(*)::int as active_count
from public.check_ins
where ended_at is null
group by spot_slug;

grant select on public.spot_occupancy to authenticated;
revoke all on public.spot_occupancy from anon;

-- ── Reviews ──────────────────────────────────────────────────────────────────
-- Self-reported noise/lighting/wifi/occupancy + rating. reviewer_name is captured
-- at submit time ("Anonymous" when the user opts in) so reads need no profile join.

create table if not exists public.reviews (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    spot_slug text not null references public.spots(slug) on delete cascade,
    reviewer_name text,
    rating int not null check (rating between 1 and 5),
    noise_level text,
    lighting text,
    wifi_quality text,
    occupancy_percent int check (occupancy_percent between 0 and 100),
    comment text,
    anonymous boolean not null default false,
    created_at timestamptz not null default now()
);

alter table public.reviews enable row level security;

create policy "Reviews are readable by signed-in users"
on public.reviews for select
to authenticated
using (true);

-- The gate: you may only insert a review for a spot you have a check-in at.
create policy "Users can review spots they have checked into"
on public.reviews for insert
to authenticated
with check (
    (select auth.uid()) = user_id
    and exists (
        select 1 from public.check_ins c
        where c.user_id = (select auth.uid())
          and c.spot_slug = reviews.spot_slug
    )
);

create policy "Users can update their own reviews"
on public.reviews for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

grant select, insert, update on public.reviews to authenticated;
revoke all on public.reviews from anon;

-- ── Seed spots (UW campus, coordinates matching the app's MockData) ───────────
insert into public.spots (slug, name, building, floor, latitude, longitude, solo_friendly, group_friendly, amenities)
values
    ('e7-study-hall',    'E7 Study Hall',     'Engineering 7',       'Room 2101',         43.47295, -80.53951, true,  false, '{"Fast Wi-Fi","Outlets","Accessible","Soft seating","Near café"}'),
    ('dc-library-3f',    'DC Library 3F',     'Davis Centre',        'Floor 3',           43.47295, -80.54247, true,  true,  '{"Fast Wi-Fi","Outlets","Accessible","Near café"}'),
    ('slc-boardroom-2a', 'SLC Boardroom 2A',  'Student Life Centre', 'Room 2A',           43.47147, -80.54610, false, true,  '{"Fast Wi-Fi","Whiteboard","Accessible","Soft seating"}'),
    ('mc-atrium',        'MC Atrium',         'Mathematics & Computer','Atrium',          43.47361, -80.54250, true,  true,  '{"Fast Wi-Fi","Outlets","Near café"}'),
    ('e5-collab-lab',    'E5 Collab Lab',     'Engineering 5',       'Collaboration Lab', 43.47125, -80.54044, false, true,  '{"Outlets","Accessible"}'),
    ('dp-library',       'DP Library',        'Dana Porter Library', 'Floor 4',           43.46990, -80.54213, true,  false, '{"Fast Wi-Fi","Outlets","Accessible","Soft seating"}')
on conflict (slug) do nothing;
