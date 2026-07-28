-- Curated photos so students can see what a study spot actually looks like before walking there.
--
-- Read-only to clients: no insert/update/delete grant, matching how public.spots itself is
-- curated. Rows are added by migration or through the Supabase dashboard, not by the app, so
-- there is no user-upload path and no moderation surface to build.
--
-- sort_order 0 is the cover image — the one shown as a thumbnail on Explore and Map cards.

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

-- No rows are seeded: the real photos have to be taken and uploaded first. To add them, upload
-- into the spot-photos bucket via the Supabase dashboard, then add a migration in this shape
-- (the public URL is https://<project>.supabase.co/storage/v1/object/public/spot-photos/<file>):
--
-- insert into public.spot_photos (spot_slug, url, caption, sort_order) values
--     ('e7-study-hall', 'https://.../e7-hall-wide.jpg',  'Long shared tables under the skylights', 0),
--     ('e7-study-hall', 'https://.../e7-hall-booth.jpg', 'Booth seating along the back wall',      1);
--
-- caption is read out as the image's content description, so write it as a description of the
-- space. sort_order 0 is the cover shown on Explore and Map cards.
