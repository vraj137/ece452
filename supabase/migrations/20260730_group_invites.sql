-- Group invites: non-friend email invites create a pending request the recipient must accept.
-- Friend invites (via the Add button or email for accepted friends) still add immediately.
create table if not exists public.group_invites (
    id uuid primary key default gen_random_uuid(),
    group_session_id uuid not null references public.group_sessions(id) on delete cascade,
    inviter_id uuid not null references auth.users(id) on delete cascade,
    invitee_id uuid not null references auth.users(id) on delete cascade,
    status text not null default 'pending' check (status in ('pending', 'accepted', 'declined')),
    created_at timestamptz not null default now(),
    unique(group_session_id, invitee_id)
);

alter table public.group_invites enable row level security;

-- Invitee can see their own pending invites
create policy "invitee can view their pending invites" on public.group_invites
for select to authenticated
using (invitee_id = auth.uid() and status = 'pending');

create or replace function public.send_group_invite_by_email(
    p_group_id uuid,
    p_email    text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_inviter_id uuid := auth.uid();
    v_invitee_id uuid;
    v_is_friend  boolean;
    v_fname      text;
    v_lname      text;
begin
    -- Caller must be the group owner
    if not exists (
        select 1 from public.group_sessions
        where id = p_group_id
          and created_by = v_inviter_id
          and ended_at is null
    ) then
        raise exception 'You must be the group owner to send invites.';
    end if;

    -- Look up invitee by email
    select id into v_invitee_id
    from auth.users
    where email = lower(trim(p_email));

    if v_invitee_id is null then
        raise exception 'No Spotra account found for that email address.';
    end if;

    if v_invitee_id = v_inviter_id then
        raise exception 'You cannot invite yourself.';
    end if;

    -- Already a member?
    if exists (
        select 1 from public.group_session_members
        where session_id = p_group_id and user_id = v_invitee_id
    ) then
        raise exception 'This person is already in the group.';
    end if;

    -- Get display name
    select first_name, last_name into v_fname, v_lname
    from public.profiles where id = v_invitee_id;

    -- Check accepted friendship
    select exists (
        select 1 from public.friendships
        where status = 'accepted'
          and (
            (requester_id = v_inviter_id and addressee_id = v_invitee_id)
            or (requester_id = v_invitee_id and addressee_id = v_inviter_id)
          )
    ) into v_is_friend;

    if v_is_friend then
        -- Friend → add directly
        insert into public.group_session_members (session_id, user_id, role)
        values (p_group_id, v_invitee_id, 'member')
        on conflict do nothing;

        return pg_catalog.jsonb_build_object(
            'type',       'added',
            'id',         v_invitee_id,
            'first_name', v_fname,
            'last_name',  v_lname
        );
    else
        -- Non-friend → create pending invite
        insert into public.group_invites (group_session_id, inviter_id, invitee_id)
        values (p_group_id, v_inviter_id, v_invitee_id)
        on conflict (group_session_id, invitee_id) do nothing;

        return pg_catalog.jsonb_build_object(
            'type',       'invited',
            'id',         v_invitee_id,
            'first_name', v_fname,
            'last_name',  v_lname
        );
    end if;
end;
$$;

revoke all on function public.send_group_invite_by_email(uuid, text) from public, anon, authenticated;
grant execute on function public.send_group_invite_by_email(uuid, text) to authenticated;

-- ─── RPC: fetch_pending_group_invites ──────────────────────────────────────────

create or replace function public.fetch_pending_group_invites()
returns table(
    id               uuid,
    group_session_id uuid,
    group_title      text,
    inviter_name     text
)
language sql
security definer
set search_path = ''
as $$
    select
        gi.id,
        gi.group_session_id,
        gs.title                                              as group_title,
        p.first_name || ' ' || p.last_name                   as inviter_name
    from public.group_invites gi
    join public.group_sessions gs on gs.id = gi.group_session_id
    join public.profiles       p  on p.id  = gi.inviter_id
    where gi.invitee_id = auth.uid()
      and gi.status     = 'pending'
      and gs.ended_at   is null
    order by gi.created_at desc;
$$;

revoke all on function public.fetch_pending_group_invites() from public, anon, authenticated;
grant execute on function public.fetch_pending_group_invites() to authenticated;

-- ─── RPC: respond_to_group_invite ──────────────────────────────────────────────

create or replace function public.respond_to_group_invite(
    p_invite_id uuid,
    p_accept    boolean
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_invite record;
begin
    select * into v_invite
    from public.group_invites
    where id         = p_invite_id
      and invitee_id = auth.uid()
      and status     = 'pending';

    if not found then
        raise exception 'Invite not found or already responded.';
    end if;

    if p_accept then
        -- Check the session is still open
        if not exists (
            select 1 from public.group_sessions
            where id = v_invite.group_session_id and ended_at is null
        ) then
            update public.group_invites set status = 'declined' where id = p_invite_id;
            raise exception 'This group session has already ended.';
        end if;

        insert into public.group_session_members (session_id, user_id, role)
        values (v_invite.group_session_id, auth.uid(), 'member')
        on conflict do nothing;

        update public.group_invites set status = 'accepted' where id = p_invite_id;
    else
        update public.group_invites set status = 'declined' where id = p_invite_id;
    end if;
end;
$$;

revoke all on function public.respond_to_group_invite(uuid, boolean) from public, anon, authenticated;
grant execute on function public.respond_to_group_invite(uuid, boolean) to authenticated;

-- ─── Realtime: notify invitee when a new invite arrives ────────────────────────

create or replace function public.broadcast_group_invite()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    perform realtime.send(
        pg_catalog.jsonb_build_object(
            'invite_id',        new.id::text,
            'group_session_id', new.group_session_id::text
        ),
        'group_invite_received',
        'group-invites-' || new.invitee_id::text,
        true
    );
    return null;
end;
$$;

revoke all on function public.broadcast_group_invite() from public, anon, authenticated;

drop trigger if exists trg_broadcast_group_invite on public.group_invites;
create trigger trg_broadcast_group_invite
after insert on public.group_invites
for each row execute function public.broadcast_group_invite();

-- Allow invitee to subscribe to their own invites channel
drop policy if exists "users can receive their own group invite notifications" on realtime.messages;
create policy "users can receive their own group invite notifications"
on realtime.messages
for select
to authenticated
using (topic = 'group-invites-' || (select auth.uid())::text);
