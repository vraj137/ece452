package com.appetizers.spotra.presentation.home

import androidx.compose.ui.graphics.Color
import com.appetizers.spotra.domain.model.StudyMode

internal val HomeBackground = Color(0xFFF8F7F3)
internal val Ink = Color(0xFF101828)
internal val HeaderMuted = Color(0xFF667085)
internal val MutedText = Color(0xFF5D6472)
internal val SwitcherTrack = Color(0xFFF0F1F3)
internal val SoloBlue = Color(0xFF2454D7)
internal val GroupGreen = Color(0xFF087F6B)
internal val GroupMoreAvatar = Color(0xFF6A8176)
internal val GroupBestFitBackground = Color(0xFFF2FBF8)
internal val GroupCardBorder = Color(0xFFD0D5DD)
internal val GroupSpotIconGreen = Color(0xFFDDF6EA)
internal val ModerateFitBackground = Color(0xFFFFF1D2)
internal val ModerateFitText = Color(0xFF7A4700)
internal val InviteInputBackground = Color(0xFFF1F0ED)
internal val MapBackground = Color(0xFFEAE6DB)
internal val MapLoadingBackground = Color(0xFFE8E8E8)
internal val MapBlock = Color(0xFFCFC9B6)
internal val CardBackground = Color(0xFFF3F5FF)
internal val QuietPill = Color(0xFFDDF6EA)
internal val QuietText = Color(0xFF086B5C)
internal val LibraryGreen = Color(0xFF249B6C)
internal val SLCOrange = Color(0xFFD89209)
internal val DPAtriumRed = Color(0xFFD83D3C)
internal val NavMuted = Color(0xFF667085)
internal val StarGold = Color(0xFFF8BC3B)
internal val CheckInHeader = Color(0xFF1B1A31)
internal val HeaderButton = Color(0xFF3A394F)
internal val HeaderSecondary = Color(0xFF9D9AA9)
internal val CheckedInPill = Color(0xFF244F50)
internal val CheckedInDot = Color(0xFF5AE5A0)
internal val CheckedInText = Color(0xFF72E6A7)
internal val SectionLabel = Color(0xFF475467)
internal val DividerLine = Color(0xFFD0D5DD)
internal val SelfRowBackground = Color(0xFFF3F3FD)
internal val SelfPillBackground = Color(0xFFE8E9FF)
internal val BuddyPill = Color(0xFFF0F0FF)
internal val RequestedPill = Color(0xFFE7F8EF)
internal val BodyText = Color(0xFF475467)
internal val PurpleAvatar = Color(0xFFA43CE2)

internal fun StudyMode.accentColor(): Color = when (this) {
    StudyMode.Solo -> SoloBlue
    StudyMode.Group -> GroupGreen
}

private val AvatarPalette = listOf(
    PurpleAvatar,
    LibraryGreen,
    SLCOrange,
    Color(0xFF0EA5E9),
    Color(0xFF14B8A6),
    Color(0xFF8B5CF6),
    DPAtriumRed,
    Color(0xFF64748B),
    Color(0xFFEF4444),
    Color(0xFFF97316),
    Color(0xFF22C55E),
)

internal fun avatarColorFor(id: String): Color =
    if (id == "you") SoloBlue else AvatarPalette[kotlin.math.abs(id.hashCode()) % AvatarPalette.size]
