alter table public.group_sessions
    add column if not exists visibility text not null default 'private';

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'group_sessions_visibility_values'
          and conrelid = 'public.group_sessions'::regclass
    ) then
        alter table public.group_sessions
            add constraint group_sessions_visibility_values
            check (visibility in ('private', 'public'));
    end if;
end
$$;

create or replace function private.is_public_group_session(p_session_id uuid)
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
             and session.visibility = 'public'
             and session.ended_at is null
       );
$$;

revoke all on function private.is_public_group_session(uuid) from public;
grant execute on function private.is_public_group_session(uuid) to authenticated;

drop policy if exists "group participants can view active sessions"
    on public.group_sessions;

create policy "participants can view groups and users can discover public groups"
    on public.group_sessions for select
    to authenticated
    using (
        ended_at is null
        and (
            visibility = 'public'
            or created_by = (select auth.uid())
            or (select private.is_group_session_member(id))
        )
    );

drop policy if exists "owners can add group members"
    on public.group_session_members;

create policy "owners can add members and users can join public groups"
    on public.group_session_members for insert
    to authenticated
    with check (
        (
            (select private.is_group_session_owner(session_id))
            and (
                role = 'member'
                or (
                    role = 'owner'
                    and user_id = (select auth.uid())
                )
            )
        )
        or (
            role = 'member'
            and user_id = (select auth.uid())
            and (select private.is_public_group_session(session_id))
        )
    );

create index if not exists idx_group_sessions_open_public
    on public.group_sessions (created_at desc)
    where visibility = 'public' and ended_at is null;
