package com.example.materialwidgets.ui.main

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.materialwidgets.R
import com.example.materialwidgets.data.DefaultDataRepository
import com.example.materialwidgets.ui.theme.MaterialWidgetsTheme
import com.example.materialwidgets.widget.calendar.CalendarCardAppWidgetReceiver
import com.example.materialwidgets.widget.calendar.CircleClockAppWidgetReceiver
import com.example.materialwidgets.widget.clock.ClockAppWidgetReceiver
import com.example.materialwidgets.widget.music.MediaSessionControllerHelper
import com.example.materialwidgets.widget.music.MusicAppWidgetReceiver
import com.example.materialwidgets.widget.music.MusicPillAppWidgetReceiver
import com.example.materialwidgets.widget.toggles.DataToggleAppWidgetReceiver
import com.example.materialwidgets.widget.toggles.FlashlightToggleAppWidgetReceiver
import com.example.materialwidgets.widget.toggles.RotateToggleAppWidgetReceiver
import com.example.materialwidgets.widget.toggles.WifiToggleAppWidgetReceiver
import com.example.materialwidgets.widget.weather.WeatherAppWidgetReceiver
import com.example.materialwidgets.widget.weather.WeatherRepository
import com.example.materialwidgets.widget.weather.formatTemp
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) },
) {
    MainScreen(data = emptyList(), modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(data: List<String>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isMediaAccessEnabled by remember {
        mutableStateOf(MediaSessionControllerHelper.isNotificationListenerEnabled(context))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isMediaAccessEnabled = MediaSessionControllerHelper.isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DynamicThemeBanner()
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.clock_widget_name),
                    subtitle = "Scalloped Cookie (2x2), Pill (2x1), Dual Card (4x2) • 12/24h Toggle",
                    icon = Icons.Rounded.AccessTime,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onPinClick = {
                        requestPinAppWidget(
                            context = context,
                            receiverClass = ClockAppWidgetReceiver::class.java,
                        )
                    },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.calendar_card_widget_name),
                    subtitle = "3-Day Strip Calendar Card (2x2) • Scalloped Today Badge, Month & Year",
                    icon = Icons.Rounded.CalendarMonth,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    onPinClick = {
                        requestPinAppWidget(
                            context = context,
                            receiverClass = CalendarCardAppWidgetReceiver::class.java,
                        )
                    },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.circle_clock_widget_name),
                    subtitle = "Circular Clock & Date (2x2) • Day-of-Week, Time Pill & Date Strip",
                    icon = Icons.Rounded.AccessTime,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinClick = {
                        requestPinAppWidget(
                            context = context,
                            receiverClass = CircleClockAppWidgetReceiver::class.java,
                        )
                    },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.weather_widget_name),
                    subtitle = "Live City Forecast (2x2 & 4x2) • No Location Permission • °C/°F Toggle",
                    icon = Icons.Rounded.Cloud,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinClick = {
                        requestPinAppWidget(
                            context = context,
                            receiverClass = WeatherAppWidgetReceiver::class.java,
                        )
                    },
                )
            }

            item {
                WeatherLocationConfigCard()
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.music_widget_name),
                    subtitle = "Live Spotify / YouTube Music MediaSession + Album Art (4x2 & 2x2)",
                    icon = Icons.Rounded.MusicNote,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onPinClick = {
                        requestPinAppWidget(
                            context = context,
                            receiverClass = MusicAppWidgetReceiver::class.java,
                        )
                    },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.music_pill_widget_name),
                    subtitle = "Compact 4x1 Stadium Bar • Circular Art, Nested Pill & Scalloped Play Button",
                    icon = Icons.Rounded.MusicNote,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    onPinClick = {
                        requestPinAppWidget(
                            context = context,
                            receiverClass = MusicPillAppWidgetReceiver::class.java,
                        )
                    },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.wifi_toggle_widget_name),
                    subtitle = "1x1 Scalloped Cookie Button • Opens Wi-Fi Quick Settings Panel",
                    icon = Icons.Rounded.Wifi,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onPinClick = {
                        requestPinAppWidget(
                            context = context,
                            receiverClass = WifiToggleAppWidgetReceiver::class.java,
                        )
                    },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.data_toggle_widget_name),
                    subtitle = "1x1 Circular Button • Opens Mobile Data / Internet Panel",
                    icon = Icons.Rounded.SwapVert,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinClick = {
                        requestPinAppWidget(
                            context = context,
                            receiverClass = DataToggleAppWidgetReceiver::class.java,
                        )
                    },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.flashlight_toggle_widget_name),
                    subtitle = "1x1 Circular Button • Toggles Camera Torch On/Off",
                    icon = Icons.Rounded.FlashlightOn,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onPinClick = {
                        requestPinAppWidget(
                            context = context,
                            receiverClass = FlashlightToggleAppWidgetReceiver::class.java,
                        )
                    },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.rotate_toggle_widget_name),
                    subtitle = "1x1 Scalloped Badge Button • Toggles Screen Auto-Rotation",
                    icon = Icons.Rounded.ScreenRotation,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onPinClick = {
                        requestPinAppWidget(
                            context = context,
                            receiverClass = RotateToggleAppWidgetReceiver::class.java,
                        )
                    },
                )
            }

            item {
                MediaSessionPermissionCard(
                    isEnabled = isMediaAccessEnabled,
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                )
            }
        }
    }
}

@Composable
private fun WeatherLocationConfigCard() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var cityInput by remember { mutableStateOf(WeatherRepository.getConfiguredCity(context)) }
    var useFahrenheit by remember { mutableStateOf(WeatherRepository.isFahrenheit(context)) }
    var weatherSnapshot by remember { mutableStateOf(WeatherRepository.getCachedWeather(context)) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                useFahrenheit = WeatherRepository.isFahrenheit(context)
                weatherSnapshot = WeatherRepository.getCachedWeather(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (WeatherRepository.getLastUpdatedMs(context) == 0L) {
            isLoading = true
            WeatherRepository.fetchAndSyncWeather(context)
                .onSuccess {
                    weatherSnapshot = it
                    statusMessage = "Live weather synced for ${it.city}"
                }
            isLoading = false
        }
    }

    fun submitCity(targetCity: String) {
        val cleaned = targetCity.trim()
        if (cleaned.isEmpty()) return
        cityInput = cleaned
        focusManager.clearFocus()
        scope.launch {
            isLoading = true
            statusMessage = null
            WeatherRepository.updateCityAndFetch(context, cleaned)
                .onSuccess { snapshot ->
                    weatherSnapshot = snapshot
                    cityInput = snapshot.city
                    statusMessage = "Updated live weather for ${snapshot.city}"
                }
                .onFailure { err ->
                    weatherSnapshot = WeatherRepository.getCachedWeather(context)
                    statusMessage = "Could not fetch '${cleaned}': ${err.localizedMessage ?: "Check connection"}"
                }
            isLoading = false
        }
    }

    val presetCities = listOf("Amsterdam", "San Francisco", "Tokyo", "London", "New York", "Berlin")
    val unitLabel = if (useFahrenheit) "F" else "C"

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationCity,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Weather Location & Units",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Configured city without GPS/Location permission",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Celsius / Fahrenheit segmented filter chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !useFahrenheit,
                        onClick = {
                            if (useFahrenheit) {
                                useFahrenheit = false
                                scope.launch { WeatherRepository.setTemperatureUnit(context, false) }
                            }
                        },
                        label = { Text("°C", fontWeight = FontWeight.Bold) },
                    )
                    FilterChip(
                        selected = useFahrenheit,
                        onClick = {
                            if (!useFahrenheit) {
                                useFahrenheit = true
                                scope.launch { WeatherRepository.setTemperatureUnit(context, true) }
                            }
                        },
                        label = { Text("°F", fontWeight = FontWeight.Bold) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live snapshot status pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${weatherSnapshot.city} • ${weatherSnapshot.condition}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = "H:${formatTemp(weatherSnapshot.highCelsius, useFahrenheit)}  " +
                                "L:${formatTemp(weatherSnapshot.lowCelsius, useFahrenheit)}  •  " +
                                "Humidity ${weatherSnapshot.humidityPercent}%  •  " +
                                "Wind ${weatherSnapshot.windKmh} km/h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        )
                    }
                    Text(
                        text = "${formatTemp(weatherSnapshot.tempCelsius, useFahrenheit)}$unitLabel",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    label = { Text("City name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submitCity(cityInput) }),
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                FilledTonalButton(
                    onClick = { submitCity(cityInput) },
                    enabled = !isLoading && cityInput.isNotBlank(),
                    modifier = Modifier.height(52.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Update")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick preset city chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presetCities.forEach { preset ->
                    AssistChip(
                        onClick = { submitCity(preset) },
                        label = { Text(preset) },
                    )
                }
            }

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusMessage!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun MediaSessionPermissionCard(
    isEnabled: Boolean,
    onOpenSettings: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnabled) {
                        "Live Media Session Connected"
                    } else {
                        "Connect Spotify & Media Players"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isEnabled) {
                        "Music widget reads live track info, album art, and controls from active media apps."
                    } else {
                        "Grant Notification Access so the Music widget can read live tracks and album art from Spotify or YouTube Music."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!isEnabled) {
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedButton(onClick = onOpenSettings) {
                    Text("Enable")
                }
            }
        }
    }
}

@Composable
private fun DynamicThemeBanner() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Material You Dynamic Color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "All widgets inherit your Android system wallpaper palette and automatically switch between Light and Dark themes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorSwatchPill("Primary", MaterialTheme.colorScheme.primary)
                ColorSwatchPill("Secondary", MaterialTheme.colorScheme.secondary)
                ColorSwatchPill("Tertiary", MaterialTheme.colorScheme.tertiary)
                ColorSwatchPill("Surface", MaterialTheme.colorScheme.primaryContainer)
            }
        }
    }
}

@Composable
private fun ColorSwatchPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun WidgetGalleryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onPinClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.85f),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            FilledTonalButton(onClick = onPinClick) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }
    }
}

private fun requestPinAppWidget(context: Context, receiverClass: Class<*>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, receiverClass)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(provider, null, null)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    MaterialWidgetsTheme { MainScreen(listOf("Android")) }
}
