-- Real-time live updates for friend requests and public groups.
-- Allow each user to receive events from their own friend requests channel
drop policy if exists "users can receive their own friend request notifications" on realtime.messages;
create policy "users can receive their own friend request notifications"
on realtime.messages
for select
to authenticated
using (topic = 'friend-requests-' || (select auth.uid())::text);

-- Broadcast when a friendship row is inserted, for example if a new req is sent
create or replace function public.broadcast_friend_request()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    perform realtime.send(
        pg_catalog.jsonb_build_object(
            'from_user_id', new.requester_id::text,
            'status', new.status
        ),
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
execute function public.broadcast_friend_request();

drop policy if exists "authenticated users can receive public groups updates" on realtime.messages;
create policy "authenticated users can receive public groups updates"
on realtime.messages
for select
to authenticated
using (topic = 'public-groups');

create or replace function public.broadcast_public_group_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if tg_op = 'INSERT' and new.visibility = 'public' and new.ended_at is null then
        perform realtime.send(
            pg_catalog.jsonb_build_object('event', 'group_created', 'session_id', new.id::text),
            'public_groups_changed',
            'public-groups',
            false
        );
    elsif tg_op = 'UPDATE' and old.ended_at is null and new.ended_at is not null and new.visibility = 'public' then
        perform realtime.send(
            pg_catalog.jsonb_build_object('event', 'group_ended', 'session_id', new.id::text),
            'public_groups_changed',
            'public-groups',
            false
        );
    end if;
    return null;
end;
$$;

revoke all on function public.broadcast_public_group_change() from public, anon, authenticated;

drop trigger if exists trg_broadcast_public_group_change on public.group_sessions;
create trigger trg_broadcast_public_group_change
after insert or update of ended_at
on public.group_sessions
for each row
execute function public.broadcast_public_group_change();
