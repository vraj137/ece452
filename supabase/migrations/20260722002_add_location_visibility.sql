alter table public.profiles
    add column if not exists location_visibility text not null default 'hidden'
    check (location_visibility in ('visible', 'approximate', 'hidden'));

drop policy if exists "Authenticated users can read all profiles" on public.profiles;
create policy "Authenticated users can read all profiles"
on public.profiles for select
to authenticated
using (true);
