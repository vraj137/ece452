create schema if not exists private;
revoke all on schema private from public;
grant usage on schema private to authenticated;

create or replace function private.is_group_session_member(p_session_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select (select auth.uid()) is not null
       and exists (
           select 1
           from public.group_session_members member
           where member.session_id = p_session_id
             and member.user_id = (select auth.uid())
       );
$$;

create or replace function private.is_group_session_owner(p_session_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select (select auth.uid()) is not null
       and exists (
           select 1
           from public.group_sessions session
           where session.id = p_session_id
             and session.created_by = (select auth.uid())
       );
$$;

revoke all on function private.is_group_session_member(uuid) from public;
revoke all on function private.is_group_session_owner(uuid) from public;
grant execute on function private.is_group_session_member(uuid) to authenticated;
grant execute on function private.is_group_session_owner(uuid) to authenticated;

drop policy if exists "members can view session" on public.group_sessions;
drop policy if exists "owners can view session" on public.group_sessions;
drop policy if exists "authenticated users can create sessions" on public.group_sessions;
drop policy if exists "owner can update session" on public.group_sessions;

create policy "group participants can view active sessions"
    on public.group_sessions for select
    to authenticated
    using (
        created_by = (select auth.uid())
        or (select private.is_group_session_member(id))
    );

create policy "users can create their own sessions"
    on public.group_sessions for insert
    to authenticated
    with check (
        created_by = (select auth.uid())
        and ended_at is null
    );

create policy "owners can update their sessions"
    on public.group_sessions for update
    to authenticated
    using ((select private.is_group_session_owner(id)))
    with check (created_by = (select auth.uid()));

drop policy if exists "members can view membership" on public.group_session_members;
drop policy if exists "owners and members can view membership" on public.group_session_members;
drop policy if exists "owner can add members" on public.group_session_members;
drop policy if exists "members can leave or owner can remove" on public.group_session_members;

create policy "group participants can view members"
    on public.group_session_members for select
    to authenticated
    using (
        (select private.is_group_session_member(session_id))
        or (select private.is_group_session_owner(session_id))
    );

create policy "owners can add group members"
    on public.group_session_members for insert
    to authenticated
    with check (
        (select private.is_group_session_owner(session_id))
        and (
            role = 'member'
            or (
                role = 'owner'
                and user_id = (select auth.uid())
            )
        )
    );

create policy "members can leave and owners can remove members"
    on public.group_session_members for delete
    to authenticated
    using (
        user_id = (select auth.uid())
        or (select private.is_group_session_owner(session_id))
    );

grant select, insert, update on table public.group_sessions to authenticated;
grant select, insert, delete on table public.group_session_members to authenticated;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'group_sessions_title_length'
          and conrelid = 'public.group_sessions'::regclass
    ) then
        alter table public.group_sessions
            add constraint group_sessions_title_length
            check (char_length(trim(title)) between 2 and 50);
    end if;
end
$$;

create unique index if not exists idx_group_sessions_one_active_owned
    on public.group_sessions (created_by)
    where ended_at is null;

update public.group_sessions
set ended_at = now()
where ended_at is null
  and title = 'Study sesh'
  and subtitle = 'Let''s find a spot!';

drop function if exists public.invite_group_member_by_email(uuid, text);
