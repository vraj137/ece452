-- Removes the abandoned second friendship model.
drop table if exists public.friend_requests cascade;

-- is_classmate() existed only to back a profiles select policy that
drop function if exists public.is_classmate(text, text);
