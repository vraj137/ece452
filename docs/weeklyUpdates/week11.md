# app-iteizers Week 11 Update

July 13 - 20

- [Edmond] Added accessible empty-states to the map: instead of rendering blank, the campus map now shows a "No study spots to show" card and the building-spaces screen shows a message when a building has no listed spaces
- [Pavan] Merged #22, built leaderboard and real time sessions with @ericzhu and @raghav. Very excited to wrap this product up!
- [Raghav] Implemented Supabase-backed friend discovery and requests, matching users by program and study term. Added the secure database migration/RLS policies, wired the Social UI to load real profiles, send requests, and accept friends, then verified the integration with builds and unit tests.
- [Eric] Worked with Pavan and Raghav to implement and merge #21, including new Explore leaderboards, real group sessions backed by Supabase, and group mode UX improvements. Also helped clean up unused sensor code and supported the new configurable booking URL banner on spot detail pages.
- [Vraj] Worked on the map that now supports one clean parent pin per building, with specific study areas nested underneath, and we added Waterloo study-space seed data with verified campus-map coordinates and N/A for unknown capacity. We also improved the map controls with refresh, zoom in, and zoom out, and the next major step is adding preloaded study spot images through Supabase Storage.
