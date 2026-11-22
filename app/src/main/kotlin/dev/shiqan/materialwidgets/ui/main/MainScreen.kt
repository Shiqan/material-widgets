package dev.shiqan.materialwidgets.ui.main

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import dev.shiqan.materialwidgets.R
import dev.shiqan.materialwidgets.data.DefaultDataRepository
import dev.shiqan.materialwidgets.ui.theme.MaterialWidgetsTheme
import dev.shiqan.materialwidgets.widget.calendar.CalendarCardAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.calendar.CircleClockAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.calendar.DateClockPillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.calendar.DatePillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.clock.AnalogClockAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.clock.ClockAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.clock.ClockBatteryPillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.music.MediaSessionControllerHelper
import dev.shiqan.materialwidgets.widget.music.MusicAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.music.MusicPillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.system.BatteryAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.system.RamPillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.system.StoragePillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.system.StorageRamAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.BluetoothToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.DataToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.FlashlightToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.MuteToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.QuickButtonStyle
import dev.shiqan.materialwidgets.widget.toggles.QuickToggleStyleRepository
import dev.shiqan.materialwidgets.widget.toggles.QuickToggleType
import dev.shiqan.materialwidgets.widget.toggles.RingToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.RingerToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.RotateToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.VibrateToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.WifiToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.util.publishAllWidgetPreviews
import dev.shiqan.materialwidgets.widget.util.requestPinGlanceWidget
import dev.shiqan.materialwidgets.widget.weather.WeatherAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.weather.WeatherPillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.weather.WeatherRepository
import dev.shiqan.materialwidgets.widget.weather.WeatherTiltedPillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.weather.formatTemp
import kotlinx.coroutines.CoroutineScope
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
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isMediaAccessEnabled by remember {
        mutableStateOf(MediaSessionControllerHelper.isNotificationListenerEnabled(context))
    }
    var quickToggleStyles by remember {
        mutableStateOf(QuickToggleStyleRepository.getAllStyles(context))
    }

    LaunchedEffect(context) {
        publishAllWidgetPreviews(context)
    }

    val pinWidget: (Class<out GlanceAppWidgetReceiver>) -> Unit = { receiverClass ->
        requestPinAppWidget(scope = scope, context = context, receiverClass = receiverClass)
    }

    val setAllQuickStyles: (QuickButtonStyle) -> Unit = { style ->
        quickToggleStyles = QuickToggleType.entries.associateWith { style }
        scope.launch {
            QuickToggleStyleRepository.setAllStyles(context, style)
        }
    }

    val toggleQuickStyle: (QuickToggleType) -> Unit = { type ->
        val current = quickToggleStyles[type] ?: QuickButtonStyle.DEFAULT
        val next = if (current == QuickButtonStyle.ROUND) QuickButtonStyle.CURVY else QuickButtonStyle.ROUND
        quickToggleStyles = quickToggleStyles + (type to next)
        scope.launch {
            QuickToggleStyleRepository.setStyle(context, type, next)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isMediaAccessEnabled = MediaSessionControllerHelper.isNotificationListenerEnabled(context)
                quickToggleStyles = QuickToggleStyleRepository.getAllStyles(context)
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
                    onPinClick = { pinWidget(ClockAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.clock_battery_pill_widget_name),
                    subtitle = "Stadium Pill (3x2) • Date Header, Bold Digital Clock & Live Battery %",
                    icon = Icons.Rounded.AccessTime,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onPinClick = { pinWidget(ClockBatteryPillAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.analog_clock_widget_name),
                    subtitle = "12-Lobe Scalloped Analog Clock (2x2) • Curved Date & Rounded Hands",
                    icon = Icons.Rounded.Schedule,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onPinClick = { pinWidget(AnalogClockAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.calendar_card_widget_name),
                    subtitle = "3-Day Strip Calendar Card (2x2) • Scalloped Today Badge, Month & Year",
                    icon = Icons.Rounded.CalendarMonth,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinClick = { pinWidget(CalendarCardAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.date_clock_pill_widget_name),
                    subtitle = "Stadium Bar (4x1) • Full Date, Month/Year & Dual Scalloped Hour/Min Badges",
                    icon = Icons.Rounded.CalendarMonth,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinClick = { pinWidget(DateClockPillAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.date_pill_widget_name),
                    subtitle = "Compact Stadium Pill (2x1) • Scalloped Day Badge, Month & Weekday",
                    icon = Icons.Rounded.CalendarMonth,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinClick = { pinWidget(DatePillAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.circle_clock_widget_name),
                    subtitle = "Transparent Stacked Clock & Date Pills (2x2) • Day, Time & Date",
                    icon = Icons.Rounded.AccessTime,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinClick = { pinWidget(CircleClockAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.weather_widget_name),
                    subtitle = "Live City Forecast (2x2 & 4x2) • No Location Permission • °C/°F Toggle",
                    icon = Icons.Rounded.Cloud,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinClick = { pinWidget(WeatherAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.weather_pill_widget_name),
                    subtitle = "Stadium Pill (4x2) • Scalloped Sun Badge, Date, Temp & H/L Range",
                    icon = Icons.Rounded.WbSunny,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinClick = { pinWidget(WeatherPillAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.weather_tilted_pill_widget_name),
                    subtitle = "45° Tilted Pill (2x2) • Pixel-Style Weather Icon & Bold Temperature",
                    icon = Icons.Rounded.WbSunny,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinClick = { pinWidget(WeatherTiltedPillAppWidgetReceiver::class.java) },
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
                    onPinClick = { pinWidget(MusicAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.music_pill_widget_name),
                    subtitle = "Compact 4x1 Stadium Bar • Full-Height Controls & Oversized Curvy Play Badge",
                    icon = Icons.Rounded.MusicNote,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onPinClick = { pinWidget(MusicPillAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.battery_widget_name),
                    subtitle = "Live Battery & Charging Gauge (2x1 Pill, 2x2 Cookie, 4x2 Dual Card) • Temp & Source",
                    icon = Icons.Rounded.BatteryChargingFull,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onPinClick = { pinWidget(BatteryAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.storage_ram_widget_name),
                    subtitle = "Internal Storage & RAM Gauge (2x2 Dual Pill & 4x2 Segmented Cards) • Live Refresh",
                    icon = Icons.Rounded.Memory,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinClick = { pinWidget(StorageRamAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.storage_pill_widget_name),
                    subtitle = "2x1 Rounded Storage Card • Icon & Flush-Right Capacity Pill",
                    icon = Icons.Rounded.Storage,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinClick = { pinWidget(StoragePillAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.ram_pill_widget_name),
                    subtitle = "2x1 Rounded RAM Card • Flush-Left Usage Pill & Chip Icon",
                    icon = Icons.Rounded.Memory,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onPinClick = { pinWidget(RamPillAppWidgetReceiver::class.java) },
                )
            }

            item {
                QuickButtonsStyleConfigCard(
                    styles = quickToggleStyles,
                    onSelectAll = setAllQuickStyles,
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.wifi_toggle_widget_name),
                    subtitle = "1x1 Quick Button • Opens Wi-Fi Quick Settings Panel",
                    icon = Icons.Rounded.Wifi,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    selectedStyle = quickToggleStyles[QuickToggleType.WIFI] ?: QuickButtonStyle.DEFAULT,
                    onToggleStyle = { toggleQuickStyle(QuickToggleType.WIFI) },
                    onPinClick = { pinWidget(WifiToggleAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.data_toggle_widget_name),
                    subtitle = "1x1 Quick Button • Opens Mobile Data / Internet Settings",
                    icon = Icons.Rounded.SwapVert,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedStyle = quickToggleStyles[QuickToggleType.DATA] ?: QuickButtonStyle.DEFAULT,
                    onToggleStyle = { toggleQuickStyle(QuickToggleType.DATA) },
                    onPinClick = { pinWidget(DataToggleAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.flashlight_toggle_widget_name),
                    subtitle = "1x1 Quick Button • Toggles Camera Torch On/Off",
                    icon = Icons.Rounded.FlashlightOn,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedStyle = quickToggleStyles[QuickToggleType.FLASHLIGHT] ?: QuickButtonStyle.DEFAULT,
                    onToggleStyle = { toggleQuickStyle(QuickToggleType.FLASHLIGHT) },
                    onPinClick = { pinWidget(FlashlightToggleAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.rotate_toggle_widget_name),
                    subtitle = "1x1 Quick Button • Toggles Screen Auto-Rotation",
                    icon = Icons.Rounded.ScreenRotation,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    selectedStyle = quickToggleStyles[QuickToggleType.ROTATE] ?: QuickButtonStyle.DEFAULT,
                    onToggleStyle = { toggleQuickStyle(QuickToggleType.ROTATE) },
                    onPinClick = { pinWidget(RotateToggleAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.bluetooth_toggle_widget_name),
                    subtitle = "1x1 Quick Button • Opens Bluetooth Settings & Pairing",
                    icon = Icons.Rounded.Bluetooth,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedStyle = quickToggleStyles[QuickToggleType.BLUETOOTH] ?: QuickButtonStyle.DEFAULT,
                    onToggleStyle = { toggleQuickStyle(QuickToggleType.BLUETOOTH) },
                    onPinClick = { pinWidget(BluetoothToggleAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.ringer_toggle_widget_name),
                    subtitle = "1x1 Quick Button • Cycles Sound, Vibrate & Silent Modes",
                    icon = Icons.Rounded.NotificationsActive,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    selectedStyle = quickToggleStyles[QuickToggleType.RINGER] ?: QuickButtonStyle.DEFAULT,
                    onToggleStyle = { toggleQuickStyle(QuickToggleType.RINGER) },
                    onPinClick = { pinWidget(RingerToggleAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.mute_toggle_widget_name),
                    subtitle = "1x1 Quick Button • Switches Ringer to Silent / Mute Mode",
                    icon = Icons.Rounded.NotificationsOff,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedStyle = quickToggleStyles[QuickToggleType.MUTE] ?: QuickButtonStyle.DEFAULT,
                    onToggleStyle = { toggleQuickStyle(QuickToggleType.MUTE) },
                    onPinClick = { pinWidget(MuteToggleAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.vibrate_toggle_widget_name),
                    subtitle = "1x1 Quick Button • Switches Ringer to Vibrate Mode",
                    icon = Icons.Rounded.Vibration,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedStyle = quickToggleStyles[QuickToggleType.VIBRATE] ?: QuickButtonStyle.DEFAULT,
                    onToggleStyle = { toggleQuickStyle(QuickToggleType.VIBRATE) },
                    onPinClick = { pinWidget(VibrateToggleAppWidgetReceiver::class.java) },
                )
            }

            item {
                WidgetGalleryCard(
                    title = stringResource(R.string.ring_toggle_widget_name),
                    subtitle = "1x1 Quick Button • Switches Ringer to Normal / Sound Mode",
                    icon = Icons.Rounded.NotificationsActive,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    selectedStyle = quickToggleStyles[QuickToggleType.RING] ?: QuickButtonStyle.DEFAULT,
                    onToggleStyle = { toggleQuickStyle(QuickToggleType.RING) },
                    onPinClick = { pinWidget(RingToggleAppWidgetReceiver::class.java) },
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
private fun QuickButtonsStyleConfigCard(
    styles: Map<QuickToggleType, QuickButtonStyle>,
    onSelectAll: (QuickButtonStyle) -> Unit,
) {
    val allRound = styles.values.all { it == QuickButtonStyle.ROUND }
    val allCurvy = styles.values.all { it == QuickButtonStyle.CURVY }

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
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quick Buttons Style",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Switch all 1x1 quick buttons or customize each button below",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = allRound,
                    onClick = { onSelectAll(QuickButtonStyle.ROUND) },
                    label = { Text(QuickButtonStyle.ROUND.label, fontWeight = FontWeight.Bold) },
                )
                FilterChip(
                    selected = allCurvy,
                    onClick = { onSelectAll(QuickButtonStyle.CURVY) },
                    label = { Text(QuickButtonStyle.CURVY.label, fontWeight = FontWeight.Bold) },
                )
            }
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
    selectedStyle: QuickButtonStyle? = null,
    onToggleStyle: (() -> Unit)? = null,
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
                if (selectedStyle != null && onToggleStyle != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AssistChip(
                        onClick = onToggleStyle,
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = contentColor,
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = contentColor.copy(alpha = 0.4f),
                        ),
                        label = {
                            Text(
                                text = "Style: ${selectedStyle.label}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                    )
                }
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

private fun requestPinAppWidget(
    scope: CoroutineScope,
    context: Context,
    receiverClass: Class<out GlanceAppWidgetReceiver>,
) {
    scope.launch {
        requestPinGlanceWidget(context = context, receiverClass = receiverClass)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    MaterialWidgetsTheme { MainScreen(listOf("Android")) }
}
