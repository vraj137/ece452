package com.appetizers.spotra

import android.app.Application
import com.appetizers.spotra.data.local.DataStoreOnboardingDraftRepository
import com.appetizers.spotra.data.remote.DebugAuthRepository
import com.appetizers.spotra.data.remote.DebugHomeRepository
import com.appetizers.spotra.data.remote.DebugProfileRepository
import com.appetizers.spotra.data.remote.DebugReviewRepository
import com.appetizers.spotra.data.remote.DebugSpotSubmissionRepository
import com.appetizers.spotra.data.remote.MissingConfigurationAuthRepository
import com.appetizers.spotra.data.remote.MissingConfigurationProfileRepository
import com.appetizers.spotra.data.remote.SupabaseAuthRepository
import com.appetizers.spotra.data.remote.SupabaseHomeRepository
import com.appetizers.spotra.data.remote.SupabaseProfileRepository
import com.appetizers.spotra.data.remote.SupabaseReviewRepository
import com.appetizers.spotra.data.remote.SupabaseSpotSubmissionRepository
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.OnboardingDraftRepository
import com.appetizers.spotra.domain.repository.ProfileRepository
import com.appetizers.spotra.domain.repository.ReviewRepository
import com.appetizers.spotra.domain.repository.SpotSubmissionRepository
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

    val authRepository: AuthRepository
    val profileRepository: ProfileRepository
    val homeRepository: HomeRepository
    val spotSubmissionRepository: SpotSubmissionRepository
    val reviewRepository: ReviewRepository
    val getStartRoute: GetStartRouteUseCase

    init {
        val hasCredentials = BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

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
        } else if (BuildConfig.DEBUG) {
            authRepository = DebugAuthRepository()
            profileRepository = DebugProfileRepository()
            spotSubmissionRepository = DebugSpotSubmissionRepository()
            homeRepository = DebugHomeRepository()
            reviewRepository = DebugReviewRepository()
        } else {
            authRepository = MissingConfigurationAuthRepository()
            profileRepository = MissingConfigurationProfileRepository()
            spotSubmissionRepository = DebugSpotSubmissionRepository()
            homeRepository = DebugHomeRepository()
            reviewRepository = DebugReviewRepository()
        }

        getStartRoute = GetStartRouteUseCase(authRepository, profileRepository)
    }
}
