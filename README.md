# app-etizers

spotra by app-etizers is a mobile platform designed to help students discover and evaluate study spaces on and around campus. The app provides an interactive map that displays study locations along with community-generated insights such as noise level, lighting quality, Wi-Fi reliability, occupancy levels, and average session duration. Students can filter locations based on their preferences, including solo study or group study environments, and quickly access relevant resources such as the University of Waterloo room booking pages.

To encourage engagement, users can rate and review study spaces after their sessions, contributing to a continuously updated database of study spot information. The platform also incorporates social features that allow students to see when friends or connections are studying nearby, connect with other students interested in group study sessions, and discover popular study locations within the community.

To further enhance the user experience, the app may include gamification elements such as badges, leaderboards for top-rated study spaces, and an interactive map experience that rewards exploration of new study locations. The overall goal is to help students find productive study environments while fostering a stronger sense of community and collaboration on campus.

## Team Members

- Akshat Jawne ([@AkshatJawne](https://github.com/AkshatJawne))
- Eric Zhu ([@ericzhu3](https://github.com/ericzhu3))
- Vraj Bhavsar ([@vraj137](https://github.com/vraj137))
- Vishvam Patel ([@VishvamPatel31](https://github.com/VishvamPatel31))
- Raghav Verma ([@RaghavVerma24](https://github.com/RaghavVerma24))
- Edmond Yang ([@Edmond0511](https://github.com/Edmond0511))
- Pavan Jayasinha ([@Sinestro38](https://github.com/Sinestro38))

## Important Links

- [Team Contract](./docs/team-contract.md)
- [Meeting Notes](./docs/meetings/)
- [Weekly Updates](./docs/weeklyUpdates/)

## Repository Structure

```text
app/ -> native Android app module using Kotlin and Jetpack Compose
docs/
├── team-contract.md
└── meetings/ -> meeting agendas and notes
└── weeklyUpdates/ -> weekly updates on group progress (P2)

build.gradle -> root Android Gradle configuration
settings.gradle -> Gradle project/module configuration
```

See [Android Project Structure](./docs/android-project-structure.md) for the current mobile codebase foundation.

## Onboarding and Supabase setup

The app includes a passwordless onboarding flow backed by Supabase Auth and Postgres:

1. Create a Supabase project.
2. Run `supabase/migrations/202606120001_create_profiles_and_restrict_signups.sql`.
3. In **Authentication > Hooks**, enable the `Before User Created` hook and select
   `public.hook_restrict_signup_to_uwaterloo`.
4. In the email confirmation template, include `{{ .Token }}` so users receive the
   six-digit code rather than only a magic link.
5. Add these untracked values to `local.properties`:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_PUBLISHABLE_KEY=your-publishable-or-anon-key
```

Never put a Supabase service-role key in the Android app. Without local credentials,
the app still builds and displays onboarding, but network actions show a configuration
error.
