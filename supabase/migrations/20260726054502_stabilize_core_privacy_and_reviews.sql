-- Reconciles checked-in migration history with the deployed project and
-- hardens the app's location, privacy, group, and review workflows.

alter table public.spots
    add column if not exists capacity int,
    add column if not exists parent_slug text,
    add column if not exists booking_url text,
    add column if not exists noise_level text,
    add column if not exists lighting text,
    add column if not exists wifi_quality text,
    add column if not exists hours_of_operation jsonb,
    add column if not exists time_zone text not null default 'America/Toronto';

alter table public.profiles
    add column if not exists login_streak int not null default 0,
    add column if not exists last_login_date date,
    add column if not exists longest_login_streak int not null default 0,
    add column if not exists checkout_count int not null default 0,
    add column if not exists location_visibility text not null default 'hidden';

alter table public.reviews
    add column if not exists quality_score int not null default 0;

alter table public.check_ins
    add column if not exists location_verified boolean not null default false,
    add column if not exists verified_distance_meters double precision,
    add column if not exists verified_at timestamptz;

alter table public.user_spot_submissions
    add column if not exists booking_url text;

create table if not exists public.study_sessions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    spot_id text not null,
    spot_name text not null,
    duration_seconds int not null check (duration_seconds >= 0),
    finished_at timestamptz not null default now()
);

create table if not exists public.user_badges (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    badge_id text not null,
    earned_at timestamptz not null default now(),
    unique (user_id, badge_id)
);

alter table public.study_sessions enable row level security;
alter table public.user_badges enable row level security;

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'spots_capacity_positive'
          and conrelid = 'public.spots'::regclass
    ) then
        alter table public.spots add constraint spots_capacity_positive
            check (capacity is null or capacity > 0);
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'spots_parent_slug_fkey'
          and conrelid = 'public.spots'::regclass
    ) then
        alter table public.spots add constraint spots_parent_slug_fkey
            foreign key (parent_slug) references public.spots(slug) on delete set null;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'check_ins_group_session_id_fkey'
          and conrelid = 'public.check_ins'::regclass
    ) then
        alter table public.check_ins add constraint check_ins_group_session_id_fkey
            foreign key (group_session_id)
            references public.group_sessions(id) on delete set null
            not valid;
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'friendships_distinct_users'
          and conrelid = 'public.friendships'::regclass
    ) then
        alter table public.friendships add constraint friendships_distinct_users
            check (requester_id <> addressee_id);
    end if;

    if not exists (
        select 1 from pg_constraint
        where conname = 'submissions_supported_uw_location'
          and conrelid = 'public.user_spot_submissions'::regclass
    ) then
        alter table public.user_spot_submissions
            add constraint submissions_supported_uw_location
            check (
                (latitude between 43.460 and 43.485 and longitude between -80.565 and -80.515)
                or (latitude between 43.445 and 43.458 and longitude between -80.510 and -80.488)
                or (latitude between 43.350 and 43.367 and longitude between -80.330 and -80.300)
            ) not valid;
    end if;
end
$$;

create index if not exists idx_spots_parent_slug
    on public.spots (parent_slug) where parent_slug is not null;
create index if not exists idx_check_ins_active_spot
    on public.check_ins (spot_slug, started_at desc) where ended_at is null;
create index if not exists idx_check_ins_user_spot_recent
    on public.check_ins (user_id, spot_slug, started_at desc);
create index if not exists idx_reviews_spot_created
    on public.reviews (spot_slug, created_at desc);
create index if not exists idx_reviews_user
    on public.reviews (user_id);
create index if not exists idx_friendships_requester_status
    on public.friendships (requester_id, status);
create index if not exists idx_friendships_addressee_status
    on public.friendships (addressee_id, status);
create index if not exists idx_study_sessions_user_finished
    on public.study_sessions (user_id, finished_at desc);

-- Retain the newest session if legacy clients created more than one active row.
with ranked_active as (
    select id,
           row_number() over (partition by user_id order by started_at desc, id desc) as row_number
    from public.check_ins
    where ended_at is null
)
update public.check_ins
set ended_at = now()
where id in (
    select id from ranked_active where row_number > 1
);

create unique index if not exists idx_check_ins_one_active_per_user
    on public.check_ins (user_id) where ended_at is null;
create unique index if not exists idx_friendships_unordered_pair
    on public.friendships (
        least(requester_id, addressee_id),
        greatest(requester_id, addressee_id)
    );

create or replace view public.spot_occupancy as
select spot_slug, count(*)::int as active_count
from public.check_ins
where ended_at is null
group by spot_slug;

create or replace view public.spot_trending as
select spot_slug, count(*)::int as checkins_7d
from public.check_ins
where started_at >= now() - interval '7 days'
group by spot_slug;

-- Replace accumulated policies on privacy-sensitive tables with one canonical set.
do $$
declare
    policy_row record;
begin
    for policy_row in
        select schemaname, tablename, policyname
        from pg_policies
        where schemaname = 'public'
          and tablename in (
              'profiles',
              'check_ins',
              'reviews',
              'friendships',
              'user_spot_submissions',
              'study_sessions',
              'user_badges'
          )
    loop
        execute format(
            'drop policy if exists %I on %I.%I',
            policy_row.policyname,
            policy_row.schemaname,
            policy_row.tablename
        );
    end loop;
end
$$;

create policy "profiles are private to their owner"
on public.profiles for select to authenticated
using ((select auth.uid()) = id);

create policy "users can insert their own profile"
on public.profiles for insert to authenticated
with check (
    (select auth.uid()) = id
    and lower(email) = lower(coalesce((select auth.jwt() ->> 'email'), ''))
);

create policy "users can update their own profile"
on public.profiles for update to authenticated
using ((select auth.uid()) = id)
with check (
    (select auth.uid()) = id
    and lower(email) = lower(coalesce((select auth.jwt() ->> 'email'), ''))
);

create policy "users can read their own check-ins"
on public.check_ins for select to authenticated
using ((select auth.uid()) = user_id);

create policy "users can end their own check-ins"
on public.check_ins for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy "verified visitors can submit reviews"
on public.reviews for insert to authenticated
with check (
    (select auth.uid()) = user_id
    and rating between 1 and 5
    and exists (
        select 1
        from public.check_ins check_in
        where check_in.user_id = (select auth.uid())
          and check_in.spot_slug = reviews.spot_slug
          and check_in.location_verified
          and check_in.started_at >= now() - interval '24 hours'
    )
);

create policy "users can update their own reviews"
on public.reviews for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy "users can delete their own reviews"
on public.reviews for delete to authenticated
using ((select auth.uid()) = user_id);

create policy "friendship parties can read"
on public.friendships for select to authenticated
using ((select auth.uid()) in (requester_id, addressee_id));

create policy "users can send friend requests"
on public.friendships for insert to authenticated
with check (
    (select auth.uid()) = requester_id
    and requester_id <> addressee_id
    and status = 'pending'
);

create policy "addressees can respond to friend requests"
on public.friendships for update to authenticated
using ((select auth.uid()) = addressee_id)
with check (
    (select auth.uid()) = addressee_id
    and requester_id <> addressee_id
);

create policy "users can submit supported UW spots"
on public.user_spot_submissions for insert to authenticated
with check (
    submitted_by_user_id = (select auth.uid())
    and lower(submitted_by_email) = lower(coalesce((select auth.jwt() ->> 'email'), ''))
    and (
        (latitude between 43.460 and 43.485 and longitude between -80.565 and -80.515)
        or (latitude between 43.445 and 43.458 and longitude between -80.510 and -80.488)
        or (latitude between 43.350 and 43.367 and longitude between -80.330 and -80.300)
    )
);

create policy "users can read their own spot submissions"
on public.user_spot_submissions for select to authenticated
using (submitted_by_user_id = (select auth.uid()));

create policy "users can read their own study sessions"
on public.study_sessions for select to authenticated
using ((select auth.uid()) = user_id);

create policy "users can create their own study sessions"
on public.study_sessions for insert to authenticated
with check ((select auth.uid()) = user_id);

create policy "users can read their own badges"
on public.user_badges for select to authenticated
using ((select auth.uid()) = user_id);

create policy "users can create their own badges"
on public.user_badges for insert to authenticated
with check ((select auth.uid()) = user_id);

drop function if exists public.is_classmate(uuid);

create or replace function public.safe_profiles(
    p_ids uuid[] default null,
    p_query text default null,
    p_program text default null,
    p_limit int default 20
)
returns table (
    id uuid,
    first_name text,
    last_name text,
    program text,
    study_term text
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    return query
    select profile.id,
           profile.first_name,
           profile.last_name,
           profile.program,
           coalesce(profile.study_term::text, '')
    from public.profiles profile
    where profile.id <> (select auth.uid())
      and (p_ids is null or profile.id = any(p_ids))
      and (p_program is null or profile.program = p_program)
      and (
          p_query is null
          or concat_ws(' ', profile.first_name, profile.last_name) ilike '%' || trim(p_query) || '%'
      )
    order by profile.first_name, profile.last_name
    limit least(greatest(coalesce(p_limit, 20), 1), 50);
end;
$$;

create or replace function public.start_verified_check_in(
    p_spot_slug text,
    p_mode text,
    p_group_session_id uuid,
    p_latitude double precision,
    p_longitude double precision
)
returns public.check_ins
language plpgsql
security definer
set search_path = ''
as $$
declare
    caller_id uuid := (select auth.uid());
    target_spot public.spots;
    distance_meters double precision;
    inserted_check_in public.check_ins;
begin
    if caller_id is null then
        raise exception 'Authentication required';
    end if;
    if p_mode not in ('solo', 'group') then
        raise exception 'Invalid study mode';
    end if;
    if not (p_latitude between -90 and 90)
       or not (p_longitude between -180 and 180) then
        raise exception 'Invalid location coordinates';
    end if;

    select * into target_spot
    from public.spots
    where slug = p_spot_slug;
    if not found then
        raise exception 'Study spot not found';
    end if;

    distance_meters := 6371000 * 2 * asin(
        least(1, sqrt(
            power(sin(radians(p_latitude - target_spot.latitude) / 2), 2)
            + cos(radians(target_spot.latitude))
            * cos(radians(p_latitude))
            * power(sin(radians(p_longitude - target_spot.longitude) / 2), 2)
        ))
    );
    if distance_meters > 250 then
        raise exception 'Move within 250 metres of this study spot to check in';
    end if;

    if p_mode = 'group' and (
        p_group_session_id is null
        or not exists (
            select 1 from public.group_session_members member
            join public.group_sessions session on session.id = member.session_id
            where member.session_id = p_group_session_id
              and member.user_id = caller_id
              and session.ended_at is null
        )
    ) then
        raise exception 'Join an active group before checking in as a group';
    end if;

    perform pg_advisory_xact_lock(hashtextextended(caller_id::text, 0));

    update public.check_ins
    set ended_at = now()
    where user_id = caller_id
      and ended_at is null;

    insert into public.check_ins (
        user_id,
        spot_slug,
        mode,
        group_session_id,
        location_verified,
        verified_distance_meters,
        verified_at
    )
    values (
        caller_id,
        p_spot_slug,
        p_mode,
        case when p_mode = 'group' then p_group_session_id else null end,
        true,
        distance_meters,
        now()
    )
    returning * into inserted_check_in;

    return inserted_check_in;
end;
$$;

create or replace function public.session_attendees(p_spot_slug text)
returns table (
    id uuid,
    first_name text,
    last_name text,
    program text,
    is_friend boolean,
    has_sent_me_request boolean
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    return query
    select profile.id,
           profile.first_name,
           profile.last_name,
           profile.program,
           coalesce(friendship.status = 'accepted', false) as is_friend,
           coalesce(
               friendship.status = 'pending'
               and friendship.requester_id = profile.id
               and friendship.addressee_id = (select auth.uid()),
               false
           ) as has_sent_me_request
    from public.check_ins check_in
    join public.profiles profile on profile.id = check_in.user_id
    left join public.friendships friendship
      on (
          (friendship.requester_id = (select auth.uid()) and friendship.addressee_id = profile.id)
          or
          (friendship.addressee_id = (select auth.uid()) and friendship.requester_id = profile.id)
      )
    where check_in.spot_slug = p_spot_slug
      and check_in.user_id <> (select auth.uid())
      and check_in.ended_at is null
      and check_in.started_at >= now() - interval '4 hours'
      and (
          (friendship.status = 'accepted' and profile.location_visibility = 'visible')
          or exists (
              select 1
              from public.check_ins own_check_in
              join public.group_session_members own_member
                on own_member.session_id = own_check_in.group_session_id
              join public.group_session_members peer_member
                on peer_member.session_id = own_member.session_id
               and peer_member.user_id = check_in.user_id
              where own_check_in.user_id = (select auth.uid())
                and own_check_in.ended_at is null
                and own_check_in.group_session_id is not null
          )
      );
end;
$$;

create or replace function public.create_group_session(
    p_title text,
    p_visibility text
)
returns public.group_sessions
language plpgsql
security definer
set search_path = ''
as $$
declare
    caller_id uuid := (select auth.uid());
    created_session public.group_sessions;
begin
    if caller_id is null then
        raise exception 'Authentication required';
    end if;
    if char_length(trim(p_title)) not between 2 and 50 then
        raise exception 'Group names must be between 2 and 50 characters';
    end if;
    if p_visibility not in ('private', 'public') then
        raise exception 'Invalid group visibility';
    end if;

    perform pg_advisory_xact_lock(hashtextextended(caller_id::text, 1));

    if exists (
        select 1
        from public.group_session_members member
        join public.group_sessions session on session.id = member.session_id
        where member.user_id = caller_id
          and session.ended_at is null
    ) then
        raise exception 'Leave your current group before creating another one';
    end if;

    insert into public.group_sessions (
        title, subtitle, visibility, created_by
    )
    values (
        trim(p_title),
        'Pick a spot and invite your friends',
        p_visibility,
        caller_id
    )
    returning * into created_session;

    insert into public.group_session_members (session_id, user_id, role)
    values (created_session.id, caller_id, 'owner');

    return created_session;
end;
$$;

create or replace function public.invite_group_member_by_email(
    p_group_id uuid,
    p_email text
)
returns table (
    id uuid,
    first_name text,
    last_name text,
    program text
)
language plpgsql
security definer
set search_path = ''
as $$
declare
    invited_profile public.profiles;
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;
    if not exists (
        select 1 from public.group_sessions
        where id = p_group_id
          and created_by = (select auth.uid())
          and ended_at is null
    ) then
        raise exception 'Only the active group owner can invite members';
    end if;

    select * into invited_profile
    from public.profiles
    where lower(email) = lower(trim(p_email));
    if not found then
        raise exception 'No Spotra account found for that email';
    end if;

    insert into public.group_session_members (session_id, user_id, role)
    values (p_group_id, invited_profile.id, 'member')
    on conflict (session_id, user_id) do nothing;

    return query
    select invited_profile.id,
           invited_profile.first_name,
           invited_profile.last_name,
           invited_profile.program;
end;
$$;

create or replace function public.list_spot_reviews(p_spot_slug text)
returns table (
    id uuid,
    spot_slug text,
    user_id uuid,
    reviewer_name text,
    rating int,
    noise_level text,
    lighting text,
    wifi_quality text,
    occupancy_percent int,
    comment text,
    anonymous boolean,
    quality_score int,
    is_owner boolean
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    return query
    select review.id,
           review.spot_slug,
           case when review.anonymous then null else review.user_id end,
           case when review.anonymous then 'Anonymous' else review.reviewer_name end,
           review.rating,
           review.noise_level,
           review.lighting,
           review.wifi_quality,
           review.occupancy_percent,
           review.comment,
           review.anonymous,
           review.quality_score,
           review.user_id = (select auth.uid())
    from public.reviews review
    where review.spot_slug = p_spot_slug
    order by review.created_at desc;
end;
$$;

create or replace function public.submit_review(
    p_spot_slug text,
    p_rating int,
    p_noise_level text,
    p_lighting text,
    p_wifi_quality text,
    p_occupancy_percent int,
    p_comment text,
    p_anonymous boolean,
    p_quality_score int
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    caller_id uuid := (select auth.uid());
    display_name text;
begin
    if caller_id is null then
        raise exception 'Authentication required';
    end if;
    if p_rating not between 1 and 5 then
        raise exception 'Rating must be between 1 and 5';
    end if;
    if p_occupancy_percent is not null and p_occupancy_percent not between 0 and 100 then
        raise exception 'Occupancy must be between 0 and 100';
    end if;
    if not exists (
        select 1
        from public.check_ins check_in
        where check_in.user_id = caller_id
          and check_in.spot_slug = p_spot_slug
          and check_in.location_verified
          and check_in.started_at >= now() - interval '24 hours'
    ) then
        raise exception 'Check in at this study spot before reviewing it';
    end if;

    select concat_ws(' ', first_name, last_name)
    into display_name
    from public.profiles
    where id = caller_id;

    insert into public.reviews (
        user_id,
        spot_slug,
        reviewer_name,
        rating,
        noise_level,
        lighting,
        wifi_quality,
        occupancy_percent,
        comment,
        anonymous,
        quality_score
    )
    values (
        caller_id,
        p_spot_slug,
        case when p_anonymous then 'Anonymous' else coalesce(nullif(display_name, ''), 'Student') end,
        p_rating,
        p_noise_level,
        p_lighting,
        p_wifi_quality,
        p_occupancy_percent,
        nullif(trim(coalesce(p_comment, '')), ''),
        p_anonymous,
        greatest(coalesce(p_quality_score, 0), 0)
    );
end;
$$;

create or replace function public.review_count(p_quality_only boolean default false)
returns int
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    return (
        select count(*)::int
        from public.reviews
        where user_id = (select auth.uid())
          and (not p_quality_only or quality_score >= 5)
    );
end;
$$;

create or replace function public.update_own_review(
    p_review_id uuid,
    p_rating int,
    p_noise_level text,
    p_lighting text,
    p_wifi_quality text,
    p_occupancy_percent int,
    p_comment text,
    p_anonymous boolean,
    p_quality_score int
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;
    if p_rating not between 1 and 5 then
        raise exception 'Rating must be between 1 and 5';
    end if;

    update public.reviews
    set rating = p_rating,
        noise_level = p_noise_level,
        lighting = p_lighting,
        wifi_quality = p_wifi_quality,
        occupancy_percent = p_occupancy_percent,
        comment = p_comment,
        anonymous = p_anonymous,
        reviewer_name = case
            when p_anonymous then 'Anonymous'
            else coalesce(
                (
                    select nullif(concat_ws(' ', profile.first_name, profile.last_name), '')
                    from public.profiles profile
                    where profile.id = (select auth.uid())
                ),
                'Student'
            )
        end,
        quality_score = greatest(coalesce(p_quality_score, 0), 0)
    where id = p_review_id
      and user_id = (select auth.uid());

    if not found then
        raise exception 'Review not found or not owned by the current user';
    end if;
end;
$$;

create or replace function public.delete_own_review(p_review_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    delete from public.reviews
    where id = p_review_id
      and user_id = (select auth.uid());

    if not found then
        raise exception 'Review not found or not owned by the current user';
    end if;
end;
$$;

revoke all on table public.profiles from anon;
revoke all on table public.profiles from authenticated;
grant select, insert, update on table public.profiles to authenticated;

revoke all on table public.check_ins from anon;
revoke all on table public.check_ins from authenticated;
grant select, update (ended_at) on table public.check_ins to authenticated;

revoke all on table public.reviews from anon;
revoke all on table public.reviews from authenticated;

revoke all on table public.friendships from anon;
revoke all on table public.friendships from authenticated;
grant select, insert, update on table public.friendships to authenticated;

revoke all on table public.user_spot_submissions from anon;
revoke all on table public.user_spot_submissions from authenticated;
grant select, insert on table public.user_spot_submissions to authenticated;

revoke all on table public.study_sessions from anon;
revoke all on table public.study_sessions from authenticated;
grant select, insert on table public.study_sessions to authenticated;

revoke all on table public.user_badges from anon;
revoke all on table public.user_badges from authenticated;
grant select, insert on table public.user_badges to authenticated;

revoke all on table public.spot_occupancy from public, anon;
grant select on table public.spot_occupancy to authenticated;
revoke all on table public.spot_trending from public, anon;
grant select on table public.spot_trending to authenticated;

revoke insert on table public.group_sessions from authenticated;
grant select, update on table public.group_sessions to authenticated;

revoke all on function public.safe_profiles(uuid[], text, text, int) from public, anon;
grant execute on function public.safe_profiles(uuid[], text, text, int) to authenticated;
revoke all on function public.start_verified_check_in(text, text, uuid, double precision, double precision) from public, anon;
grant execute on function public.start_verified_check_in(text, text, uuid, double precision, double precision) to authenticated;
revoke all on function public.session_attendees(text) from public, anon;
grant execute on function public.session_attendees(text) to authenticated;
revoke all on function public.create_group_session(text, text) from public, anon;
grant execute on function public.create_group_session(text, text) to authenticated;
revoke all on function public.invite_group_member_by_email(uuid, text) from public, anon;
grant execute on function public.invite_group_member_by_email(uuid, text) to authenticated;
revoke all on function public.list_spot_reviews(text) from public, anon;
grant execute on function public.list_spot_reviews(text) to authenticated;
revoke all on function public.submit_review(text, int, text, text, text, int, text, boolean, int) from public, anon;
grant execute on function public.submit_review(text, int, text, text, text, int, text, boolean, int) to authenticated;
revoke all on function public.review_count(boolean) from public, anon;
grant execute on function public.review_count(boolean) to authenticated;
revoke all on function public.update_own_review(uuid, int, text, text, text, int, text, boolean, int) from public, anon;
grant execute on function public.update_own_review(uuid, int, text, text, text, int, text, boolean, int) to authenticated;
revoke all on function public.delete_own_review(uuid) from public, anon;
grant execute on function public.delete_own_review(uuid) to authenticated;

notify pgrst, 'reload schema';
