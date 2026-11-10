package com.scann.thekami.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Selectable colour themes. To add one: add an entry here plus a light and a dark scheme below. */
enum class AppTheme(val label: String) {
    DEFAULT("Blue"),
    DUSK("Dusk");

    fun colorScheme(dark: Boolean): ColorScheme = when (this) {
        DEFAULT -> if (dark) BlueDark else BlueLight
        DUSK -> if (dark) DuskDark else DuskLight
    }
}

private val BlueLight = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF705575),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41474E),
    outline = Color(0xFF71777F),
    outlineVariant = Color(0xFFC1C7CF)
)

private val BlueDark = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497A),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFDDBBE0),
    onTertiary = Color(0xFF3F2844),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E9),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E9),
    surfaceVariant = Color(0xFF41474E),
    onSurfaceVariant = Color(0xFFC1C7CF),
    outline = Color(0xFF8B9199),
    outlineVariant = Color(0xFF41474E)
)

private val DuskLight = lightColorScheme(
    primary = Color(0xFF676386),
    onPrimary = Color(0xFFF5F4F7),
    primaryContainer = Color(0xFFA5B2EB),
    onPrimaryContainer = Color(0xFF2B2D3B),
    secondary = Color(0xFF9A5A2B),
    onSecondary = Color(0xFFF5F4F7),
    secondaryContainer = Color(0xFFDA935D),
    onSecondaryContainer = Color(0xFF2B2D3B),
    tertiary = Color(0xFFDDDE68),
    onTertiary = Color(0xFF2B2D3B),
    background = Color(0xFFF5F4F7),
    onBackground = Color(0xFF2B2D3B),
    surface = Color(0xFFF5F4F7),
    onSurface = Color(0xFF2B2D3B),
    surfaceVariant = Color(0xFFE2E1EA),
    onSurfaceVariant = Color(0xFF494C65),
    outline = Color(0xFF676386),
    outlineVariant = Color(0xFFC9C8D6),
    inverseSurface = Color(0xFF2B2D3B),
    inverseOnSurface = Color(0xFFF5F4F7),
    inversePrimary = Color(0xFFA5B2EB),
    surfaceDim = Color(0xFFD9D8E0),
    surfaceBright = Color(0xFFF5F4F7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0EFF4),
    surfaceContainer = Color(0xFFEAE9F0),
    surfaceContainerHigh = Color(0xFFE4E3EB),
    surfaceContainerHighest = Color(0xFFDEDDE6)
)

private val DuskDark = darkColorScheme(
    primary = Color(0xFFA5B2EB),
    onPrimary = Color(0xFF2B2D3B),
    primaryContainer = Color(0xFF494C65),
    onPrimaryContainer = Color(0xFFF5F4F7),
    secondary = Color(0xFFDA935D),
    onSecondary = Color(0xFF2B2D3B),
    secondaryContainer = Color(0xFF676386),
    onSecondaryContainer = Color(0xFFF5F4F7),
    tertiary = Color(0xFFDDDE68),
    onTertiary = Color(0xFF2B2D3B),
    background = Color(0xFF2B2D3B),
    onBackground = Color(0xFFF5F4F7),
    surface = Color(0xFF2B2D3B),
    onSurface = Color(0xFFF5F4F7),
    surfaceVariant = Color(0xFF494C65),
    onSurfaceVariant = Color(0xFFC9C9D8),
    outline = Color(0xFF8A88AD),
    outlineVariant = Color(0xFF494C65),
    inverseSurface = Color(0xFFF5F4F7),
    inverseOnSurface = Color(0xFF2B2D3B),
    inversePrimary = Color(0xFF676386),
    surfaceDim = Color(0xFF22242F),
    surfaceBright = Color(0xFF494C65),
    surfaceContainerLowest = Color(0xFF242633),
    surfaceContainerLow = Color(0xFF2F3140),
    surfaceContainer = Color(0xFF353749),
    surfaceContainerHigh = Color(0xFF42445C),
    surfaceContainerHighest = Color(0xFF494C65)
)
