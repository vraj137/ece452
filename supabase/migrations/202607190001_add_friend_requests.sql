-- Social discovery only reveals classmates who share both program and study term.
-- Email addresses remain protected by the profiles table and are never returned by the app.
create or replace function public.is_classmate(candidate_program text, candidate_study_term text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.profiles me
        where me.id = auth.uid()
          and me.program = candidate_program
          and me.study_term = candidate_study_term
    );
$$;

grant execute on function public.is_classmate(text, text) to authenticated;

create policy "Users can discover classmates in their program and term"
on public.profiles for select
to authenticated
using (public.is_classmate(program, study_term));

create table if not exists public.friend_requests (
    requester_id uuid not null references public.profiles(id) on delete cascade,
    recipient_id uuid not null references public.profiles(id) on delete cascade,
    status text not null default 'pending' check (status in ('pending', 'accepted')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (requester_id, recipient_id),
    check (requester_id <> recipient_id)
);

alter table public.friend_requests enable row level security;

create policy "Users can view their friend requests"
on public.friend_requests for select
to authenticated
using ((select auth.uid()) in (requester_id, recipient_id));

create policy "Users can send friend requests"
on public.friend_requests for insert
to authenticated
with check ((select auth.uid()) = requester_id and status = 'pending');

create policy "Recipients can accept friend requests"
on public.friend_requests for update
to authenticated
using ((select auth.uid()) = recipient_id and status = 'pending')
with check ((select auth.uid()) = recipient_id and status = 'accepted');

grant select, insert, update on public.friend_requests to authenticated;
revoke all on public.friend_requests from anon;

create trigger friend_requests_set_updated_at
before update on public.friend_requests
for each row execute function public.set_updated_at();
