create or replace function public.friends_all_spots()
returns table (
    spot_slug  text,
    id         uuid,
    first_name text,
    last_name  text
)
language sql
security definer
set search_path = public
stable
as $$
    select ci.spot_slug, p.id, p.first_name, p.last_name
    from public.check_ins ci
    join public.friendships f on (
        (f.requester_id = auth.uid() and f.addressee_id = ci.user_id)
        or
        (f.addressee_id = auth.uid() and f.requester_id = ci.user_id)
    )
    join public.profiles p on p.id = ci.user_id
    where ci.ended_at is null
      and ci.started_at >= now() - interval '2 hours'
      and f.status = 'accepted'
      and ci.user_id <> auth.uid()
      and p.location_visibility = 'visible';
$$;

grant execute on function public.friends_all_spots() to authenticated;
revoke execute on function public.friends_all_spots() from anon;
