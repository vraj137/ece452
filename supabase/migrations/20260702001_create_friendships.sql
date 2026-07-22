
create table if not exists public.friendships (
    id uuid primary key default gen_random_uuid(),
    requester_id uuid not null references auth.users(id) on delete cascade,
    addressee_id uuid not null references auth.users(id) on delete cascade,
    status text not null default 'pending' check (status in ('pending', 'accepted', 'declined')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint unique_pair unique (requester_id, addressee_id)
);

alter table public.friendships enable row level security;

create policy "Parties can read their friendships"
on public.friendships for select
to authenticated
using (auth.uid() = requester_id or auth.uid() = addressee_id);

create policy "Requester can send friend requests"
on public.friendships for insert
to authenticated
with check (auth.uid() = requester_id);

create policy "Addressee can respond to requests"
on public.friendships for update
to authenticated
using (auth.uid() = addressee_id)
with check (auth.uid() = addressee_id);

grant select, insert, update on public.friendships to authenticated;
revoke all on public.friendships from anon;

create trigger friendships_set_updated_at
before update on public.friendships
for each row execute function public.set_updated_at();

create or replace function public.friends_at_spot(p_spot_slug text)
returns table (
    id uuid,
    first_name text,
    last_name text
)
language sql
security definer
set search_path = public
stable
as $$
    select p.id, p.first_name, p.last_name
    from public.check_ins ci
    join public.friendships f on (
        (f.requester_id = auth.uid() and f.addressee_id = ci.user_id)
        or
        (f.addressee_id = auth.uid() and f.requester_id = ci.user_id)
    )
    join public.profiles p on p.id = ci.user_id
    where ci.spot_slug = p_spot_slug
      and ci.ended_at is null
      and ci.started_at >= now() - interval '2 hours'
      and f.status = 'accepted'
      and ci.user_id <> auth.uid();
$$;

grant execute on function public.friends_at_spot(text) to authenticated;
revoke execute on function public.friends_at_spot(text) from anon;
