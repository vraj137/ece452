# app-iteizers Week 9 Update

June 29 - July 6

- [Pavan] Built out the social mode backend with Akshat. Wired the friend request send/accept/decline flow from the SocialViewModel through the friend repository, and integrated the new friendships schema so friend connections persist across sessions. Added the debug/mock handlers so the social flow is testable without Supabase credentials, and helped tighten up completed-session history on the Profile screen.
- [Raghav] Continued Supabase integration work, connecting app features to the backend and ensuring data flows correctly across sessions. Refined the map experience with additional polish and improvements to spot interactions. Also made updates to the Profile screen to improve consistency and usability across the app.
- [Vraj] Added the live-status layer to the map, which is a refresh control that re-pulls real-time spot occupancy from Supabase, plus accessibility improvements (screen-reader labels on map pins and grouped live-status indicators), while keeping the map framed on UW campus.
- [Vishvam] Working on search and filters for the Map and Explore tabs where students can now search spots by name/building and filter by space type (solo/group), amenities (wifi, outlets, whiteboard, etc.), and current occupancy, all computed client-side from the spots already loaded (no new backend calls).
