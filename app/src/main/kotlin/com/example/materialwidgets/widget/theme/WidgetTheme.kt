package com.example.materialwidgets.widget.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.glance.GlanceComposable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import com.example.materialwidgets.ui.theme.DarkColorScheme
import com.example.materialwidgets.ui.theme.LightColorScheme

/**
 * Fallback Material 3 ColorProviders for pre-Android 12 devices,
 * respecting light and dark mode automatically.
 */
val FallbackWidgetColorProviders = ColorProviders(
    light = LightColorScheme,
    dark = DarkColorScheme,
)

/**
 * Material You (Dynamic Color) Glance theme that automatically inherits the
 * wallpaper / system color scheme selected by Android on API 31+ (Android 12+)
 * and respects light/dark mode seamlessly across all API levels.
 */
@Composable
fun MaterialYouGlanceTheme(
    dynamicColor: Boolean = true,
    content: @GlanceComposable @Composable () -> Unit,
) {
    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceTheme.colors
    } else {
        FallbackWidgetColorProviders
    }

    GlanceTheme(
        colors = colors,
        content = content,
    )
}
