package com.example.materialwidgets.widget.weather

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
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
import com.example.materialwidgets.R
import com.example.materialwidgets.widget.theme.MaterialYouGlanceTheme
import com.example.materialwidgets.widget.util.showWidgetErrorFallback

data class WeatherSnapshot(
    val city: String,
    val condition: String,
    val tempCelsius: Int,
    val highCelsius: Int,
    val lowCelsius: Int,
    val humidityPercent: Int,
    val windKmh: Int,
    val uvIndex: String,
    @param:DrawableRes val iconRes: Int,
    val forecastHours: List<Pair<String, Int>>,
)

val SAMPLE_WEATHER_STATES = listOf(
    WeatherSnapshot(
        city = "San Francisco",
        condition = "Mostly Sunny",
        tempCelsius = 22,
        highCelsius = 24,
        lowCelsius = 14,
        humidityPercent = 58,
        windKmh = 14,
        uvIndex = "High",
        iconRes = R.drawable.ic_weather_sunny,
        forecastHours = listOf("12 PM" to 22, "3 PM" to 24, "6 PM" to 19, "9 PM" to 16),
    ),
    WeatherSnapshot(
        city = "Amsterdam",
        condition = "Scattered Clouds",
        tempCelsius = 17,
        highCelsius = 19,
        lowCelsius = 11,
        humidityPercent = 72,
        windKmh = 21,
        uvIndex = "Mod",
        iconRes = R.drawable.ic_weather_cloudy,
        forecastHours = listOf("12 PM" to 17, "3 PM" to 18, "6 PM" to 15, "9 PM" to 13),
    ),
    WeatherSnapshot(
        city = "Tokyo",
        condition = "Light Rain",
        tempCelsius = 15,
        highCelsius = 18,
        lowCelsius = 12,
        humidityPercent = 86,
        windKmh = 18,
        uvIndex = "Low",
        iconRes = R.drawable.ic_weather_rainy,
        forecastHours = listOf("12 PM" to 15, "3 PM" to 16, "6 PM" to 14, "9 PM" to 13),
    ),
)

class WeatherAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherAppWidget()
}

class WeatherAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val index = (prefs[KEY_WEATHER_INDEX] ?: 0).mod(SAMPLE_WEATHER_STATES.size)
            val useFahrenheit = prefs[KEY_USE_FAHRENHEIT] ?: false
            MaterialYouGlanceTheme {
                WeatherWidgetContent(
                    weather = SAMPLE_WEATHER_STATES[index],
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
        showWidgetErrorFallback(context, appWidgetId, "Weather widget error")
    }

    companion object {
        val KEY_WEATHER_INDEX = intPreferencesKey("weather_state_index")
        val KEY_USE_FAHRENHEIT = booleanPreferencesKey("weather_use_fahrenheit")
    }
}

class CycleWeatherAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[WeatherAppWidget.KEY_WEATHER_INDEX] ?: 0
            prefs[WeatherAppWidget.KEY_WEATHER_INDEX] = (current + 1) % SAMPLE_WEATHER_STATES.size
        }
        WeatherAppWidget().update(context, glanceId)
    }
}

class ToggleTemperatureUnitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[WeatherAppWidget.KEY_USE_FAHRENHEIT] ?: false
            prefs[WeatherAppWidget.KEY_USE_FAHRENHEIT] = !current
        }
        WeatherAppWidget().update(context, glanceId)
    }
}

private fun formatTemp(celsius: Int, useFahrenheit: Boolean): String {
    val value = if (useFahrenheit) (celsius * 9 / 5) + 32 else celsius
    return "$value°"
}

@Composable
fun WeatherWidgetContent(
    weather: WeatherSnapshot,
    useFahrenheit: Boolean = false,
) {
    val size = LocalSize.current
    val unitLabel = if (useFahrenheit) "F" else "C"

    if (size.width >= 240.dp) {
        // Wide 4x2 expressive Material You weather card with forecast & metric pills
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(28.dp)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.primaryContainer)
                                .cornerRadius(12.dp)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .clickable(actionRunCallback<CycleWeatherAction>()),
                        ) {
                            Text(
                                text = weather.city,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onPrimaryContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = weather.condition,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSecondaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${formatTemp(weather.tempCelsius, useFahrenheit)}$unitLabel",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSecondaryContainer,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = GlanceModifier.clickable(actionRunCallback<ToggleTemperatureUnitAction>()),
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = "H:${formatTemp(weather.highCelsius, useFahrenheit)}  L:${formatTemp(weather.lowCelsius, useFahrenheit)}",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }

                // Scalloped sun/weather badge + refresh action
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(56.dp)
                        .background(
                            imageProvider = ImageProvider(R.drawable.bg_scalloped_cookie),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiaryContainer),
                        )
                        .clickable(actionRunCallback<CycleWeatherAction>()),
                ) {
                    Image(
                        provider = ImageProvider(weather.iconRes),
                        contentDescription = weather.condition,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                        modifier = GlanceModifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Hourly forecast / metric pills row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                weather.forecastHours.forEachIndexed { idx, (hourLabel, tempC) ->
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(GlanceTheme.colors.surface)
                            .cornerRadius(16.dp)
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = hourLabel,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                        Text(
                            text = formatTemp(tempC, useFahrenheit),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    if (idx < weather.forecastHours.lastIndex) {
                        Spacer(modifier = GlanceModifier.width(6.dp))
                    }
                }
            }
        }
    } else {
        // Compact 2x2 Material You Weather Tile
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(28.dp)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = weather.city,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = weather.condition,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(40.dp)
                        .background(
                            imageProvider = ImageProvider(R.drawable.bg_scalloped_cookie),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiaryContainer),
                        )
                        .clickable(actionRunCallback<CycleWeatherAction>()),
                ) {
                    Image(
                        provider = ImageProvider(weather.iconRes),
                        contentDescription = weather.condition,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                        modifier = GlanceModifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "${formatTemp(weather.tempCelsius, useFahrenheit)}$unitLabel",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionRunCallback<ToggleTemperatureUnitAction>()),
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(12.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable(actionRunCallback<CycleWeatherAction>()),
                ) {
                    Text(
                        text = "${formatTemp(weather.highCelsius, useFahrenheit)}/${formatTemp(weather.lowCelsius, useFahrenheit)}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 160)
@Preview(widthDp = 280, heightDp = 155)
@Composable
fun WeatherWidgetPreview() {
    MaterialYouGlanceTheme {
        WeatherWidgetContent(weather = SAMPLE_WEATHER_STATES.first())
    }
}
