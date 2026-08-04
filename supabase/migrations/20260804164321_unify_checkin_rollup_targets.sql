-- One canonical mapping from a checked-in room to every spot whose aggregate should change.
-- UNION (rather than UNION ALL) also protects malformed self-parent rows from double counting.
create or replace function private.checkin_rollup_targets(p_spot_slug text)
returns table (target_slug text)
language sql
stable
security invoker
set search_path = ''
rows 2
as $$
    select spot.slug
    from public.spots as spot
    where spot.slug = p_spot_slug

    union

    select spot.parent_slug
    from public.spots as spot
    where spot.slug = p_spot_slug
      and spot.parent_slug is not null;
$$;

revoke all on function private.checkin_rollup_targets(text)
from public, anon, authenticated, service_role;

-- Raw check-in rows stay private. Signed-in clients can execute only these aggregate
-- functions indirectly through security-invoker views in the public schema.
create or replace function private.spot_occupancy_rollup()
returns table (
    spot_slug text,
    active_count int
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        target.target_slug as spot_slug,
        count(*)::int as active_count
    from public.check_ins as check_in
    cross join lateral private.checkin_rollup_targets(check_in.spot_slug) as target
    where check_in.ended_at is null
    group by target.target_slug;
$$;

create or replace function private.spot_trending_rollup()
returns table (
    spot_slug text,
    checkins_7d int
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        target.target_slug as spot_slug,
        count(*)::int as checkins_7d
    from public.check_ins as check_in
    cross join lateral private.checkin_rollup_targets(check_in.spot_slug) as target
    where check_in.started_at >= now() - interval '7 days'
    group by target.target_slug;
$$;

revoke all on function private.spot_occupancy_rollup()
from public, anon, authenticated, service_role;
revoke all on function private.spot_trending_rollup()
from public, anon, authenticated, service_role;
grant usage on schema private to authenticated, service_role;
grant execute on function private.spot_occupancy_rollup()
to authenticated, service_role;
grant execute on function private.spot_trending_rollup()
to authenticated, service_role;

create or replace view public.spot_occupancy
with (security_invoker = true)
as
select *
from private.spot_occupancy_rollup();

create or replace view public.spot_trending
with (security_invoker = true)
as
select *
from private.spot_trending_rollup();

revoke all on table public.spot_occupancy
from public, anon, authenticated, service_role;
revoke all on table public.spot_trending
from public, anon, authenticated, service_role;
grant select on table public.spot_occupancy to authenticated, service_role;
grant select on table public.spot_trending to authenticated, service_role;

-- Bring the realtime counter cache back to the same canonical roll-up before new deltas land.
insert into public.spot_occupancy_counts (spot_slug, active_count, updated_at)
select
    spot.slug,
    coalesce(occupancy.active_count, 0),
    now()
from public.spots as spot
left join private.spot_occupancy_rollup() as occupancy
  on occupancy.spot_slug = spot.slug
on conflict (spot_slug) do update
set active_count = excluded.active_count,
    updated_at = excluded.updated_at;

-- A delta now starts from the checked-in room and updates/broadcasts every canonical target.
create or replace function public.publish_spot_occupancy_delta(
    p_spot_slug text,
    p_delta int
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    target record;
    current_count int;
    target_capacity int;
    weekly_check_ins int;
begin
    if p_delta = 0 then
        return;
    end if;

    for target in
        select rollup.target_slug
        from private.checkin_rollup_targets(p_spot_slug) as rollup
    loop
        select spot.capacity
        into target_capacity
        from public.spots as spot
        where spot.slug = target.target_slug;

        insert into public.spot_occupancy_counts (spot_slug, active_count, updated_at)
        values (target.target_slug, greatest(p_delta, 0), now())
        on conflict (spot_slug) do update
        set active_count = greatest(
                0,
                public.spot_occupancy_counts.active_count + p_delta
            ),
            updated_at = excluded.updated_at
        returning active_count into current_count;

        select count(*)::int
        into weekly_check_ins
        from public.check_ins as check_in
        cross join lateral private.checkin_rollup_targets(check_in.spot_slug) as rollup
        where check_in.started_at >= now() - interval '7 days'
          and rollup.target_slug = target.target_slug;

        perform realtime.send(
            pg_catalog.jsonb_build_object(
                'spot_slug', target.target_slug,
                'active_count', current_count,
                'capacity', target_capacity,
                'checkins_7d', weekly_check_ins
            ),
            'occupancy_changed',
            'spot-occupancy',
            true
        );
    end loop;
end;
$$;

revoke all on function public.publish_spot_occupancy_delta(text, int)
from public, anon, authenticated;

-- Parent lookup and fan-out now live exclusively in checkin_rollup_targets().
create or replace function public.broadcast_spot_occupancy_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    old_is_active boolean;
    new_is_active boolean;
    occupancy_delta int;
begin
    if tg_op = 'INSERT' then
        if new.ended_at is null then
            perform public.publish_spot_occupancy_delta(new.spot_slug, 1);
        end if;
        return null;
    end if;

    if tg_op = 'DELETE' then
        if old.ended_at is null then
            perform public.publish_spot_occupancy_delta(old.spot_slug, -1);
        end if;
        return null;
    end if;

    old_is_active := old.ended_at is null;
    new_is_active := new.ended_at is null;

    if old.spot_slug is distinct from new.spot_slug then
        if old_is_active then
            perform public.publish_spot_occupancy_delta(old.spot_slug, -1);
        end if;
        if new_is_active then
            perform public.publish_spot_occupancy_delta(new.spot_slug, 1);
        end if;
    else
        occupancy_delta :=
            (case when new_is_active then 1 else 0 end) -
            (case when old_is_active then 1 else 0 end);

        perform public.publish_spot_occupancy_delta(new.spot_slug, occupancy_delta);
    end if;

    return null;
end;
$$;

revoke all on function public.broadcast_spot_occupancy_change()
from public, anon, authenticated;
