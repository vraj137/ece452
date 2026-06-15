package com.appetizers.spotra

import android.app.Application
import com.appetizers.spotra.data.local.DataStoreOnboardingDraftRepository
import com.appetizers.spotra.data.remote.DebugAuthRepository
import com.appetizers.spotra.data.remote.DebugProfileRepository
import com.appetizers.spotra.data.remote.MissingConfigurationAuthRepository
import com.appetizers.spotra.data.remote.MissingConfigurationProfileRepository
import com.appetizers.spotra.data.remote.SupabaseAuthRepository
import com.appetizers.spotra.data.remote.SupabaseProfileRepository
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.OnboardingDraftRepository
import com.appetizers.spotra.domain.repository.ProfileRepository
import com.appetizers.spotra.domain.usecase.GetStartRouteUseCase
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.json.Json

class SpotraApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
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
        } else if (BuildConfig.DEBUG) {
            authRepository = DebugAuthRepository()
            profileRepository = DebugProfileRepository()
        } else {
            authRepository = MissingConfigurationAuthRepository()
            profileRepository = MissingConfigurationProfileRepository()
        }

        getStartRoute = GetStartRouteUseCase(authRepository, profileRepository)
    }
}
