alter table public.spots
    add column if not exists noise_level text,
    add column if not exists lighting    text;

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
