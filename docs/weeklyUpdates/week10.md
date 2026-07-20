# app-iteizers Week 10 Update

July 6 - 13

- [Akshat] Continued work on social features, debugging state issues where friends were registering for a profile but not persisting. Held a meeting witht the rest of the team to plan out what features we still have to complete, and how to split that work up. Began work on badges feature.
- [Pavan] Cleaned up the debug repositories to pull entirely from MockData instead of hard-coded values, and fixed a few edge cases in the onboarding flow where the draft state wasn't clearing correctly after profile creation.
- [Vraj] Continued to work on check-in features to test the real-time accuracy and review forms after each completed study session to ensure we populate each study spot with live and accurate data. Added a few pre-located study spots in Supabase.
- [Edmond] Wired the color-coded marker states into the Mapbox map so each spot pin now reflects live occupancy (green/yellow/red) pulled from Vraj's real-time status layer, and added a non-color status cue (marker shape + label) so the indicators stay readable without relying on color. Hooked the marker tap-to-focus into the new check-in flow so tapping a spot opens its detail sheet, and extended the marker/home-repository unit tests to cover the occupancy state mapping and the color-independent fallbacks.
- [Eric] Finalized the spot review form implementation. Added opt-in toggles for volume detection and light/sunlight detection. Both default to off and require explicit user consent before any sensor sampling runs, keeping the feature privacy-first. Extended the review-form tests to cover the consent states.
- [Vishvam] Continued integrating the frontend with the backend by wiring the new social and badge-related data flows into the existing architecture. Refactored shared ViewModels and repository logic to support the new features while keeping state management consistent across screens. Worked with the team to resolve integration issues between profile, friends, and check-in functionality, ensuring the app remained stable as new features were merged.
- [Raghav] Continuing the integration of the app with Supabase, focused on enhancing backend data and dealing with problems related to state updates between screens. Dealt with merge conflicts, and did regression tests to guarantee the stability of the recently integrated features.

