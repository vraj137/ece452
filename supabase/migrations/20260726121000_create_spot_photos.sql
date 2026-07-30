-- Curated photos so students can see what a study spot actually looks like before walking there.

create table if not exists public.spot_photos (
    id         uuid primary key default gen_random_uuid(),
    spot_slug  text not null references public.spots(slug) on delete cascade,
    url        text not null,
    caption    text,
    sort_order int not null default 0,
    created_at timestamptz not null default now(),
    constraint spot_photos_url_is_https check (url like 'https://%')
);

create index if not exists idx_spot_photos_spot_slug
    on public.spot_photos (spot_slug, sort_order);

alter table public.spot_photos enable row level security;

create policy "Spot photos are readable by signed-in users"
on public.spot_photos for select
to authenticated
using (true);

grant select on public.spot_photos to authenticated;
revoke all on public.spot_photos from anon;

-- Public bucket: objects are served over plain HTTPS, which is what lets the Android client load
-- them with an image library alone and no Supabase storage SDK module.
insert into storage.buckets (id, name, public)
values ('spot-photos', 'spot-photos', true)
on conflict (id) do nothing;

create policy "Spot photos are publicly readable"
on storage.objects for select
to public
using (bucket_id = 'spot-photos');