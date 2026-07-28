create table if not exists public.user_spot_submissions (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    description text not null default '',
    latitude double precision not null,
    longitude double precision not null,
    building text not null default '',
    floor text not null default '',
    booking_url text,
    submitted_by_email text not null default '',
    submitted_by_user_id uuid references auth.users(id) on delete set null,
    status text not null default 'pending',
    created_at timestamptz not null default now()
);

alter table public.user_spot_submissions enable row level security;

create policy "Authenticated users can submit spots"
on public.user_spot_submissions for insert
to authenticated
with check (true);

create policy "Users can view their own submissions"
on public.user_spot_submissions for select
to authenticated
using (submitted_by_user_id = auth.uid());

grant insert, select on public.user_spot_submissions to authenticated;
