drop policy if exists "participants can view groups and users can discover public groups"
  on public.group_sessions;

create policy "participants and public can view groups"
  on public.group_sessions
  for select
  to authenticated
  using (
    created_by = (select auth.uid())
    or (select private.is_group_session_member(id))
    or (visibility = 'public' and ended_at is null)
  );
