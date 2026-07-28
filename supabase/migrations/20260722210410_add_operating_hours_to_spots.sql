alter table public.spots
    add column if not exists hours_of_operation jsonb,
    add column if not exists time_zone text not null default 'America/Toronto';

comment on column public.spots.hours_of_operation is
    'Weekly local operating hours keyed by lowercase weekday. Example: {"monday":{"opens":"08:00","closes":"23:00"},"sunday":null}. Use {"open_24_hours":true} for a 24-hour day; null means closed.';

comment on column public.spots.time_zone is
    'IANA time-zone identifier used to interpret hours_of_operation. Waterloo locations use America/Toronto so EST/EDT changes are handled automatically.';

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'spots_hours_of_operation_is_object'
          and conrelid = 'public.spots'::regclass
    ) then
        alter table public.spots
            add constraint spots_hours_of_operation_is_object
            check (
                hours_of_operation is null
                or jsonb_typeof(hours_of_operation) = 'object'
            );
    end if;
end
$$;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'spots_time_zone_not_blank'
          and conrelid = 'public.spots'::regclass
    ) then
        alter table public.spots
            add constraint spots_time_zone_not_blank
            check (btrim(time_zone) <> '');
    end if;
end
$$;
