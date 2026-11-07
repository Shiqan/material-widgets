package com.example.materialwidgets.widget.weather

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

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
    val isLive: Boolean = false,
)

val SAMPLE_WEATHER_STATES = listOf(
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

/**
 * Fetches live weather for a user-configured city name using Open-Meteo's free API
 * (no API key or Android location permissions required) and synchronizes state
 * between the app and all pinned [WeatherAppWidget] instances.
 */
object WeatherRepository {
    private const val PREFS_NAME = "weather_widget_prefs"
    private const val KEY_CONFIGURED_CITY = "configured_city"
    private const val KEY_RESOLVED_CITY = "resolved_city"
    private const val KEY_LATITUDE = "latitude"
    private const val KEY_LONGITUDE = "longitude"
    private const val KEY_USE_FAHRENHEIT = "use_fahrenheit"
    private const val KEY_CONDITION = "condition"
    private const val KEY_TEMP_C = "temp_c"
    private const val KEY_HIGH_C = "high_c"
    private const val KEY_LOW_C = "low_c"
    private const val KEY_HUMIDITY = "humidity"
    private const val KEY_WIND_KMH = "wind_kmh"
    private const val KEY_UV_INDEX = "uv_index"
    private const val KEY_WEATHER_CODE = "weather_code"
    private const val KEY_FORECAST_SERIALIZED = "forecast_serialized"
    private const val KEY_LAST_UPDATED_MS = "last_updated_ms"

    const val DEFAULT_CITY = "Amsterdam"
    private const val STALE_THRESHOLD_MS = 30 * 60 * 1000L // 30 minutes

    private val FALLBACK_COORDINATES = mapOf(
        "amsterdam" to Triple("Amsterdam", 52.3676, 4.9041),
        "san francisco" to Triple("San Francisco", 37.7749, -122.4194),
        "tokyo" to Triple("Tokyo", 35.6762, 139.6503),
        "london" to Triple("London", 51.5074, -0.1278),
        "new york" to Triple("New York", 40.7128, -74.0060),
        "berlin" to Triple("Berlin", 52.5200, 13.4050),
        "paris" to Triple("Paris", 48.8566, 2.3522),
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getConfiguredCity(context: Context): String =
        prefs(context).getString(KEY_CONFIGURED_CITY, DEFAULT_CITY)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_CITY

    fun isFahrenheit(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USE_FAHRENHEIT, false)

    fun getLastUpdatedMs(context: Context): Long =
        prefs(context).getLong(KEY_LAST_UPDATED_MS, 0L)

    fun getCachedWeather(context: Context): WeatherSnapshot {
        val p = prefs(context)
        val lastUpdated = p.getLong(KEY_LAST_UPDATED_MS, 0L)
        val configuredCity = getConfiguredCity(context)
        if (lastUpdated == 0L) {
            return SAMPLE_WEATHER_STATES.first().copy(city = configuredCity)
        }
        val city = p.getString(KEY_RESOLVED_CITY, configuredCity) ?: configuredCity
        val condition = p.getString(KEY_CONDITION, "Partly Cloudy") ?: "Partly Cloudy"
        val tempC = p.getInt(KEY_TEMP_C, 18)
        val highC = p.getInt(KEY_HIGH_C, 21)
        val lowC = p.getInt(KEY_LOW_C, 12)
        val humidity = p.getInt(KEY_HUMIDITY, 65)
        val wind = p.getInt(KEY_WIND_KMH, 15)
        val uv = p.getString(KEY_UV_INDEX, "Mod") ?: "Mod"
        val code = p.getInt(KEY_WEATHER_CODE, 2)
        val forecastRaw = p.getString(KEY_FORECAST_SERIALIZED, null)
        val forecast = deserializeForecast(forecastRaw)
            .ifEmpty { SAMPLE_WEATHER_STATES.first().forecastHours }

        return WeatherSnapshot(
            city = city,
            condition = condition,
            tempCelsius = tempC,
            highCelsius = highC,
            lowCelsius = lowC,
            humidityPercent = humidity,
            windKmh = wind,
            uvIndex = uv,
            iconRes = iconForWeatherCode(code),
            forecastHours = forecast,
            isLive = true,
        )
    }

    suspend fun setTemperatureUnit(context: Context, useFahrenheit: Boolean) {
        prefs(context).edit().putBoolean(KEY_USE_FAHRENHEIT, useFahrenheit).apply()
        pushStateToWidgets(context)
    }

    suspend fun toggleTemperatureUnit(context: Context): Boolean {
        val next = !isFahrenheit(context)
        setTemperatureUnit(context, next)
        return next
    }

    suspend fun updateCityAndFetch(context: Context, cityQuery: String): Result<WeatherSnapshot> {
        val cleaned = cityQuery.trim().ifBlank { DEFAULT_CITY }
        prefs(context).edit()
            .putString(KEY_CONFIGURED_CITY, cleaned)
            .remove(KEY_LATITUDE)
            .remove(KEY_LONGITUDE)
            .apply()
        return fetchAndSyncWeather(context, forceCityLookup = true)
    }

    suspend fun syncIfStale(context: Context) {
        val now = System.currentTimeMillis()
        val last = getLastUpdatedMs(context)
        if (last == 0L || (now - last) > STALE_THRESHOLD_MS) {
            fetchAndSyncWeather(context, forceCityLookup = false)
        }
    }

    suspend fun fetchAndSyncWeather(
        context: Context,
        forceCityLookup: Boolean = false,
    ): Result<WeatherSnapshot> = withContext(Dispatchers.IO) {
        val p = prefs(context)
        val configuredCity = getConfiguredCity(context)

        try {
            val (resolvedCity, lat, lon) = resolveCoordinates(p, configuredCity, forceCityLookup)
            val snapshot = fetchOpenMeteoForecast(resolvedCity, lat, lon)

            p.edit()
                .putString(KEY_RESOLVED_CITY, snapshot.city)
                .putString(KEY_LATITUDE, lat.toString())
                .putString(KEY_LONGITUDE, lon.toString())
                .putString(KEY_CONDITION, snapshot.condition)
                .putInt(KEY_TEMP_C, snapshot.tempCelsius)
                .putInt(KEY_HIGH_C, snapshot.highCelsius)
                .putInt(KEY_LOW_C, snapshot.lowCelsius)
                .putInt(KEY_HUMIDITY, snapshot.humidityPercent)
                .putInt(KEY_WIND_KMH, snapshot.windKmh)
                .putString(KEY_UV_INDEX, snapshot.uvIndex)
                .putInt(KEY_WEATHER_CODE, weatherCodeForIcon(snapshot.iconRes))
                .putString(KEY_FORECAST_SERIALIZED, serializeForecast(snapshot.forecastHours))
                .putLong(KEY_LAST_UPDATED_MS, System.currentTimeMillis())
                .apply()

            pushStateToWidgets(context)
            Result.success(snapshot)
        } catch (e: Exception) {
            // Ensure widgets still show the configured city name even when offline
            pushStateToWidgets(context)
            Result.failure(e)
        }
    }

    suspend fun pushStateToWidgets(context: Context) {
        val snapshot = getCachedWeather(context)
        val useFahrenheit = isFahrenheit(context)
        val serializedForecast = serializeForecast(snapshot.forecastHours)
        val code = weatherCodeForIcon(snapshot.iconRes)
        val lastUpdated = getLastUpdatedMs(context)

        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(WeatherAppWidget::class.java)
        for (glanceId in glanceIds) {
            updateAppWidgetState(context, glanceId) { glancePrefs ->
                glancePrefs[WeatherAppWidget.KEY_USE_FAHRENHEIT] = useFahrenheit
                glancePrefs[WeatherAppWidget.KEY_CITY] = snapshot.city
                glancePrefs[WeatherAppWidget.KEY_CONDITION] = snapshot.condition
                glancePrefs[WeatherAppWidget.KEY_TEMP_C] = snapshot.tempCelsius
                glancePrefs[WeatherAppWidget.KEY_HIGH_C] = snapshot.highCelsius
                glancePrefs[WeatherAppWidget.KEY_LOW_C] = snapshot.lowCelsius
                glancePrefs[WeatherAppWidget.KEY_HUMIDITY] = snapshot.humidityPercent
                glancePrefs[WeatherAppWidget.KEY_WIND_KMH] = snapshot.windKmh
                glancePrefs[WeatherAppWidget.KEY_UV_INDEX] = snapshot.uvIndex
                glancePrefs[WeatherAppWidget.KEY_WEATHER_CODE] = code
                glancePrefs[WeatherAppWidget.KEY_FORECAST] = serializedForecast
                glancePrefs[WeatherAppWidget.KEY_LAST_UPDATED] = lastUpdated
            }
            WeatherAppWidget().update(context, glanceId)
        }
    }

    private fun resolveCoordinates(
        p: android.content.SharedPreferences,
        cityQuery: String,
        forceLookup: Boolean,
    ): Triple<String, Double, Double> {
        if (!forceLookup) {
            val cachedLat = p.getString(KEY_LATITUDE, null)?.toDoubleOrNull()
            val cachedLon = p.getString(KEY_LONGITUDE, null)?.toDoubleOrNull()
            val cachedName = p.getString(KEY_RESOLVED_CITY, null)
            if (cachedLat != null && cachedLon != null && !cachedName.isNullOrBlank()) {
                return Triple(cachedName, cachedLat, cachedLon)
            }
        }

        val encoded = URLEncoder.encode(cityQuery, "UTF-8")
        val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json"
        val geoJsonStr = httpGet(geoUrl)
        val geoJson = JSONObject(geoJsonStr)
        val results = geoJson.optJSONArray("results")
        if (results != null && results.length() > 0) {
            val first = results.getJSONObject(0)
            val name = first.optString("name", cityQuery)
            val lat = first.getDouble("latitude")
            val lon = first.getDouble("longitude")
            return Triple(name, lat, lon)
        }

        val fallback = FALLBACK_COORDINATES[cityQuery.lowercase(Locale.ROOT)]
        if (fallback != null) {
            return fallback
        }
        throw IllegalArgumentException("City '$cityQuery' not found")
    }

    private fun fetchOpenMeteoForecast(
        cityName: String,
        latitude: Double,
        longitude: Double,
    ): WeatherSnapshot {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
            "&hourly=temperature_2m" +
            "&daily=temperature_2m_max,temperature_2m_min,uv_index_max" +
            "&temperature_unit=celsius&wind_speed_unit=kmh&timezone=auto&forecast_days=2"

        val response = httpGet(url)
        val root = JSONObject(response)

        val current = root.getJSONObject("current")
        val tempC = current.optDouble("temperature_2m", 18.0).roundToInt()
        val humidity = current.optInt("relative_humidity_2m", 65)
        val windKmh = current.optDouble("wind_speed_10m", 12.0).roundToInt()
        val weatherCode = current.optInt("weather_code", 1)
        val currentIsoTime = current.optString("time", "")

        val daily = root.optJSONObject("daily")
        val highC = daily?.optJSONArray("temperature_2m_max")?.optDouble(0, tempC + 3.0)?.roundToInt()
            ?: (tempC + 3)
        val lowC = daily?.optJSONArray("temperature_2m_min")?.optDouble(0, tempC - 4.0)?.roundToInt()
            ?: (tempC - 4)
        val uvMax = daily?.optJSONArray("uv_index_max")?.optDouble(0, 4.0) ?: 4.0

        val hourly = root.optJSONObject("hourly")
        val forecastHours = parseUpcomingForecastHours(hourly, currentIsoTime, tempC)

        return WeatherSnapshot(
            city = cityName,
            condition = conditionLabelForCode(weatherCode),
            tempCelsius = tempC,
            highCelsius = maxOf(highC, tempC),
            lowCelsius = minOf(lowC, tempC),
            humidityPercent = humidity,
            windKmh = windKmh,
            uvIndex = uvLabel(uvMax),
            iconRes = iconForWeatherCode(weatherCode),
            forecastHours = forecastHours,
            isLive = true,
        )
    }

    private fun parseUpcomingForecastHours(
        hourly: JSONObject?,
        currentIsoTime: String,
        fallbackTempC: Int,
    ): List<Pair<String, Int>> {
        if (hourly == null) return SAMPLE_WEATHER_STATES.first().forecastHours
        val times = hourly.optJSONArray("time") ?: return SAMPLE_WEATHER_STATES.first().forecastHours
        val temps = hourly.optJSONArray("temperature_2m") ?: return SAMPLE_WEATHER_STATES.first().forecastHours

        val currentHourPrefix = currentIsoTime.take(13) // "YYYY-MM-DDTHH"
        var startIndex = 0
        if (currentHourPrefix.isNotEmpty()) {
            for (i in 0 until times.length()) {
                if (times.optString(i).startsWith(currentHourPrefix)) {
                    startIndex = i
                    break
                }
            }
        }

        val result = mutableListOf<Pair<String, Int>>()
        val offsets = listOf(1, 4, 7, 10)
        for (offset in offsets) {
            val idx = (startIndex + offset).coerceAtMost(times.length() - 1)
            if (idx in 0 until times.length() && idx < temps.length()) {
                val timeStr = times.optString(idx)
                val label = formatIsoHourLabel(timeStr)
                val temp = temps.optDouble(idx, fallbackTempC.toDouble()).roundToInt()
                result.add(label to temp)
            }
        }
        return if (result.size == 4) result else SAMPLE_WEATHER_STATES.first().forecastHours
    }

    private fun formatIsoHourLabel(isoTime: String): String {
        return try {
            val dt = LocalDateTime.parse(isoTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("h a", Locale.US))
        } catch (_: Exception) {
            isoTime.substringAfter('T', "12:00").take(5)
        }
    }

    private fun httpGet(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "MaterialWidgets/1.0")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code from $urlString")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    fun conditionLabelForCode(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1 -> "Mostly Sunny"
        2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Light Drizzle"
        56, 57 -> "Freezing Drizzle"
        61, 63 -> "Light Rain"
        65 -> "Heavy Rain"
        66, 67 -> "Freezing Rain"
        71, 73, 75, 77 -> "Snowfall"
        80, 81, 82 -> "Rain Showers"
        85, 86 -> "Snow Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Partly Cloudy"
    }

    @DrawableRes
    fun iconForWeatherCode(code: Int): Int = when (code) {
        0, 1 -> R.drawable.ic_weather_sunny
        2, 3, 45, 48 -> R.drawable.ic_weather_cloudy
        else -> R.drawable.ic_weather_rainy
    }

    private fun weatherCodeForIcon(@DrawableRes iconRes: Int): Int = when (iconRes) {
        R.drawable.ic_weather_sunny -> 0
        R.drawable.ic_weather_cloudy -> 2
        else -> 61
    }

    private fun uvLabel(uv: Double): String = when {
        uv < 3.0 -> "Low"
        uv < 6.0 -> "Mod"
        uv < 8.0 -> "High"
        else -> "V.High"
    }

    fun serializeForecast(hours: List<Pair<String, Int>>): String =
        hours.joinToString("|") { "${it.first}=${it.second}" }

    fun deserializeForecast(serialized: String?): List<Pair<String, Int>> {
        if (serialized.isNullOrBlank()) return emptyList()
        return serialized.split("|").mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) {
                val temp = parts[1].toIntOrNull() ?: return@mapNotNull null
                parts[0] to temp
            } else {
                null
            }
        }
    }
}

class WeatherAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherAppWidget()
}

class WeatherAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Trigger a live sync if we haven't fetched yet or if the cached forecast is stale
        WeatherRepository.syncIfStale(context)
        val fallbackSnapshot = WeatherRepository.getCachedWeather(context)
        val fallbackFahrenheit = WeatherRepository.isFahrenheit(context)

        provideContent {
            val prefs = currentState<Preferences>()
            val useFahrenheit = prefs[KEY_USE_FAHRENHEIT] ?: fallbackFahrenheit
            val city = prefs[KEY_CITY] ?: fallbackSnapshot.city
            val condition = prefs[KEY_CONDITION] ?: fallbackSnapshot.condition
            val tempC = prefs[KEY_TEMP_C] ?: fallbackSnapshot.tempCelsius
            val highC = prefs[KEY_HIGH_C] ?: fallbackSnapshot.highCelsius
            val lowC = prefs[KEY_LOW_C] ?: fallbackSnapshot.lowCelsius
            val humidity = prefs[KEY_HUMIDITY] ?: fallbackSnapshot.humidityPercent
            val wind = prefs[KEY_WIND_KMH] ?: fallbackSnapshot.windKmh
            val uv = prefs[KEY_UV_INDEX] ?: fallbackSnapshot.uvIndex
            val iconRes = prefs[KEY_WEATHER_CODE]?.let { WeatherRepository.iconForWeatherCode(it) }
                ?: fallbackSnapshot.iconRes
            val forecast = WeatherRepository.deserializeForecast(prefs[KEY_FORECAST])
                .ifEmpty { fallbackSnapshot.forecastHours }

            val weather = WeatherSnapshot(
                city = city,
                condition = condition,
                tempCelsius = tempC,
                highCelsius = highC,
                lowCelsius = lowC,
                humidityPercent = humidity,
                windKmh = wind,
                uvIndex = uv,
                iconRes = iconRes,
                forecastHours = forecast,
                isLive = (prefs[KEY_LAST_UPDATED] ?: 0L) > 0L || fallbackSnapshot.isLive,
            )

            MaterialYouGlanceTheme {
                WeatherWidgetContent(
                    weather = weather,
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
        val KEY_CITY = stringPreferencesKey("weather_city")
        val KEY_CONDITION = stringPreferencesKey("weather_condition")
        val KEY_TEMP_C = intPreferencesKey("weather_temp_c")
        val KEY_HIGH_C = intPreferencesKey("weather_high_c")
        val KEY_LOW_C = intPreferencesKey("weather_low_c")
        val KEY_HUMIDITY = intPreferencesKey("weather_humidity")
        val KEY_WIND_KMH = intPreferencesKey("weather_wind_kmh")
        val KEY_UV_INDEX = stringPreferencesKey("weather_uv_index")
        val KEY_WEATHER_CODE = intPreferencesKey("weather_code")
        val KEY_FORECAST = stringPreferencesKey("weather_forecast_hours")
        val KEY_LAST_UPDATED = longPreferencesKey("weather_last_updated")
    }
}

/**
 * Refreshes live weather data for the configured city when tapping the badge or city pill.
 */
class CycleWeatherAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WeatherRepository.fetchAndSyncWeather(context)
    }
}

class ToggleTemperatureUnitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WeatherRepository.toggleTemperatureUnit(context)
    }
}

fun formatTemp(celsius: Int, useFahrenheit: Boolean): String {
    val value = if (useFahrenheit) ((celsius * 9.0 / 5.0) + 32.0).roundToInt() else celsius
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
                        contentDescription = "Refresh ${weather.condition}",
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
                        contentDescription = "Refresh ${weather.condition}",
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
