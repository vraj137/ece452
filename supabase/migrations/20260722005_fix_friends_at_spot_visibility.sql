create or replace function public.friends_at_spot(p_spot_slug text)
returns table (
    id uuid,
    first_name text,
    last_name text
)
language sql
security definer
set search_path = ''
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
      and auth.uid() is not null
      and ci.ended_at is null
      and ci.started_at >= now() - interval '2 hours'
      and f.status = 'accepted'
      and ci.user_id <> auth.uid()
      and p.location_visibility = 'visible';
$$;

revoke execute on function public.friends_at_spot(text) from public;
revoke execute on function public.friends_at_spot(text) from anon;
grant execute on function public.friends_at_spot(text) to authenticated;

notify pgrst, 'reload schema';
