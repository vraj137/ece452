-- Keep exact study-space occupancy for detail screens while rolling child spaces up to
-- their building card for Explore. Hidden check-ins remain anonymous but still count.
create or replace view public.spot_trending
as
select
    target.target_slug as spot_slug,
    count(*)::int as checkins_7d
from public.check_ins as check_in
join public.spots as checked_spot
  on checked_spot.slug = check_in.spot_slug
cross join lateral (
    select checked_spot.slug as target_slug
    union all
    select checked_spot.parent_slug
    where checked_spot.parent_slug is not null
) as target
where check_in.started_at >= now() - interval '7 days'
group by target.target_slug;

revoke all on table public.spot_trending from public, anon, authenticated;
grant select on table public.spot_trending to authenticated;

create index if not exists idx_check_ins_spot_started_at
on public.check_ins (spot_slug, started_at desc);

insert into public.spot_occupancy_counts (spot_slug, active_count, updated_at)
select
    target_spot.slug,
    (
        select count(*)::int
        from public.check_ins as check_in
        join public.spots as checked_spot
          on checked_spot.slug = check_in.spot_slug
        where check_in.ended_at is null
          and (
              checked_spot.slug = target_spot.slug
              or (
                  target_spot.parent_slug is null
                  and checked_spot.parent_slug = target_spot.slug
              )
          )
    ),
    now()
from public.spots as target_spot
on conflict (spot_slug) do update
set active_count = excluded.active_count,
    updated_at = excluded.updated_at;

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
    current_count int;
    spot_capacity int;
    target_parent_slug text;
    weekly_check_ins int;
begin
    if p_delta = 0 then
        return;
    end if;

    select spot.capacity, spot.parent_slug
    into spot_capacity, target_parent_slug
    from public.spots as spot
    where spot.slug = p_spot_slug;

    if not found then
        return;
    end if;

    insert into public.spot_occupancy_counts (spot_slug, active_count, updated_at)
    values (p_spot_slug, greatest(p_delta, 0), now())
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
    join public.spots as checked_spot
      on checked_spot.slug = check_in.spot_slug
    where check_in.started_at >= now() - interval '7 days'
      and (
          checked_spot.slug = p_spot_slug
          or (
              target_parent_slug is null
              and checked_spot.parent_slug = p_spot_slug
          )
      );

    perform realtime.send(
        pg_catalog.jsonb_build_object(
            'spot_slug', p_spot_slug,
            'active_count', current_count,
            'capacity', spot_capacity,
            'checkins_7d', weekly_check_ins
        ),
        'occupancy_changed',
        'spot-occupancy',
        true
    );
end;
$$;

revoke all on function public.publish_spot_occupancy_delta(text, int)
from public, anon, authenticated;

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
    old_parent_slug text;
    new_parent_slug text;
begin
    if tg_op <> 'INSERT' then
        select spot.parent_slug
        into old_parent_slug
        from public.spots as spot
        where spot.slug = old.spot_slug;
    end if;

    if tg_op <> 'DELETE' then
        select spot.parent_slug
        into new_parent_slug
        from public.spots as spot
        where spot.slug = new.spot_slug;
    end if;

    if tg_op = 'INSERT' then
        if new.ended_at is null then
            perform public.publish_spot_occupancy_delta(new.spot_slug, 1);
            if new_parent_slug is not null then
                perform public.publish_spot_occupancy_delta(new_parent_slug, 1);
            end if;
        end if;
        return null;
    end if;

    if tg_op = 'DELETE' then
        if old.ended_at is null then
            perform public.publish_spot_occupancy_delta(old.spot_slug, -1);
            if old_parent_slug is not null then
                perform public.publish_spot_occupancy_delta(old_parent_slug, -1);
            end if;
        end if;
        return null;
    end if;

    old_is_active := old.ended_at is null;
    new_is_active := new.ended_at is null;

    if old.spot_slug is distinct from new.spot_slug then
        if old_is_active then
            perform public.publish_spot_occupancy_delta(old.spot_slug, -1);
            if old_parent_slug is not null then
                perform public.publish_spot_occupancy_delta(old_parent_slug, -1);
            end if;
        end if;
        if new_is_active then
            perform public.publish_spot_occupancy_delta(new.spot_slug, 1);
            if new_parent_slug is not null then
                perform public.publish_spot_occupancy_delta(new_parent_slug, 1);
            end if;
        end if;
    else
        occupancy_delta :=
            (case when new_is_active then 1 else 0 end) -
            (case when old_is_active then 1 else 0 end);

        perform public.publish_spot_occupancy_delta(new.spot_slug, occupancy_delta);
        if new_parent_slug is not null then
            perform public.publish_spot_occupancy_delta(new_parent_slug, occupancy_delta);
        end if;
    end if;

    return null;
end;
$$;

revoke all on function public.broadcast_spot_occupancy_change()
from public, anon, authenticated;
