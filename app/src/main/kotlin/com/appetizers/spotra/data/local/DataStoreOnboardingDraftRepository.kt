package com.appetizers.spotra.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.appetizers.spotra.domain.model.OnboardingDraft
import com.appetizers.spotra.domain.repository.OnboardingDraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.onboardingDataStore by preferencesDataStore("onboarding")

class DataStoreOnboardingDraftRepository(
    private val context: Context,
    private val json: Json
) : OnboardingDraftRepository {
    private val draftKey = stringPreferencesKey("draft")

    override val draft: Flow<OnboardingDraft> = context.onboardingDataStore.data.map { preferences ->
        preferences[draftKey]?.let {
            runCatching { json.decodeFromString<OnboardingDraft>(it) }.getOrNull()
        } ?: OnboardingDraft()
    }

    override suspend fun save(draft: OnboardingDraft) {
        context.onboardingDataStore.edit { preferences ->
            preferences[draftKey] = json.encodeToString(draft)
        }
    }

    override suspend fun clear() {
        context.onboardingDataStore.edit { it.remove(draftKey) }
    }
}
