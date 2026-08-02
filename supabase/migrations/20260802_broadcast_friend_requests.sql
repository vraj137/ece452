-- Allow authenticated users to receive friend-request broadcasts on their own channel.
drop policy if exists "users can receive their own friend request events" on realtime.messages;
create policy "users can receive their own friend request events"
on realtime.messages
for select
to authenticated
using (topic = 'friend-requests-' || auth.uid()::text);

-- Broadcast to the addressee whenever a new pending friend request arrives.
create or replace function public.broadcast_friend_request()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    perform realtime.send(
        pg_catalog.jsonb_build_object('from_user_id', new.requester_id::text, 'status', 'pending'),
        'friend_request_received',
        'friend-requests-' || new.addressee_id::text,
        true
    );
    return null;
end;
$$;

revoke all on function public.broadcast_friend_request() from public, anon, authenticated;

drop trigger if exists trg_broadcast_friend_request on public.friendships;
create trigger trg_broadcast_friend_request
after insert
on public.friendships
for each row
when (new.status = 'pending')
execute function public.broadcast_friend_request();
