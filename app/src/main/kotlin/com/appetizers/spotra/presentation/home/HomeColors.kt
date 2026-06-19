package com.appetizers.spotra.presentation.home

import androidx.compose.ui.graphics.Color
import com.appetizers.spotra.domain.model.StudyMode

internal enum class NavTab { Map, Explore, Social, Profile }

internal val HomeBackground = Color(0xFFF8F7F3)
internal val Ink = Color(0xFF171A3C)
internal val HeaderMuted = Color(0xFFA8A6AA)
internal val MutedText = Color(0xFF96949A)
internal val SwitcherTrack = Color(0xFFEDE9E0)
internal val SoloBlue = Color(0xFF4355E8)
internal val SensorScoreBackground = Color(0xFF293B8E)
internal val GroupGreen = Color(0xFF21A46F)
internal val GroupHeaderGreen = Color(0xFF115C3B)
internal val GroupHeaderChip = Color(0xFF3A7B62)
internal val GroupBackButton = Color(0xFF0D4A31)
internal val GroupHeaderSecondary = Color(0xFFB7D0C3)
internal val GroupMoreAvatar = Color(0xFF6A8176)
internal val GroupBestFitBackground = Color(0xFFEFFAF4)
internal val GroupCardBorder = Color(0xFFEDE9E0)
internal val GroupFeatureChipBackground = Color(0xFFEDE9E0)
internal val GroupSpotIconGreen = Color(0xFFDDF6EA)
internal val GroupSpotIconYellow = Color(0xFFFFF1D2)
internal val ModerateFitBackground = Color(0xFFFFF1D2)
internal val ModerateFitText = Color(0xFF8A5200)
internal val InviteInputBackground = Color(0xFFF1F0ED)
internal val DisabledSend = Color(0xFF9ACFB9)
internal val MapBackground = Color(0xFFEAE6DB)
internal val MapLoadingBackground = Color(0xFFE8E8E8)
internal val MapBlock = Color(0xFFCFC9B6)
internal val CardBackground = Color(0xFFF0F1FF)
internal val QuietPill = Color(0xFFDDF6EA)
internal val QuietText = Color(0xFF137B4B)
internal val LibraryGreen = Color(0xFF249B6C)
internal val SLCOrange = Color(0xFFD89209)
internal val DPAtriumRed = Color(0xFFD83D3C)
internal val NavMuted = Color(0xFFC2BEB5)
internal val StarGold = Color(0xFFF8BC3B)
internal val CheckInHeader = Color(0xFF1B1A31)
internal val HeaderButton = Color(0xFF3A394F)
internal val HeaderSecondary = Color(0xFF9D9AA9)
internal val CheckedInPill = Color(0xFF244F50)
internal val CheckedInDot = Color(0xFF5AE5A0)
internal val CheckedInText = Color(0xFF72E6A7)
internal val SectionLabel = Color(0xFFA5A5A7)
internal val DividerLine = Color(0xFFECE9E3)
internal val SelfRowBackground = Color(0xFFF3F3FD)
internal val SelfPillBackground = Color(0xFFE8E9FF)
internal val BuddyPill = Color(0xFFF0F0FF)
internal val RequestedPill = Color(0xFFE7F8EF)
internal val BodyText = Color(0xFF5F5C62)
internal val PurpleAvatar = Color(0xFFA43CE2)

internal fun StudyMode.accentColor(): Color = when (this) {
    StudyMode.Solo -> SoloBlue
    StudyMode.Group -> GroupGreen
}

internal fun avatarColorFor(id: String, index: Int = 0): Color = when (id) {
    "you" -> SoloBlue
    "akshat" -> PurpleAvatar
    "eric" -> LibraryGreen
    "raghav" -> SLCOrange
    "pavan" -> Color(0xFF0EA5E9)
    "edmond" -> Color(0xFF14B8A6)
    "maya" -> Color(0xFF8B5CF6)
    "leah" -> DPAtriumRed
    "kai" -> Color(0xFF64748B)
    "priya" -> Color(0xFFEF4444)
    "ben" -> Color(0xFFF97316)
    "lina" -> Color(0xFF22C55E)
    else -> listOf(
        Color(0xFF14B8A6),
        Color(0xFF8B5CF6),
        Color(0xFFEF4444),
        Color(0xFFF97316),
        Color(0xFF22C55E)
    )[index.coerceAtLeast(0) % 5]
}
