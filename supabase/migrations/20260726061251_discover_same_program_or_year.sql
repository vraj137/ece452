-- Suggest safe directory profiles that share the caller's program or year.
create index if not exists idx_profiles_program
    on public.profiles (program)
    where program <> '';

create index if not exists idx_profiles_study_term
    on public.profiles (study_term)
    where study_term is not null;

create or replace function public.discover_profiles(p_limit int default 20)
returns table (
    id uuid,
    first_name text,
    last_name text,
    program text,
    study_term text
)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    caller_id uuid := (select auth.uid());
    caller_program text;
    caller_term text;
    caller_year text;
begin
    if caller_id is null then
        raise exception 'Authentication required';
    end if;

    select profile.program, profile.study_term::text
    into caller_program, caller_term
    from public.profiles profile
    where profile.id = caller_id;

    if not found then
        raise exception 'Complete your profile before using Discover';
    end if;

    caller_year := case
        when caller_term ~ '^[1-4][AB]$' then left(caller_term, 1)
        else nullif(caller_term, '')
    end;

    return query
    select profile.id,
           profile.first_name,
           profile.last_name,
           profile.program,
           coalesce(profile.study_term::text, '')
    from public.profiles profile
    where profile.id <> caller_id
      and (
          (nullif(caller_program, '') is not null and profile.program = caller_program)
          or (
              caller_year is not null
              and case
                  when profile.study_term::text ~ '^[1-4][AB]$'
                      then left(profile.study_term::text, 1)
                  else nullif(profile.study_term::text, '')
              end = caller_year
          )
      )
      and not exists (
          select 1
          from public.friendships friendship
          where (
              friendship.requester_id = caller_id
              and friendship.addressee_id = profile.id
          ) or (
              friendship.addressee_id = caller_id
              and friendship.requester_id = profile.id
          )
      )
    order by
        case
            when profile.program = caller_program
             and caller_year is not null
             and case
                    when profile.study_term::text ~ '^[1-4][AB]$'
                        then left(profile.study_term::text, 1)
                    else nullif(profile.study_term::text, '')
                 end = caller_year
                then 0
            when profile.program = caller_program then 1
            else 2
        end,
        profile.first_name,
        profile.last_name
    limit least(greatest(coalesce(p_limit, 20), 1), 50);
end;
$$;

revoke all on function public.discover_profiles(int) from public, anon;
grant execute on function public.discover_profiles(int) to authenticated;

notify pgrst, 'reload schema';
