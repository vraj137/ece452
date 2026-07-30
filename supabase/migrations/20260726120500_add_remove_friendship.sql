-- Lets a user unfriend someone, or cancel a friend request they sent.
create or replace function public.remove_friendship(p_friendship_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    -- Silently a no-op for a caller who is not party to the friendship: the row count is never
    -- returned, so this cannot be used to probe which friendship ids exist.
    delete from public.friendships
    where id = p_friendship_id
      and ((select auth.uid()) in (requester_id, addressee_id));
end;
$$;

revoke all on function public.remove_friendship(uuid) from public, anon;
grant execute on function public.remove_friendship(uuid) to authenticated;
