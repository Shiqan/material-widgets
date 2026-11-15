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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeatherPillAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherPillAppWidget()
}

/**
 * Stadium Pill Weather Widget:
 * - Full-rounded stadium capsule (`secondaryContainer`)
 * - Left: large scalloped sun / weather badge
 * - Right: Date ("Thu, Jan 30"), bold temperature ("30°"), and H / L temperature range
 */
class WeatherPillAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(DpSize(280.dp, 130.dp)))

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
                WeatherPillWidgetContent(
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
                WeatherPillWidgetContent(
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
        showWidgetErrorFallback(context, appWidgetId, "Weather pill widget error")
    }
}

@Composable
fun WeatherPillWidgetContent(
    weather: WeatherSnapshot,
    useFahrenheit: Boolean = false,
    today: LocalDate = LocalDate.now(),
) {
    val size = LocalSize.current
    val pillRadius = (size.height / 2).coerceAtLeast(36.dp)
    val badgeSize = (size.height - 28.dp).coerceIn(68.dp, 108.dp)
    val dateLabel = today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(pillRadius)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Scalloped sun / weather badge (tap to refresh live weather)
        if (weather.iconRes == R.drawable.ic_weather_sunny) {
            Image(
                provider = ImageProvider(R.drawable.ic_weather_sun_scalloped),
                contentDescription = "Refresh ${weather.condition}",
                modifier = GlanceModifier
                    .size(badgeSize)
                    .clickable(actionRunCallback<CycleWeatherAction>()),
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .size(badgeSize)
                    .background(
                        imageProvider = ImageProvider(R.drawable.bg_scalloped_cookie),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiaryContainer),
                    )
                    .clickable(actionRunCallback<CycleWeatherAction>()),
            ) {
                Image(
                    provider = ImageProvider(weather.iconRes),
                    contentDescription = "Refresh ${weather.condition}",
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                    modifier = GlanceModifier.size(badgeSize * 0.48f),
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(14.dp))

        // Right: Date header, bold large temperature, and H / L range
        Column(
            modifier = GlanceModifier.defaultWeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dateLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
                modifier = GlanceModifier.clickable(actionRunCallback<CycleWeatherAction>()),
            )

            Text(
                text = formatTemp(weather.tempCelsius, useFahrenheit),
                style = TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                modifier = GlanceModifier.clickable(actionRunCallback<ToggleTemperatureUnitAction>()),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "H ${formatTemp(weather.highCelsius, useFahrenheit)}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "L ${formatTemp(weather.lowCelsius, useFahrenheit)}",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 130)
@Composable
fun WeatherPillWidgetPreview() {
    MaterialYouGlanceTheme {
        WeatherPillWidgetContent(
            weather = SAMPLE_WEATHER_STATES[1],
        )
    }
}
