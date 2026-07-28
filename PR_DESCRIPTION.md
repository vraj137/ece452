# Harden spot submissions and improve home-flow resilience

## Summary

- Add a Supabase migration that normalizes existing spot-submission statuses, defaults new submissions to `pending`, and enforces the allowed status values.
- Restrict spot-submission inserts to authenticated users submitting their own Waterloo-area locations with a `pending` status.
- Limit authenticated table permissions to selecting submissions and inserting only the required submission fields.
- Report average review occupancy instead of using the first available value.
- Preserve Home screen navigation and submission state through configuration changes.
- Prevent duplicate friend-request submissions, retain the current user ID for search filtering, and surface missing-request errors.
- Limit review comments to 500 characters and improve leaderboard alignment.

## Validation

- `git diff --check`
- Attempted `./gradlew.bat testDebugUnitTest --no-daemon` (did not complete within the 60-second execution limit)
