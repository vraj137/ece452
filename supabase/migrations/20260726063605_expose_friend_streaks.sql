-- Expose streak counts only across accepted friendships.
create or replace function public.friend_streaks()
returns table (
    user_id uuid,
    login_streak int
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
           greatest(profile.login_streak, 0)
    from public.friendships friendship
    join public.profiles profile
      on profile.id = case
          when friendship.requester_id = (select auth.uid())
              then friendship.addressee_id
          else friendship.requester_id
      end
    where friendship.status = 'accepted'
      and (
          friendship.requester_id = (select auth.uid())
          or friendship.addressee_id = (select auth.uid())
      );
end;
$$;

revoke all on function public.friend_streaks() from public, anon;
grant execute on function public.friend_streaks() to authenticated;

notify pgrst, 'reload schema';
