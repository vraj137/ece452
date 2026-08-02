-- Friends who are both actively checked into the same spot should always see each other
-- in the live session attendee list, regardless of location_visibility.
create or replace function public.session_attendees(p_spot_slug text)
returns table (
    id uuid,
    first_name text,
    last_name text,
    program text,
    is_friend boolean,
    has_sent_me_request boolean
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    return query
    select profile.id,
           profile.first_name,
           profile.last_name,
           profile.program,
           coalesce(friendship.status = 'accepted', false) as is_friend,
           coalesce(
               friendship.status = 'pending'
               and friendship.requester_id = profile.id
               and friendship.addressee_id = (select auth.uid()),
               false
           ) as has_sent_me_request
    from public.check_ins check_in
    join public.profiles profile on profile.id = check_in.user_id
    left join public.friendships friendship
      on (
          (friendship.requester_id = (select auth.uid()) and friendship.addressee_id = profile.id)
          or
          (friendship.addressee_id = (select auth.uid()) and friendship.requester_id = profile.id)
      )
    where check_in.spot_slug = p_spot_slug
      and check_in.user_id <> (select auth.uid())
      and check_in.ended_at is null
      and check_in.started_at >= now() - interval '4 hours'
      and (
          -- accepted friends always see each other when co-located, no visibility gate
          friendship.status = 'accepted'
          or exists (
              select 1
              from public.check_ins own_check_in
              join public.group_session_members own_member
                on own_member.session_id = own_check_in.group_session_id
              join public.group_session_members peer_member
                on peer_member.session_id = own_member.session_id
               and peer_member.user_id = check_in.user_id
              where own_check_in.user_id = (select auth.uid())
                and own_check_in.ended_at is null
                and own_check_in.group_session_id is not null
          )
      );
end;
$$;
