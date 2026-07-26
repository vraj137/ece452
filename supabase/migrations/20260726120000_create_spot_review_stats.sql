-- Aggregates crowdsourced review data per spot so the Explore leaderboards rank on what
-- students actually reported, instead of the static noise_level/lighting values seeded in
-- 20260722001_add_noise_lighting_to_spots.sql.
--
-- IMPORTANT: the aggregation rules here (average rounded to one decimal; modal label =
-- highest count, ties broken alphabetically ascending) are mirrored in Kotlin by
-- app/src/main/kotlin/com/appetizers/spotra/domain/usecase/SpotReviewAggregates.kt,
-- which computes the same values for the spot detail screen. Keep the two in step.
--
-- Exposes no user_id and no comment text, so anonymous reviews stay anonymous. The
-- underlying reviews table is already readable by any signed-in user.

create or replace view public.spot_review_stats as
with labelled as (
    select spot_slug, 'noise' as facet, noise_level as label
    from public.reviews
    where noise_level is not null
    union all
    select spot_slug, 'lighting', lighting
    from public.reviews
    where lighting is not null
    union all
    select spot_slug, 'wifi', wifi_quality
    from public.reviews
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
    from public.reviews
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
left join modal on modal.spot_slug = rated.spot_slug;

grant select on public.spot_review_stats to authenticated;
revoke all on public.spot_review_stats from anon;
