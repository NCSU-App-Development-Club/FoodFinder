package org.appdevncsu.foodfinder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
)

@Immutable
data class FoodFinderExtendedColors(
    val statusOpen: Color,
    val onStatusOpen: Color,
    val statusClosingSoon: Color,
    val onStatusClosingSoon: Color,
)

private val LightExtendedColors = FoodFinderExtendedColors(
    statusOpen = StatusOpenLight,
    onStatusOpen = OnStatusOpenLight,
    statusClosingSoon = StatusClosingSoonLight,
    onStatusClosingSoon = OnStatusClosingSoonLight,
)

private val DarkExtendedColors = FoodFinderExtendedColors(
    statusOpen = StatusOpenDark,
    onStatusOpen = OnStatusOpenDark,
    statusClosingSoon = StatusClosingSoonDark,
    onStatusClosingSoon = OnStatusClosingSoonDark,
)

val LocalFoodFinderExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Accessor mirroring `MaterialTheme.colorScheme` / `MaterialTheme.typography`. */
val MaterialTheme.foodFinderExtended: FoodFinderExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFoodFinderExtendedColors.current

@Composable
fun FoodFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalFoodFinderExtendedColors provides extendedColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
