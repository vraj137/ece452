-- Keep raw review rows private while exposing only aggregate statistics to signed-in users.
-- The function lives outside the Data API's public schema and is callable only by the
-- authenticated role; the public view remains a security-invoker view.
create or replace function private.spot_review_stats_rollup()
returns table (
    spot_slug text,
    average_rating float8,
    review_count int,
    noise_level text,
    lighting text,
    wifi_quality text
)
language sql
stable
security definer
set search_path = ''
as $$
with review_targets as (
    select
        target.spot_slug,
        review.rating,
        review.noise_level,
        review.lighting,
        review.wifi_quality
    from public.reviews as review
    join public.spots as reviewed_spot
      on reviewed_spot.slug = review.spot_slug
    cross join lateral (
        select reviewed_spot.slug as spot_slug
        union
        select reviewed_spot.parent_slug
        where reviewed_spot.parent_slug is not null
    ) as target
),
labelled as (
    select spot_slug, 'noise' as facet, noise_level as label
    from review_targets
    where noise_level is not null
    union all
    select spot_slug, 'lighting', lighting
    from review_targets
    where lighting is not null
    union all
    select spot_slug, 'wifi', wifi_quality
    from review_targets
    where wifi_quality is not null
),
ranked as (
    select
        spot_slug,
        facet,
        label,
        row_number() over (
            partition by spot_slug, facet
            order by count(*) desc, label asc
        ) as rn
    from labelled
    group by spot_slug, facet, label
),
modal as (
    select
        spot_slug,
        max(label) filter (where facet = 'noise')    as noise_level,
        max(label) filter (where facet = 'lighting') as lighting,
        max(label) filter (where facet = 'wifi')     as wifi_quality
    from ranked
    where rn = 1
    group by spot_slug
),
rated as (
    select
        spot_slug,
        round(avg(rating)::numeric, 1)::float8 as average_rating,
        count(*)::int                          as review_count
    from review_targets
    group by spot_slug
)
select
    rated.spot_slug,
    rated.average_rating,
    rated.review_count,
    modal.noise_level,
    modal.lighting,
    modal.wifi_quality
from rated
left join modal on modal.spot_slug = rated.spot_slug
where (select auth.uid()) is not null;
$$;

revoke all on function private.spot_review_stats_rollup() from public, anon;
grant usage on schema private to authenticated;
grant execute on function private.spot_review_stats_rollup() to authenticated;

-- The map filters operate on parent spots, whereas reviews are normally submitted for a
-- specific room or study area. The roll-up function returns both the exact child statistics
-- and a second aggregate under the parent building's slug.
create or replace view public.spot_review_stats
with (security_invoker = true)
as
select *
from private.spot_review_stats_rollup();

revoke all on table public.spot_review_stats from public, anon, authenticated;
grant select on table public.spot_review_stats to authenticated;
