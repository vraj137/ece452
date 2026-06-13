create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    first_name text not null check (char_length(trim(first_name)) >= 2),
    last_name text not null check (char_length(trim(last_name)) >= 2),
    email text not null check (lower(email) ~ '^[^@]+@uwaterloo\.ca$'),
    program text not null check (char_length(trim(program)) >= 2),
    study_term text not null check (
        study_term in ('1A', '1B', '2A', '2B', '3A', '3B', '4A', '4B', 'Graduate', 'Other')
    ),
    onboarding_complete boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

create policy "Users can read their own profile"
on public.profiles for select
to authenticated
using ((select auth.uid()) = id);

create policy "Users can create their own profile"
on public.profiles for insert
to authenticated
with check (
    (select auth.uid()) = id
    and lower(email) = lower(coalesce((select auth.jwt() ->> 'email'), ''))
);

create policy "Users can update their own profile"
on public.profiles for update
to authenticated
using ((select auth.uid()) = id)
with check (
    (select auth.uid()) = id
    and lower(email) = lower(coalesce((select auth.jwt() ->> 'email'), ''))
);

grant select, insert, update on public.profiles to authenticated;
revoke all on public.profiles from anon;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create trigger profiles_set_updated_at
before update on public.profiles
for each row execute function public.set_updated_at();

create or replace function public.hook_restrict_signup_to_uwaterloo(event jsonb)
returns jsonb
language plpgsql
stable
set search_path = ''
as $$
declare
    signup_email text;
begin
    signup_email := lower(coalesce(event->'user'->>'email', ''));

    if signup_email !~ '^[^@]+@uwaterloo\.ca$' then
        return jsonb_build_object(
            'error', jsonb_build_object(
                'http_code', 403,
                'message', 'A valid @uwaterloo.ca email address is required.'
            )
        );
    end if;

    return '{}'::jsonb;
end;
$$;

grant usage on schema public to supabase_auth_admin;
grant execute on function public.hook_restrict_signup_to_uwaterloo(jsonb) to supabase_auth_admin;
revoke execute on function public.hook_restrict_signup_to_uwaterloo(jsonb) from anon, authenticated, public;
