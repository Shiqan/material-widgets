package dev.shiqan.materialwidgets.widget.weather

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.shiqan.materialwidgets.R
import dev.shiqan.materialwidgets.widget.theme.MaterialYouGlanceTheme
import dev.shiqan.materialwidgets.widget.util.showWidgetErrorFallback

class WeatherTiltedPillAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherTiltedPillAppWidget()
}

/**
 * Pixel-inspired 2x2 Tilted Weather Pill Widget:
 * - Rounded rectangle rotated 45° (`secondaryContainer`)
 * - Centered current weather icon (tap to refresh live weather)
 * - Bold temperature readout below (tap to toggle °C / °F)
 */
class WeatherTiltedPillAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(DpSize(160.dp, 160.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val isPreviewPin = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id) == AppWidgetManager.INVALID_APPWIDGET_ID
        }.getOrDefault(false)
        if (!isPreviewPin) {
            WeatherRepository.syncIfStale(context)
        }
        val fallbackSnapshot = WeatherRepository.getCachedWeather(context)
        val fallbackFahrenheit = WeatherRepository.isFahrenheit(context)

        provideContent {
            val prefs = currentState<Preferences>()
            val useFahrenheit = prefs[WeatherAppWidget.KEY_USE_FAHRENHEIT] ?: fallbackFahrenheit
            val weather = WeatherRepository.resolveSnapshotFromPrefs(prefs, fallbackSnapshot)

            MaterialYouGlanceTheme {
                WeatherTiltedPillWidgetContent(
                    weather = weather,
                    useFahrenheit = useFahrenheit,
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val snapshot = WeatherRepository.getCachedWeather(context)
        val useFahrenheit = WeatherRepository.isFahrenheit(context)
        provideContent {
            MaterialYouGlanceTheme {
                WeatherTiltedPillWidgetContent(
                    weather = snapshot,
                    useFahrenheit = useFahrenheit,
                )
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        showWidgetErrorFallback(context, appWidgetId, "Tilted weather pill error")
    }
}

@Composable
fun WeatherTiltedPillWidgetContent(
    weather: WeatherSnapshot,
    useFahrenheit: Boolean = false,
) {
    val size = LocalSize.current
    val containerSize = size.width.coerceAtMost(size.height).coerceAtLeast(110.dp)
    val lobeInset = containerSize * 0.11f
    val lobeBoxSize = containerSize * 0.44f
    val iconSize = (containerSize * 0.30f).coerceIn(32.dp, 68.dp)
    val tempSp = (containerSize.value * 0.23f).coerceIn(24f, 50f).sp

    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.fillMaxSize(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .size(containerSize)
                .appWidgetBackground()
                .background(
                    imageProvider = ImageProvider(R.drawable.bg_weather_tilted_pill),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.secondaryContainer),
                )
                .clickable(actionRunCallback<CycleWeatherAction>()),
        ) {
            // Top-right tilted lobe: current weather icon
            Box(
                contentAlignment = Alignment.TopEnd,
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(top = lobeInset, end = lobeInset),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(lobeBoxSize)
                        .clickable(actionRunCallback<CycleWeatherAction>()),
                ) {
                    if (weather.iconRes == R.drawable.ic_weather_sunny) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_weather_sun_scalloped),
                            contentDescription = "Refresh ${weather.condition}",
                            modifier = GlanceModifier.size(iconSize),
                        )
                    } else {
                        Image(
                            provider = ImageProvider(weather.iconRes),
                            contentDescription = "Refresh ${weather.condition}",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                            modifier = GlanceModifier.size(iconSize),
                        )
                    }
                }
            }

            // Bottom-left tilted lobe: bold current temperature
            Box(
                contentAlignment = Alignment.BottomStart,
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(bottom = lobeInset, start = lobeInset),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(lobeBoxSize)
                        .clickable(actionRunCallback<ToggleTemperatureUnitAction>()),
                ) {
                    Text(
                        text = formatTemp(weather.tempCelsius, useFahrenheit),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = tempSp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 160)
@Composable
fun WeatherTiltedPillWidgetPreview() {
    MaterialYouGlanceTheme {
        WeatherTiltedPillWidgetContent(
            weather = SAMPLE_WEATHER_STATES[1],
        )
    }
}
