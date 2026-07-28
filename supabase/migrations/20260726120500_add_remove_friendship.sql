-- Lets a user unfriend someone, or cancel a friend request they sent.
--
-- Neither was possible before: 20260702001_create_friendships.sql grants only
-- (select, insert, update) on public.friendships, and the update policy is scoped to the
-- addressee — so a requester could not even withdraw their own pending row.
--
-- Deleting rather than marking 'declined' is deliberate. The unordered-pair unique index on
-- friendships would otherwise block the two users from ever connecting again.
--
-- Follows the RPC convention established in 20260726054502_stabilize_core_privacy_and_reviews.sql
-- for anything relationship-sensitive: security definer, pinned search_path, execute granted only
-- to authenticated.

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
