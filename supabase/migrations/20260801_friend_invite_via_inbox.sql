create or replace function public.invite_group_member_by_id(
    p_group_id uuid,
    p_user_id  uuid
)
returns table (
    id         uuid,
    first_name text,
    last_name  text,
    program    text
)
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_inviter_id uuid := auth.uid();
    v_profile    public.profiles%rowtype;
begin
    if v_inviter_id is null then
        raise exception 'Authentication required';
    end if;

    if not exists (
        select 1 from public.group_sessions
        where id         = p_group_id
          and created_by = v_inviter_id
          and ended_at   is null
    ) then
        raise exception 'Only the active group owner can invite members';
    end if;

    if p_user_id = v_inviter_id then
        raise exception 'You cannot invite yourself.';
    end if;

    select * into v_profile
    from public.profiles
    where profiles.id = p_user_id;

    if not found then
        raise exception 'User not found';
    end if;

    if exists (
        select 1 from public.group_session_members
        where session_id = p_group_id and user_id = p_user_id
    ) then
        raise exception 'This person is already in the group.';
    end if;

    -- Create pending invite.  On conflict (duplicate) do nothing.
    insert into public.group_invites (group_session_id, inviter_id, invitee_id)
    values (p_group_id, v_inviter_id, p_user_id)
    on conflict (group_session_id, invitee_id) do nothing;

    return query
    select v_profile.id,
           v_profile.first_name,
           v_profile.last_name,
           v_profile.program;
end;
$$;

revoke all on function public.invite_group_member_by_id(uuid, uuid) from public, anon;
grant execute on function public.invite_group_member_by_id(uuid, uuid) to authenticated;

notify pgrst, 'reload schema';
