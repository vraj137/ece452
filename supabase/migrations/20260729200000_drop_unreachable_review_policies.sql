-- Removes RLS policies on public.reviews that no PostgREST call can reach

drop policy if exists "verified visitors can submit reviews" on public.reviews;
drop policy if exists "users can update their own reviews" on public.reviews;
drop policy if exists "users can delete their own reviews" on public.reviews;
