package com.example.materialwidgets.widget.toggles

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
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
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.materialwidgets.R
import com.example.materialwidgets.widget.theme.MaterialYouGlanceTheme
import com.example.materialwidgets.widget.util.showWidgetErrorFallback

enum class QuickButtonShape {
    CIRCLE,
    SCALLOP_8,
    SCALLOP_12,
}

// ---------------------------------------------------------------------------
// 1. Wi-Fi Quick Button Widget (Scalloped Cookie Shape)
// ---------------------------------------------------------------------------

class WifiToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WifiToggleAppWidget()
}

class WifiToggleAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val wifiPanelAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            actionStartActivity(
                Intent(Settings.Panel.ACTION_WIFI).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
        } else {
            actionStartActivity(
                Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
        }

        provideContent {
            val isActive = currentState<Preferences>()[KEY_WIFI_ACTIVE] ?: true
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_wifi,
                    contentDescription = "Wi-Fi Settings",
                    shape = QuickButtonShape.SCALLOP_8,
                    isActive = isActive,
                    onClick = wifiPanelAction,
                )
            }
        }
    }

    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        showWidgetErrorFallback(context, appWidgetId, "Wi-Fi")
    }

    companion object {
        val KEY_WIFI_ACTIVE = booleanPreferencesKey("quick_toggle_wifi_active")
    }
}

// ---------------------------------------------------------------------------
// 2. Mobile Data Quick Button Widget (Circular Shape)
// ---------------------------------------------------------------------------

class DataToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DataToggleAppWidget()
}

class DataToggleAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val internetPanelAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            actionStartActivity(
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
        } else {
            actionStartActivity(
                Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
        }

        provideContent {
            val isActive = currentState<Preferences>()[KEY_DATA_ACTIVE] ?: false
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_data,
                    contentDescription = "Mobile Data Settings",
                    shape = QuickButtonShape.CIRCLE,
                    isActive = isActive,
                    onClick = internetPanelAction,
                )
            }
        }
    }

    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        showWidgetErrorFallback(context, appWidgetId, "Data")
    }

    companion object {
        val KEY_DATA_ACTIVE = booleanPreferencesKey("quick_toggle_data_active")
    }
}

// ---------------------------------------------------------------------------
// 3. Flashlight / Torch Quick Button Widget (Circular Shape)
// ---------------------------------------------------------------------------

class FlashlightToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlashlightToggleAppWidget()
}

class FlashlightToggleAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val isTorchOn = currentState<Preferences>()[KEY_FLASHLIGHT_ON] ?: false
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_flashlight,
                    contentDescription = if (isTorchOn) "Turn Flashlight Off" else "Turn Flashlight On",
                    shape = QuickButtonShape.CIRCLE,
                    isActive = isTorchOn,
                    onClick = actionRunCallback<ToggleFlashlightAction>(),
                )
            }
        }
    }

    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        showWidgetErrorFallback(context, appWidgetId, "Torch")
    }

    companion object {
        val KEY_FLASHLIGHT_ON = booleanPreferencesKey("quick_toggle_flashlight_on")
    }
}

class ToggleFlashlightAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        var nextState = false
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[FlashlightToggleAppWidget.KEY_FLASHLIGHT_ON] ?: false
            nextState = !current
            prefs[FlashlightToggleAppWidget.KEY_FLASHLIGHT_ON] = nextState
        }

        // Toggle real hardware camera torch if available on device
        try {
            val cameraManager = context.getSystemService(CameraManager::class.java)
            val torchCameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (torchCameraId != null) {
                cameraManager.setTorchMode(torchCameraId, nextState)
            }
        } catch (_: Exception) {
            // Emulator or device without flash unit; visual widget toggle still updates
        }

        FlashlightToggleAppWidget().update(context, glanceId)
    }
}

// ---------------------------------------------------------------------------
// 4. Auto-Rotate Quick Button Widget (12-Lobe Scalloped Badge Shape)
// ---------------------------------------------------------------------------

class RotateToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RotateToggleAppWidget()
}

class RotateToggleAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val systemAutoRotate = try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                1,
            ) == 1
        } catch (_: Exception) {
            true
        }

        provideContent {
            val prefs = currentState<Preferences>()
            val isRotateOn = prefs[KEY_ROTATE_ON] ?: systemAutoRotate
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_rotate,
                    contentDescription = "Toggle Auto-Rotate",
                    shape = QuickButtonShape.SCALLOP_12,
                    isActive = isRotateOn,
                    onClick = actionRunCallback<ToggleAutoRotateAction>(),
                )
            }
        }
    }

    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        showWidgetErrorFallback(context, appWidgetId, "Rotate")
    }

    companion object {
        val KEY_ROTATE_ON = booleanPreferencesKey("quick_toggle_rotate_on")
    }
}

class ToggleAutoRotateAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        var nextState = true
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[RotateToggleAppWidget.KEY_ROTATE_ON] ?: true
            nextState = !current
            prefs[RotateToggleAppWidget.KEY_ROTATE_ON] = nextState
        }

        if (Settings.System.canWrite(context)) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    if (nextState) 1 else 0,
                )
            } catch (_: Exception) {
            }
        } else {
            // Open Display settings if system write is not granted
            try {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }

        RotateToggleAppWidget().update(context, glanceId)
    }
}

// ---------------------------------------------------------------------------
// 5. Bluetooth Quick Button Widget (Scalloped Cookie Shape)
// ---------------------------------------------------------------------------

class BluetoothToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BluetoothToggleAppWidget()
}

class BluetoothToggleAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val systemBtEnabled = try {
            context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled ?: true
        } catch (_: Exception) {
            true
        }

        provideContent {
            val prefs = currentState<Preferences>()
            val isBtOn = prefs[KEY_BLUETOOTH_ON] ?: systemBtEnabled
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_bluetooth,
                    contentDescription = "Bluetooth Settings",
                    shape = QuickButtonShape.SCALLOP_8,
                    isActive = isBtOn,
                    onClick = actionRunCallback<ToggleBluetoothAction>(),
                )
            }
        }
    }

    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        showWidgetErrorFallback(context, appWidgetId, "Bluetooth")
    }

    companion object {
        val KEY_BLUETOOTH_ON = booleanPreferencesKey("quick_toggle_bluetooth_on")
    }
}

class ToggleBluetoothAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val systemBtEnabled = try {
            context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled
        } catch (_: Exception) {
            null
        }

        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[BluetoothToggleAppWidget.KEY_BLUETOOTH_ON] ?: (systemBtEnabled ?: true)
            prefs[BluetoothToggleAppWidget.KEY_BLUETOOTH_ON] = !current
        }

        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }

        BluetoothToggleAppWidget().update(context, glanceId)
    }
}

// ---------------------------------------------------------------------------
// 6. Ringer / Sound Mode Cycle Widget (12-Lobe Scalloped Badge Shape)
// ---------------------------------------------------------------------------

class RingerToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RingerToggleAppWidget()
}

class RingerToggleAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val systemRingerMode = try {
            context.getSystemService(AudioManager::class.java)?.ringerMode
                ?: AudioManager.RINGER_MODE_NORMAL
        } catch (_: Exception) {
            AudioManager.RINGER_MODE_NORMAL
        }

        provideContent {
            val prefs = currentState<Preferences>()
            val mode = prefs[KEY_RINGER_MODE] ?: systemRingerMode
            val (iconRes, desc, isActive) = when (mode) {
                AudioManager.RINGER_MODE_VIBRATE -> Triple(
                    R.drawable.ic_toggle_ringer_vibrate,
                    "Ringer: Vibrate",
                    true,
                )
                AudioManager.RINGER_MODE_SILENT -> Triple(
                    R.drawable.ic_toggle_ringer_silent,
                    "Ringer: Silent",
                    false,
                )
                else -> Triple(
                    R.drawable.ic_toggle_ringer_normal,
                    "Ringer: Sound On",
                    true,
                )
            }
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = iconRes,
                    contentDescription = desc,
                    shape = QuickButtonShape.SCALLOP_12,
                    isActive = isActive,
                    onClick = actionRunCallback<CycleRingerModeAction>(),
                )
            }
        }
    }

    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        showWidgetErrorFallback(context, appWidgetId, "Ringer")
    }

    companion object {
        val KEY_RINGER_MODE = intPreferencesKey("quick_toggle_ringer_mode")
    }
}

class CycleRingerModeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val audioManager = context.getSystemService(AudioManager::class.java)
        val currentSystemMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

        var nextMode = AudioManager.RINGER_MODE_VIBRATE
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[RingerToggleAppWidget.KEY_RINGER_MODE] ?: currentSystemMode
            nextMode = when (current) {
                AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            prefs[RingerToggleAppWidget.KEY_RINGER_MODE] = nextMode
        }

        try {
            if (nextMode == AudioManager.RINGER_MODE_SILENT) {
                // Switching to Silent may require DND policy access; fall back to Vibrate/Normal if restricted
                audioManager?.ringerMode = AudioManager.RINGER_MODE_SILENT
            } else {
                audioManager?.ringerMode = nextMode
            }
        } catch (_: SecurityException) {
            // If Do Not Disturb access is not granted for Silent mode, cycle between Normal and Vibrate
            val fallbackMode = if (currentSystemMode == AudioManager.RINGER_MODE_NORMAL) {
                AudioManager.RINGER_MODE_VIBRATE
            } else {
                AudioManager.RINGER_MODE_NORMAL
            }
            try {
                audioManager?.ringerMode = fallbackMode
            } catch (_: Exception) {
            }
        } catch (_: Exception) {
        }

        RingerToggleAppWidget().update(context, glanceId)
    }
}

// ---------------------------------------------------------------------------
// Shared Composable for 1x1 Expressive Material You Quick Toggle Buttons
// ---------------------------------------------------------------------------

@Composable
fun QuickToggleButtonContent(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    shape: QuickButtonShape,
    isActive: Boolean,
    onClick: Action,
) {
    val size = LocalSize.current
    val buttonSize = size.width.coerceAtMost(size.height).coerceIn(52.dp, 110.dp)
    val iconSize = (buttonSize / 2.6f).coerceIn(22.dp, 36.dp)

    val containerColor = if (isActive) {
        GlanceTheme.colors.tertiaryContainer
    } else {
        GlanceTheme.colors.secondaryContainer
    }
    val contentColor = if (isActive) {
        GlanceTheme.colors.onTertiaryContainer
    } else {
        GlanceTheme.colors.onSecondaryContainer
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.fillMaxSize(),
    ) {
        val shapeModifier = when (shape) {
            QuickButtonShape.CIRCLE -> {
                GlanceModifier
                    .size(buttonSize)
                    .appWidgetBackground()
                    .background(containerColor)
                    .cornerRadius(buttonSize / 2)
            }
            QuickButtonShape.SCALLOP_8 -> {
                GlanceModifier
                    .size(buttonSize)
                    .appWidgetBackground()
                    .background(
                        imageProvider = ImageProvider(R.drawable.bg_scalloped_cookie),
                        colorFilter = ColorFilter.tint(containerColor),
                    )
            }
            QuickButtonShape.SCALLOP_12 -> {
                GlanceModifier
                    .size(buttonSize)
                    .appWidgetBackground()
                    .background(
                        imageProvider = ImageProvider(R.drawable.bg_scalloped_badge_12),
                        colorFilter = ColorFilter.tint(containerColor),
                    )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = shapeModifier.clickable(onClick),
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = contentDescription,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = GlanceModifier.size(iconSize),
            )
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 76, heightDp = 76)
@Composable
fun QuickToggleButtonsPreview() {
    MaterialYouGlanceTheme {
        QuickToggleButtonContent(
            iconRes = R.drawable.ic_toggle_wifi,
            contentDescription = "Wi-Fi",
            shape = QuickButtonShape.SCALLOP_8,
            isActive = true,
            onClick = actionRunCallback<ToggleFlashlightAction>(),
        )
    }
}
