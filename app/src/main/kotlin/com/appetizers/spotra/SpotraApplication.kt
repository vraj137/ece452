package com.appetizers.spotra

import android.app.Application
import com.appetizers.spotra.data.local.DataStoreOnboardingDraftRepository
import com.appetizers.spotra.data.location.DebugLocationRepository
import com.appetizers.spotra.data.location.FusedLocationRepository
import com.appetizers.spotra.data.location.LocationRepository
import com.appetizers.spotra.data.remote.DebugAuthRepository
import com.appetizers.spotra.data.remote.DebugBadgeRepository
import com.appetizers.spotra.data.remote.DebugFriendRepository
import com.appetizers.spotra.data.remote.DebugHomeRepository
import com.appetizers.spotra.data.remote.DebugProfileRepository
import com.appetizers.spotra.data.remote.DebugReviewRepository
import com.appetizers.spotra.data.remote.DebugSpotSubmissionRepository
import com.appetizers.spotra.data.remote.DebugStreakRepository
import com.appetizers.spotra.data.remote.MissingConfigurationAuthRepository
import com.appetizers.spotra.data.remote.MissingConfigurationProfileRepository
import com.appetizers.spotra.data.remote.SupabaseAuthRepository
import com.appetizers.spotra.data.remote.SupabaseBadgeRepository
import com.appetizers.spotra.data.remote.SupabaseFriendRepository
import com.appetizers.spotra.data.remote.SupabaseHomeRepository
import com.appetizers.spotra.data.remote.SupabaseProfileRepository
import com.appetizers.spotra.data.remote.SupabaseReviewRepository
import com.appetizers.spotra.data.remote.SupabaseSpotSubmissionRepository
import com.appetizers.spotra.data.remote.SupabaseStreakRepository
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.BadgeRepository
import com.appetizers.spotra.domain.repository.FriendRepository
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.OnboardingDraftRepository
import com.appetizers.spotra.domain.repository.ProfileRepository
import com.appetizers.spotra.domain.repository.ReviewRepository
import com.appetizers.spotra.domain.repository.SpotSubmissionRepository
import com.appetizers.spotra.domain.repository.StreakRepository
import com.appetizers.spotra.domain.usecase.AwardBadgesUseCase
import com.appetizers.spotra.domain.usecase.GetStartRouteUseCase
import com.mapbox.common.MapboxOptions
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.json.Json

class SpotraApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.MAPBOX_PUBLIC_TOKEN.isNotBlank()) {
            MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN
        }
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val draftRepository: OnboardingDraftRepository =
        DataStoreOnboardingDraftRepository(application, json)

    val locationRepository: LocationRepository

    val authRepository: AuthRepository
    val profileRepository: ProfileRepository
    val homeRepository: HomeRepository
    val spotSubmissionRepository: SpotSubmissionRepository
    val reviewRepository: ReviewRepository
    val friendRepository: FriendRepository
    val streakRepository: StreakRepository
    val badgeRepository: BadgeRepository
    val awardBadgesUseCase: AwardBadgesUseCase
    val getStartRoute: GetStartRouteUseCase

    init {
        val hasCredentials = BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

        locationRepository = if (hasCredentials || BuildConfig.DEBUG) {
            FusedLocationRepository(application)
        } else {
            DebugLocationRepository()
        }

        if (hasCredentials) {
            val client = createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
            ) {
                install(Auth)
                install(Postgrest)
            }
            authRepository = SupabaseAuthRepository(client)
            profileRepository = SupabaseProfileRepository(client)
            spotSubmissionRepository = SupabaseSpotSubmissionRepository(client)
            homeRepository = SupabaseHomeRepository(client)
            reviewRepository = SupabaseReviewRepository(client)
            friendRepository = SupabaseFriendRepository(client)
            streakRepository = SupabaseStreakRepository(client)
            badgeRepository = SupabaseBadgeRepository(client)
        } else if (BuildConfig.DEBUG) {
            authRepository = DebugAuthRepository()
            profileRepository = DebugProfileRepository()
            spotSubmissionRepository = DebugSpotSubmissionRepository()
            homeRepository = DebugHomeRepository()
            reviewRepository = DebugReviewRepository()
            friendRepository = DebugFriendRepository()
            streakRepository = DebugStreakRepository()
            badgeRepository = DebugBadgeRepository()
        } else {
            authRepository = MissingConfigurationAuthRepository()
            profileRepository = MissingConfigurationProfileRepository()
            spotSubmissionRepository = DebugSpotSubmissionRepository()
            homeRepository = DebugHomeRepository()
            reviewRepository = DebugReviewRepository()
            friendRepository = DebugFriendRepository()
            streakRepository = DebugStreakRepository()
            badgeRepository = DebugBadgeRepository()
        }

        awardBadgesUseCase = AwardBadgesUseCase(badgeRepository, reviewRepository)
        getStartRoute = GetStartRouteUseCase(authRepository, profileRepository)
    }
}
