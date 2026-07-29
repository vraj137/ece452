update public.user_spot_submissions
set status = 'pending'
where status not in ('pending', 'approved', 'rejected');

alter table public.user_spot_submissions
    alter column status set default 'pending';

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname in (
            'user_spot_submissions_valid_status',
            'user_spot_submissions_status_check'
        )
          and conrelid = 'public.user_spot_submissions'::regclass
    ) then
        alter table public.user_spot_submissions
            add constraint user_spot_submissions_valid_status
            check (status in ('pending', 'approved', 'rejected'));
    end if;
end
$$;

drop policy if exists "users can submit supported UW spots"
    on public.user_spot_submissions;

create policy "users can submit supported UW spots"
on public.user_spot_submissions for insert
to authenticated
with check (
    submitted_by_user_id = (select auth.uid())
    and lower(submitted_by_email) = lower(coalesce((select auth.jwt() ->> 'email'), ''))
    and status = 'pending'
    and (
        (latitude between 43.460 and 43.485 and longitude between -80.565 and -80.515)
        or (latitude between 43.445 and 43.458 and longitude between -80.510 and -80.488)
        or (latitude between 43.350 and 43.367 and longitude between -80.330 and -80.300)
    )
);

revoke all on table public.user_spot_submissions from anon;
revoke all on table public.user_spot_submissions from authenticated;

grant select on table public.user_spot_submissions to authenticated;
grant insert (
    name,
    description,
    latitude,
    longitude,
    building,
    floor,
    booking_url,
    submitted_by_email,
    submitted_by_user_id,
    status
) on table public.user_spot_submissions to authenticated;
