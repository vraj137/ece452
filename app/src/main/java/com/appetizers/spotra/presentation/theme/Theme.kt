package com.appetizers.spotra.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SpotraPrimary,
    secondary = SpotraSecondary,
    background = SpotraBackground,
    surface = SpotraSurface
)

@Composable
fun SpotraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
