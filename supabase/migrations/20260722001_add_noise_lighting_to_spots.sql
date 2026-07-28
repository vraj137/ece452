alter table public.spots
    add column if not exists capacity    int,
    add column if not exists parent_slug text,
    add column if not exists noise_level text,
    add column if not exists lighting    text,
    add column if not exists wifi_quality text;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'spots_capacity_positive'
          and conrelid = 'public.spots'::regclass
    ) then
        alter table public.spots
            add constraint spots_capacity_positive
            check (capacity is null or capacity > 0);
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'spots_parent_slug_fkey'
          and conrelid = 'public.spots'::regclass
    ) then
        alter table public.spots
            add constraint spots_parent_slug_fkey
            foreign key (parent_slug) references public.spots(slug) on delete set null;
    end if;
end
$$;

create index if not exists idx_spots_parent_slug
    on public.spots (parent_slug)
    where parent_slug is not null;

update public.spots set noise_level = 'Low',      lighting = 'Good'    where slug = 'e7-study-hall';
update public.spots set noise_level = 'Silent',   lighting = 'Natural' where slug = 'dc-library-3f';
update public.spots set noise_level = 'Moderate', lighting = 'Bright'  where slug = 'slc-boardroom-2a';
update public.spots set noise_level = 'Moderate', lighting = 'Natural' where slug = 'mc-atrium';
update public.spots set noise_level = 'Lively',   lighting = 'Poor'    where slug = 'e5-collab-lab';
update public.spots set noise_level = 'Silent',   lighting = 'Bright'  where slug = 'dp-library';

update public.spots set capacity = 30  where slug = 'e7-study-hall'    and capacity is null;
update public.spots set capacity = 30  where slug = 'dc-library-3f'    and capacity is null;
update public.spots set capacity = 8   where slug = 'slc-boardroom-2a' and capacity is null;
update public.spots set capacity = 40  where slug = 'mc-atrium'        and capacity is null;
update public.spots set capacity = 20  where slug = 'e5-collab-lab'    and capacity is null;
update public.spots set capacity = 30  where slug = 'dp-library'       and capacity is null;
