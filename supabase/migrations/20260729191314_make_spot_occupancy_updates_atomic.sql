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
begin
    if p_delta = 0 or not exists (
        select 1
        from public.spots
        where slug = p_spot_slug
    ) then
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

    select capacity
    into spot_capacity
    from public.spots
    where slug = p_spot_slug;

    perform realtime.send(
        pg_catalog.jsonb_build_object(
            'spot_slug', p_spot_slug,
            'active_count', current_count,
            'capacity', spot_capacity
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
