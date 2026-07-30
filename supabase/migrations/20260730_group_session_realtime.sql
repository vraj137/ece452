-- Realtime broadcasts for group session membership changes and session end.

drop policy if exists "group members can receive group session events" on realtime.messages;
create policy "group members can receive group session events"
on realtime.messages
for select
to authenticated
using (
    topic like 'group-session-%'
    and exists (
        select 1
        from public.group_session_members m
        where 'group-session-' || m.session_id::text = topic
          and m.user_id = auth.uid()
    )
);

-- Trigger: broadcast when a member joins or leaves
create or replace function public.broadcast_group_member_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    target_session_id uuid;
begin
    target_session_id := coalesce(new.session_id, old.session_id);
    perform realtime.send(
        pg_catalog.jsonb_build_object('session_id', target_session_id::text),
        'members_changed',
        'group-session-' || target_session_id::text,
        true
    );
    return null;
end;
$$;

revoke all on function public.broadcast_group_member_change() from public, anon, authenticated;

drop trigger if exists trg_broadcast_group_member_change on public.group_session_members;
create trigger trg_broadcast_group_member_change
after insert or delete
on public.group_session_members
for each row
execute function public.broadcast_group_member_change();

-- Trigger: broadcast when a group session is ended by the owner
create or replace function public.broadcast_group_session_end()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if old.ended_at is null and new.ended_at is not null then
        perform realtime.send(
            pg_catalog.jsonb_build_object('session_id', new.id::text),
            'session_ended',
            'group-session-' || new.id::text,
            true
        );
    end if;
    return null;
end;
$$;

revoke all on function public.broadcast_group_session_end() from public, anon, authenticated;

drop trigger if exists trg_broadcast_group_session_end on public.group_sessions;
create trigger trg_broadcast_group_session_end
after update of ended_at
on public.group_sessions
for each row
execute function public.broadcast_group_session_end();

-- RPC: invite a group member directly by user ID (for friend invites, bypasses email lookup)
create or replace function public.invite_group_member_by_id(
    p_group_id uuid,
    p_user_id uuid
)
returns table (
    id uuid,
    first_name text,
    last_name text,
    program text
)
language plpgsql
security definer
set search_path = ''
as $$
declare
    invited_profile public.profiles;
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;
    if not exists (
        select 1 from public.group_sessions
        where id = p_group_id
          and created_by = (select auth.uid())
          and ended_at is null
    ) then
        raise exception 'Only the active group owner can invite members';
    end if;

    select * into invited_profile
    from public.profiles
    where profiles.id = p_user_id;
    if not found then
        raise exception 'User not found';
    end if;

    insert into public.group_session_members (session_id, user_id, role)
    values (p_group_id, invited_profile.id, 'member')
    on conflict (session_id, user_id) do nothing;

    return query
    select invited_profile.id,
           invited_profile.first_name,
           invited_profile.last_name,
           invited_profile.program;
end;
$$;

revoke all on function public.invite_group_member_by_id(uuid, uuid) from public, anon;
grant execute on function public.invite_group_member_by_id(uuid, uuid) to authenticated;

notify pgrst, 'reload schema';
