# Android Project Structure

This repository uses Kotlin, Jetpack Compose, Navigation Compose, DataStore, and
Supabase behind repository interfaces.

```text
app/
└── src/main/
    ├── AndroidManifest.xml
    ├── kotlin/com/appetizers/spotra/
    │   ├── MainActivity.kt
    │   ├── SpotraApplication.kt
    │   ├── data/ -> DataStore and Supabase implementations
    │   ├── domain/ -> models and repository contracts
    │   └── presentation/ -> app routing, onboarding, home, components, and theme
    └── res/
        ├── drawable/
        ├── mipmap-anydpi-v26/
        └── values/
```

`supabase/migrations/` contains the backend schema, row-level security policies, and
the authentication hook that restricts account creation to `@uwaterloo.ca`.
