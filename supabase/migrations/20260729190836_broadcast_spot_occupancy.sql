create table if not exists public.spot_occupancy_counts (
    spot_slug text primary key references public.spots(slug) on delete cascade,
    active_count int not null default 0 check (active_count >= 0),
    updated_at timestamptz not null default now()
);

alter table public.spot_occupancy_counts enable row level security;

drop policy if exists "authenticated users can read spot occupancy" on public.spot_occupancy_counts;

create policy "authenticated users can read spot occupancy"
on public.spot_occupancy_counts
for select
to authenticated
using (true);

revoke all on table public.spot_occupancy_counts from public, anon, authenticated;
grant select on table public.spot_occupancy_counts to authenticated;

insert into public.spot_occupancy_counts (spot_slug, active_count, updated_at)
select
    spot.slug,
    count(check_in.id) filter (where check_in.ended_at is null)::int,
    now()
from public.spots spot
left join public.check_ins check_in on check_in.spot_slug = spot.slug
group by spot.slug
on conflict (spot_slug) do update
set active_count = excluded.active_count,
    updated_at = excluded.updated_at;

create or replace view public.spot_occupancy
with (security_invoker = true)
as
select spot_slug, active_count
from public.spot_occupancy_counts;

revoke all on table public.spot_occupancy from public, anon, authenticated;
grant select on table public.spot_occupancy to authenticated;

drop policy if exists "authenticated users can receive spot occupancy" on realtime.messages;

create policy "authenticated users can receive spot occupancy"
on realtime.messages
for select
to authenticated
using (topic = 'spot-occupancy');

create or replace function public.broadcast_spot_occupancy_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    affected_spot text;
    affected_spots text[];
    current_count int;
    spot_capacity int;
    spot_exists boolean;
begin
    if tg_op = 'INSERT' then
        affected_spots := array[new.spot_slug];
    elsif tg_op = 'DELETE' then
        affected_spots := array[old.spot_slug];
    elsif old.spot_slug is distinct from new.spot_slug then
        affected_spots := array[old.spot_slug, new.spot_slug];
    else
        affected_spots := array[new.spot_slug];
    end if;

    foreach affected_spot in array affected_spots
    loop
        select count(*)::int
        into current_count
        from public.check_ins
        where spot_slug = affected_spot
          and ended_at is null;

        select exists (
            select 1
            from public.spots
            where slug = affected_spot
        )
        into spot_exists;

        if not spot_exists then
            continue;
        end if;

        select capacity
        into spot_capacity
        from public.spots
        where slug = affected_spot;

        insert into public.spot_occupancy_counts (spot_slug, active_count, updated_at)
        values (affected_spot, current_count, now())
        on conflict (spot_slug) do update
        set active_count = excluded.active_count,
            updated_at = excluded.updated_at;

        perform realtime.send(
            pg_catalog.jsonb_build_object(
                'spot_slug', affected_spot,
                'active_count', current_count,
                'capacity', spot_capacity
            ),
            'occupancy_changed',
            'spot-occupancy',
            true
        );
    end loop;

    return null;
end;
$$;

revoke all on function public.broadcast_spot_occupancy_change() from public, anon, authenticated;

drop trigger if exists broadcast_spot_occupancy_change on public.check_ins;

create trigger broadcast_spot_occupancy_change
after insert or delete or update of ended_at, spot_slug
on public.check_ins
for each row
execute function public.broadcast_spot_occupancy_change();
