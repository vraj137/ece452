-- Group-session tables were not explicitly exposed to authenticated Data API
-- clients, and the original membership SELECT policy queried its own table,
-- causing PostgreSQL's "infinite recursion detected in policy" error.

grant select, insert, update on public.group_sessions to authenticated;
grant select, insert, delete on public.group_session_members to authenticated;

drop policy if exists "members can view session" on public.group_sessions;
drop policy if exists "members can view membership" on public.group_session_members;

-- The current app creates and owns its group session. Keeping session reads
-- owner-scoped avoids a circular RLS dependency between these two tables.
create policy "owners can view session"
    on public.group_sessions for select
    to authenticated
    using ((select auth.uid()) = created_by);

-- Owners can render the full member list; a member can always read their own
-- membership row. This policy only consults group_sessions, so it cannot recurse.
create policy "owners and members can view membership"
    on public.group_session_members for select
    to authenticated
    using (
        user_id = (select auth.uid())
        or exists (
            select 1
            from public.group_sessions s
            where s.id = group_session_members.session_id
              and s.created_by = (select auth.uid())
        )
    );
