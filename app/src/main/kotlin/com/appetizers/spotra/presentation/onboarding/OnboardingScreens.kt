package com.appetizers.spotra.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appetizers.spotra.domain.model.StudyTerm
import com.appetizers.spotra.presentation.components.ErrorMessage
import com.appetizers.spotra.presentation.components.PrimaryButton
import com.appetizers.spotra.presentation.components.SecondaryButton
import com.appetizers.spotra.presentation.components.SpotraLogo
import com.appetizers.spotra.presentation.components.SpotraTextField
import com.appetizers.spotra.presentation.components.SpotraWordmark
import com.appetizers.spotra.presentation.components.StepHeader
import com.appetizers.spotra.presentation.theme.SpotraBorder
import com.appetizers.spotra.presentation.theme.SpotraPrimaryContainer
import com.appetizers.spotra.presentation.theme.SpotraSuccess

@Composable
fun WelcomeScreen(onCreateAccount: () -> Unit, onSignIn: () -> Unit) {
    ScreenContainer(verticalArrangement = Arrangement.Center) {
        SpotraLogo()
        Spacer(Modifier.height(24.dp))
        SpotraWordmark()
        Spacer(Modifier.height(12.dp))
        Text(
            "Find your perfect\nstudy spot at UW.",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Live occupancy, noise, and Wi-Fi scores, all in one place.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(36.dp))
        PrimaryButton("Create account", onCreateAccount, icon = Icons.Rounded.PersonAdd)
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Sign in", onSignIn)
        Spacer(Modifier.height(18.dp))
        Text(
            "Requires a @uwaterloo.ca email",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NameScreen(state: OnboardingUiState, onBack: () -> Unit, viewModel: OnboardingViewModel) {
    FormScreen(onBack) {
        StepHeader(1, "What's your name?", "This is how you'll appear to study buddies.")
        Spacer(Modifier.height(22.dp))
        SpotraTextField(
            state.draft.firstName,
            { viewModel.updateName(firstName = it) },
            "First name",
            placeholder = "First name",
            leadingIcon = Icons.Rounded.Person
        )
        Spacer(Modifier.height(14.dp))
        SpotraTextField(
            state.draft.lastName,
            { viewModel.updateName(lastName = it) },
            "Last name",
            placeholder = "Last name",
            leadingIcon = Icons.Rounded.Person
        )
        Spacer(Modifier.height(14.dp))
        ErrorMessage(state.error)
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            "Continue",
            viewModel::continueFromName,
            icon = Icons.AutoMirrored.Rounded.ArrowForward
        )
    }
}

@Composable
fun EmailScreen(
    state: OnboardingUiState,
    onBack: () -> Unit,
    viewModel: OnboardingViewModel,
    signIn: Boolean
) {
    FormScreen(onBack) {
        if (signIn) {
            Text("Welcome back", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in with your University of Waterloo email.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            StepHeader(2, "Your UW email", "Verify your student status and unlock all features.")
        }
        Spacer(Modifier.height(22.dp))
        SpotraTextField(
            state.draft.email,
            viewModel::updateEmail,
            "University of Waterloo email",
            placeholder = "you@uwaterloo.ca",
            leadingIcon = Icons.Rounded.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(12.dp))
        InfoCard("A 6-digit code will be sent to verify your address. This keeps Spotra exclusive to students.")
        Spacer(Modifier.height(14.dp))
        ErrorMessage(state.error)
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            "Send verification code",
            viewModel::sendOtp,
            loading = state.isLoading
        )
    }
}

@Composable
fun OtpScreen(state: OnboardingUiState, onBack: () -> Unit, viewModel: OnboardingViewModel) {
    FormScreen(onBack) {
        if (state.isRegistration) {
            StepHeader(2, "Check your email", "Enter the 6-digit code sent to ${state.draft.email}.")
        } else {
            Text("Check your email", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter the 6-digit code sent to ${state.draft.email}.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(22.dp))
        SpotraTextField(
            state.otp,
            viewModel::updateOtp,
            "Verification code",
            placeholder = "000000",
            leadingIcon = Icons.Rounded.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
        Spacer(Modifier.height(14.dp))
        ErrorMessage(state.error)
        Spacer(Modifier.height(18.dp))
        PrimaryButton("Verify code", viewModel::verifyOtp, loading = state.isLoading)
        Spacer(Modifier.height(8.dp))
        SecondaryButton("Resend code", viewModel::resendOtp)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ProgramScreen(state: OnboardingUiState, onBack: () -> Unit, viewModel: OnboardingViewModel) {
    FormScreen(onBack) {
        StepHeader(3, "Major & year", "Helps you find and connect with classmates.")
        Spacer(Modifier.height(22.dp))
        SpotraTextField(
            state.draft.program,
            viewModel::updateProgram,
            "Major / Program",
            placeholder = "Computer Engineering",
            leadingIcon = Icons.Rounded.School
        )
        Spacer(Modifier.height(18.dp))
        Text("Year of study", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudyTerm.entries.forEach { term ->
                TermChip(
                    term = term,
                    selected = state.draft.studyTerm == term,
                    onClick = { viewModel.updateStudyTerm(term) }
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        ErrorMessage(state.error)
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            "Complete profile",
            viewModel::completeProfile,
            loading = state.isLoading,
            icon = Icons.AutoMirrored.Rounded.ArrowForward
        )
    }
}

@Composable
fun CompleteScreen(state: OnboardingUiState, onFinish: () -> Unit) {
    ScreenContainer(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(SpotraSuccess.copy(alpha = .13f), CircleShape)
                .border(1.dp, SpotraSuccess.copy(alpha = .5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, null, tint = SpotraSuccess, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "You're all set, ${state.draft.firstName}!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your account is ready. Here's what Spotra knows about you.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .border(1.dp, SpotraBorder, RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryRow("Name", "${state.draft.firstName} ${state.draft.lastName}")
            SummaryRow("Email", state.draft.email)
            SummaryRow("Program", state.draft.program)
            SummaryRow("Term", state.draft.studyTerm?.label.orEmpty())
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Find a study spot", onFinish, icon = Icons.Rounded.Map)
    }
}

@Composable
private fun TermChip(term: StudyTerm, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) SpotraPrimaryContainer else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(10.dp)
            )
            .border(
                BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else SpotraBorder),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 12.dp)
    ) {
        Text(
            term.label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoCard(text: String) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .border(1.dp, SpotraBorder, RoundedCornerShape(10.dp))
            .padding(14.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun FormScreen(onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, top = 72.dp, end = 24.dp, bottom = 24.dp)
                    .widthIn(max = 480.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
                content = content
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 4.dp, top = 6.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        }
    }
}

@Composable
private fun ScreenContainer(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}
