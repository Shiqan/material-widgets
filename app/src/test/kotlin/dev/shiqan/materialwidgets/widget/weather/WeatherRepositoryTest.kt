package dev.shiqan.materialwidgets.widget.weather

import dev.shiqan.materialwidgets.R
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherRepositoryTest {

    @Test
    fun iconForWeatherCodeMapsAllConditionGroups() {
        assertEquals(R.drawable.ic_weather_sunny, WeatherRepository.iconForWeatherCode(0))
        assertEquals(R.drawable.ic_weather_sunny, WeatherRepository.iconForWeatherCode(1))
        assertEquals(R.drawable.ic_weather_partly_cloudy, WeatherRepository.iconForWeatherCode(2))
        assertEquals(R.drawable.ic_weather_cloudy, WeatherRepository.iconForWeatherCode(3))
        assertEquals(R.drawable.ic_weather_foggy, WeatherRepository.iconForWeatherCode(45))
        assertEquals(R.drawable.ic_weather_foggy, WeatherRepository.iconForWeatherCode(48))
        assertEquals(R.drawable.ic_weather_rainy, WeatherRepository.iconForWeatherCode(51))
        assertEquals(R.drawable.ic_weather_rainy, WeatherRepository.iconForWeatherCode(63))
        assertEquals(R.drawable.ic_weather_rainy, WeatherRepository.iconForWeatherCode(80))
        assertEquals(R.drawable.ic_weather_snowy, WeatherRepository.iconForWeatherCode(71))
        assertEquals(R.drawable.ic_weather_snowy, WeatherRepository.iconForWeatherCode(85))
        assertEquals(R.drawable.ic_weather_thunderstorm, WeatherRepository.iconForWeatherCode(95))
        assertEquals(R.drawable.ic_weather_thunderstorm, WeatherRepository.iconForWeatherCode(99))
    }

    @Test
    fun serializeAndDeserializeForecastPreservesWeatherCodes() {
        val hours = listOf(
            HourlyForecast("12 PM", 18, 0),
            HourlyForecast("3 PM", 19, 2),
            HourlyForecast("6 PM", 15, 61),
            HourlyForecast("9 PM", 12, 95),
        )
        val encoded = WeatherRepository.serializeForecast(hours)
        val decoded = WeatherRepository.deserializeForecast(encoded)

        assertEquals(hours, decoded)
        assertEquals(R.drawable.ic_weather_sunny, decoded[0].iconRes)
        assertEquals(R.drawable.ic_weather_partly_cloudy, decoded[1].iconRes)
        assertEquals(R.drawable.ic_weather_rainy, decoded[2].iconRes)
        assertEquals(R.drawable.ic_weather_thunderstorm, decoded[3].iconRes)
    }

    @Test
    fun deserializeForecastSupportsLegacyTwoPartFormat() {
        val legacy = "12 PM=17|3 PM=18|6 PM=15|9 PM=13"
        val decoded = WeatherRepository.deserializeForecast(legacy, fallbackWeatherCode = 61)

        assertEquals(4, decoded.size)
        assertEquals(HourlyForecast("12 PM", 17, 61), decoded[0])
        assertEquals(R.drawable.ic_weather_rainy, decoded[0].iconRes)
    }

    @Test
    fun sampleWeatherStatesProvideFourHourlyForecastItems() {
        for (sample in SAMPLE_WEATHER_STATES) {
            assertEquals(4, sample.forecastHours.size)
        }
    }
}
