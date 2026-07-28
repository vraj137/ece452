package com.appetizers.spotra.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appetizers.spotra.domain.model.OnboardingDraft
import com.appetizers.spotra.domain.model.StudyTerm
import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.OnboardingDraftRepository
import com.appetizers.spotra.domain.repository.ProfileRepository
import com.appetizers.spotra.presentation.navigation.Routes
import com.appetizers.spotra.presentation.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val draft: OnboardingDraft = OnboardingDraft(),
    val otp: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRegistration: Boolean = true
)

sealed interface OnboardingEvent {
    data class Navigate(val route: String) : OnboardingEvent
}

object OnboardingValidation {
    fun isValidName(value: String): Boolean = value.trim().length >= 2
    fun isValidUwEmail(value: String): Boolean =
        Regex("^[A-Z0-9._%+-]+@uwaterloo\\.ca$", RegexOption.IGNORE_CASE)
            .matches(value.trim())

    fun isValidOtp(value: String): Boolean = value.length == 6 && value.all(Char::isDigit)
    fun isValidProgram(value: String): Boolean = value.trim().length >= 2
}

class OnboardingViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val draftRepository: OnboardingDraftRepository
) : ViewModel() {
    private var draftSaveJob: Job? = null
    private var hasLocalDraftChanges = false
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>()
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            draftRepository.draft.collect { draft ->
                _uiState.update { state ->
                    if (hasLocalDraftChanges) state else state.copy(draft = draft)
                }
            }
        }
    }

    fun beginRegistration() {
        resetAuthFlow(isRegistration = true)
    }

    fun beginSignIn() {
        resetAuthFlow(isRegistration = false)
    }

    private fun resetAuthFlow(isRegistration: Boolean) {
        draftSaveJob?.cancel()
        hasLocalDraftChanges = true
        _uiState.value = OnboardingUiState(isRegistration = isRegistration)
        viewModelScope.launch {
            draftRepository.clear()
            hasLocalDraftChanges = false
        }
    }

    fun updateName(firstName: String? = null, lastName: String? = null) {
        saveDraft {
            copy(
                firstName = firstName ?: this.firstName,
                lastName = lastName ?: this.lastName
            )
        }
    }

    fun updateEmail(email: String) = saveDraft { copy(email = email) }
    fun updateProgram(program: String) = saveDraft { copy(program = program) }
    fun updateStudyTerm(term: StudyTerm) = saveDraft { copy(studyTerm = term) }

    fun updateOtp(otp: String) {
        _uiState.update { it.copy(otp = otp.filter(Char::isDigit).take(6), error = null) }
    }

    fun continueFromName() {
        val draft = uiState.value.draft
        when {
            !OnboardingValidation.isValidName(draft.firstName) ->
                showError("Enter a first name with at least 2 characters.")
            !OnboardingValidation.isValidName(draft.lastName) ->
                showError("Enter a last name with at least 2 characters.")
            else -> navigate(Routes.Email)
        }
    }

    fun sendOtp() = runRequest {
        val email = uiState.value.draft.email.trim().lowercase()
        require(OnboardingValidation.isValidUwEmail(email)) {
            "Use your @uwaterloo.ca email address."
        }
        saveDraftAndAwait { copy(email = email) }
        authRepository.sendOtp(email, createUser = uiState.value.isRegistration)
        _uiState.update { it.copy(otp = "") }
        _events.emit(OnboardingEvent.Navigate(Routes.Otp))
    }

    fun verifyOtp() = runRequest {
        val state = uiState.value
        require(OnboardingValidation.isValidOtp(state.otp)) {
            "Enter the 6-digit code from your email."
        }
        authRepository.verifyOtp(state.draft.email, state.otp)
        if (state.isRegistration) {
            _events.emit(OnboardingEvent.Navigate(Routes.Program))
        } else {
            _events.emit(OnboardingEvent.Navigate(Routes.Home))
        }
    }

    fun resendOtp() = runRequest {
        val state = uiState.value
        authRepository.sendOtp(state.draft.email, createUser = state.isRegistration)
    }

    fun completeProfile() = runRequest {
        val state = uiState.value
        val draft = state.draft
        val user = requireNotNull(authRepository.currentUser()) {
            "Your session expired. Request a new verification code."
        }
        require(user.email.isNotBlank()) {
            "Your account email is missing. Please sign in again."
        }
        profileRepository.saveProfile(
            UserProfile(
                userId = user.id,
                firstName = draft.firstName.trim(),
                lastName = draft.lastName.trim(),
                email = user.email,
                program = draft.program.trim(),
                studyTerm = draft.studyTerm
            )
        )
        _events.emit(OnboardingEvent.Navigate(Routes.Complete))
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            draftSaveJob?.cancel()
            draftRepository.clear()
            _events.emit(OnboardingEvent.Navigate(Routes.Home))
        }
    }

    private fun saveDraft(transform: OnboardingDraft.() -> OnboardingDraft) {
        val updated = uiState.value.draft.transform()
        hasLocalDraftChanges = true
        _uiState.update { it.copy(draft = updated, error = null) }
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(250)
            draftRepository.save(updated)
        }
    }

    private suspend fun saveDraftAndAwait(transform: OnboardingDraft.() -> OnboardingDraft) {
        val updated = uiState.value.draft.transform()
        draftSaveJob?.cancel()
        hasLocalDraftChanges = true
        _uiState.update { it.copy(draft = updated, error = null) }
        draftRepository.save(updated)
    }

    private fun runRequest(block: suspend () -> Unit) {
        if (uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { block() }
                .onFailure { showError(it.toUserMessage("Something went wrong. Try again.")) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun navigate(route: String) {
        viewModelScope.launch { _events.emit(OnboardingEvent.Navigate(route)) }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
        private val draftRepository: OnboardingDraftRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OnboardingViewModel(authRepository, profileRepository, draftRepository) as T
    }
}
