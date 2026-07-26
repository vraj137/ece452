-- Removes the abandoned second friendship model.
--
-- 202607190001_add_friend_requests.sql introduced public.friend_requests (requester_id /
-- recipient_id) alongside the pre-existing public.friendships (requester_id / addressee_id).
-- Only friendships was ever wired up: SpotraApplication instantiates SupabaseFriendRepository,
-- never SupabaseSocialRepository. Two tables modelling the same relationship with different
-- column names is a standing trap, so the unused one goes.
--
-- The matching Kotlin (SocialRepository, SupabaseSocialRepository, SocialUser, SocialSnapshot)
-- is deleted in the same change.

drop table if exists public.friend_requests cascade;

-- is_classmate() existed only to back a profiles select policy that
-- 20260726054502_stabilize_core_privacy_and_reviews.sql dropped when it rebuilt every policy on
-- profiles. Cross-user profile reads now go through safe_profiles()/discover_profiles().
drop function if exists public.is_classmate(text, text);
