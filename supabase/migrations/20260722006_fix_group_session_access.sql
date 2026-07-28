
grant select, insert, update on public.group_sessions to authenticated;
grant select, insert, delete on public.group_session_members to authenticated;

drop policy if exists "members can view session" on public.group_sessions;
drop policy if exists "members can view membership" on public.group_session_members;

create policy "owners can view session"
    on public.group_sessions for select
    to authenticated
    using ((select auth.uid()) = created_by);


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
