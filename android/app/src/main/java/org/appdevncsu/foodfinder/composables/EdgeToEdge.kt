package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Central edge-to-edge policy for the app.
 *
 * [ScreenScaffold] applies [scaffoldContentWindowInsets] (top + horizontal) and leaves the
 * bottom navigation-bar inset for scrolling content, so lists draw behind the transparent
 * gesture bar and pad their last item instead of being cut off.
 */
val scaffoldContentWindowInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)

/** Bottom navigation-bar insets shared by all scrolling content. */
val bottomNavBarInsets: WindowInsets
    @Composable get() = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)

/** Content padding for scrolling containers so the last item clears the navigation bar. */
@Composable
fun bottomNavBarContentPadding(): PaddingValues = bottomNavBarInsets.asPaddingValues()

/** Marks the bottom nav-bar insets as consumed so nested layouts don't re-apply them. */
@Composable
fun Modifier.consumeBottomNavBarInsets(): Modifier = consumeWindowInsets(bottomNavBarInsets)

/**
 * Padding for full-screen non-scrolling content (e.g. [ErrorState]) so centered content
 * and actions like Retry are not obscured by the navigation bar.
 */
@Composable
fun Modifier.padBottomNavBarInsets(): Modifier =
    windowInsetsPadding(bottomNavBarInsets).consumeWindowInsets(bottomNavBarInsets)
