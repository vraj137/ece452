create or replace view public.spot_trending as
select spot_slug, count(*)::int as checkins_7d
from public.check_ins
where started_at > now() - interval '7 days'
group by spot_slug;

grant select on public.spot_trending to authenticated;
revoke all on public.spot_trending from anon;
