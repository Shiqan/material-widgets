package dev.shiqan.materialwidgets.widget.toggles

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.PreviewSizeMode
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
import dev.shiqan.materialwidgets.R
import dev.shiqan.materialwidgets.widget.theme.MaterialYouGlanceTheme
import dev.shiqan.materialwidgets.widget.util.refreshWidgetPreview
import dev.shiqan.materialwidgets.widget.util.showWidgetErrorFallback
import kotlin.reflect.KClass

enum class QuickButtonShape {
    CIRCLE,
    SCALLOP_8,
    SCALLOP_12,
}

enum class QuickButtonStyle(val id: String, val label: String) {
    ROUND("round", "Round (Default)"),
    CURVY("curvy", "Curvy");

    fun toShape(): QuickButtonShape = when (this) {
        ROUND -> QuickButtonShape.CIRCLE
        CURVY -> QuickButtonShape.SCALLOP_8
    }

    companion object {
        val DEFAULT = ROUND

        fun fromId(id: String?): QuickButtonStyle =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

enum class QuickToggleType(val prefKey: String) {
    WIFI("style_wifi"),
    DATA("style_data"),
    FLASHLIGHT("style_flashlight"),
    ROTATE("style_rotate"),
    BLUETOOTH("style_bluetooth"),
    RINGER("style_ringer"),
    MUTE("style_mute"),
    VIBRATE("style_vibrate"),
    RING("style_ring"),
}

/**
 * Persists user-selected Quick Button shapes (Round default vs. Curvy scalloped)
 * and pushes updates to pinned Glance widgets and system previews.
 */
object QuickToggleStyleRepository {
    private const val PREFS_NAME = "quick_toggle_style_prefs"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStyle(context: Context, type: QuickToggleType): QuickButtonStyle =
        QuickButtonStyle.fromId(prefs(context).getString(type.prefKey, QuickButtonStyle.DEFAULT.id))

    fun getAllStyles(context: Context): Map<QuickToggleType, QuickButtonStyle> =
        QuickToggleType.entries.associateWith { getStyle(context, it) }

    suspend fun setStyle(context: Context, type: QuickToggleType, style: QuickButtonStyle) {
        prefs(context).edit().putString(type.prefKey, style.id).apply()
        pushStyleToWidget(context, type, style)
    }

    suspend fun setAllStyles(context: Context, style: QuickButtonStyle) {
        val editor = prefs(context).edit()
        for (type in QuickToggleType.entries) {
            editor.putString(type.prefKey, style.id)
        }
        editor.apply()
        for (type in QuickToggleType.entries) {
            pushStyleToWidget(context, type, style)
        }
    }

    private suspend fun pushStyleToWidget(
        context: Context,
        type: QuickToggleType,
        style: QuickButtonStyle,
    ) {
        val (widgetClass, receiverClass, createWidget) = widgetDescriptor(type)
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(widgetClass)
        for (glanceId in glanceIds) {
            updateAppWidgetState(context, glanceId) { glancePrefs ->
                glancePrefs[BaseQuickToggleAppWidget.KEY_BUTTON_STYLE] = style.id
            }
            createWidget().update(context, glanceId)
        }
        refreshWidgetPreview(context, receiverClass)
    }

    private fun widgetDescriptor(
        type: QuickToggleType,
    ): Triple<Class<out GlanceAppWidget>, KClass<out GlanceAppWidgetReceiver>, () -> GlanceAppWidget> =
        when (type) {
            QuickToggleType.WIFI -> Triple(
                WifiToggleAppWidget::class.java,
                WifiToggleAppWidgetReceiver::class,
                ::WifiToggleAppWidget,
            )
            QuickToggleType.DATA -> Triple(
                DataToggleAppWidget::class.java,
                DataToggleAppWidgetReceiver::class,
                ::DataToggleAppWidget,
            )
            QuickToggleType.FLASHLIGHT -> Triple(
                FlashlightToggleAppWidget::class.java,
                FlashlightToggleAppWidgetReceiver::class,
                ::FlashlightToggleAppWidget,
            )
            QuickToggleType.ROTATE -> Triple(
                RotateToggleAppWidget::class.java,
                RotateToggleAppWidgetReceiver::class,
                ::RotateToggleAppWidget,
            )
            QuickToggleType.BLUETOOTH -> Triple(
                BluetoothToggleAppWidget::class.java,
                BluetoothToggleAppWidgetReceiver::class,
                ::BluetoothToggleAppWidget,
            )
            QuickToggleType.RINGER -> Triple(
                RingerToggleAppWidget::class.java,
                RingerToggleAppWidgetReceiver::class,
                ::RingerToggleAppWidget,
            )
            QuickToggleType.MUTE -> Triple(
                MuteToggleAppWidget::class.java,
                MuteToggleAppWidgetReceiver::class,
                ::MuteToggleAppWidget,
            )
            QuickToggleType.VIBRATE -> Triple(
                VibrateToggleAppWidget::class.java,
                VibrateToggleAppWidgetReceiver::class,
                ::VibrateToggleAppWidget,
            )
            QuickToggleType.RING -> Triple(
                RingToggleAppWidget::class.java,
                RingToggleAppWidgetReceiver::class,
                ::RingToggleAppWidget,
            )
        }
}

abstract class BaseQuickToggleAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(DpSize(76.dp, 76.dp)))

    companion object {
        val KEY_BUTTON_STYLE = stringPreferencesKey("quick_toggle_button_style")
    }
}

// ---------------------------------------------------------------------------
// 1. Wi-Fi Quick Button Widget
// ---------------------------------------------------------------------------

class WifiToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WifiToggleAppWidget()
}

class WifiToggleAppWidget : BaseQuickToggleAppWidget() {
    private fun readSystemWifiEnabled(context: Context): Boolean = try {
        context.applicationContext.getSystemService(WifiManager::class.java)?.isWifiEnabled ?: true
    } catch (_: Exception) {
        true
    }

    private fun createClickAction(context: Context): Action =
        actionStartActivity(
            QuickSettingsTrampolineActivity.createIntent(
                context = context,
                target = QuickSettingsTrampolineActivity.TARGET_WIFI,
            ),
        )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val wifiPanelAction = createClickAction(context)
        val systemWifiEnabled = readSystemWifiEnabled(context)
        val fallbackStyle = QuickToggleStyleRepository.getStyle(context, QuickToggleType.WIFI)
        provideContent {
            val prefs = currentState<Preferences>()
            val isActive = prefs[KEY_WIFI_ACTIVE] ?: systemWifiEnabled
            val style = prefs[KEY_BUTTON_STYLE]?.let(QuickButtonStyle::fromId) ?: fallbackStyle
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_wifi,
                    contentDescription = "Wi-Fi Settings",
                    shape = style.toShape(),
                    isActive = isActive,
                    onClick = wifiPanelAction,
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val wifiPanelAction = createClickAction(context)
        val systemWifiEnabled = readSystemWifiEnabled(context)
        val style = QuickToggleStyleRepository.getStyle(context, QuickToggleType.WIFI)
        provideContent {
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_wifi,
                    contentDescription = "Wi-Fi Settings",
                    shape = style.toShape(),
                    isActive = systemWifiEnabled,
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
// 2. Mobile Data Quick Button Widget
// ---------------------------------------------------------------------------

class DataToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DataToggleAppWidget()
}

class DataToggleAppWidget : BaseQuickToggleAppWidget() {
    private fun readSystemMobileDataEnabled(context: Context): Boolean = try {
        Settings.Global.getInt(context.contentResolver, "mobile_data", 1) == 1
    } catch (_: Exception) {
        true
    }

    private fun createClickAction(context: Context): Action =
        actionStartActivity(
            QuickSettingsTrampolineActivity.createIntent(
                context = context,
                target = QuickSettingsTrampolineActivity.TARGET_MOBILE_DATA,
            ),
        )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dataPanelAction = createClickAction(context)
        val systemDataEnabled = readSystemMobileDataEnabled(context)
        val fallbackStyle = QuickToggleStyleRepository.getStyle(context, QuickToggleType.DATA)
        provideContent {
            val prefs = currentState<Preferences>()
            val isActive = prefs[KEY_DATA_ACTIVE] ?: systemDataEnabled
            val style = prefs[KEY_BUTTON_STYLE]?.let(QuickButtonStyle::fromId) ?: fallbackStyle
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_data,
                    contentDescription = "Mobile Data Settings",
                    shape = style.toShape(),
                    isActive = isActive,
                    onClick = dataPanelAction,
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val dataPanelAction = createClickAction(context)
        val systemDataEnabled = readSystemMobileDataEnabled(context)
        val style = QuickToggleStyleRepository.getStyle(context, QuickToggleType.DATA)
        provideContent {
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_data,
                    contentDescription = "Mobile Data Settings",
                    shape = style.toShape(),
                    isActive = systemDataEnabled,
                    onClick = dataPanelAction,
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
// 3. Flashlight / Torch Quick Button Widget
// ---------------------------------------------------------------------------

class FlashlightToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlashlightToggleAppWidget()
}

class FlashlightToggleAppWidget : BaseQuickToggleAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val fallbackStyle = QuickToggleStyleRepository.getStyle(context, QuickToggleType.FLASHLIGHT)
        provideContent {
            val prefs = currentState<Preferences>()
            val isTorchOn = prefs[KEY_FLASHLIGHT_ON] ?: false
            val style = prefs[KEY_BUTTON_STYLE]?.let(QuickButtonStyle::fromId) ?: fallbackStyle
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_flashlight,
                    contentDescription = if (isTorchOn) "Turn Flashlight Off" else "Turn Flashlight On",
                    shape = style.toShape(),
                    isActive = isTorchOn,
                    onClick = actionRunCallback<ToggleFlashlightAction>(),
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val style = QuickToggleStyleRepository.getStyle(context, QuickToggleType.FLASHLIGHT)
        provideContent {
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_flashlight,
                    contentDescription = "Turn Flashlight On",
                    shape = style.toShape(),
                    isActive = false,
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
// 4. Auto-Rotate Quick Button Widget
// ---------------------------------------------------------------------------

class RotateToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RotateToggleAppWidget()
}

class RotateToggleAppWidget : BaseQuickToggleAppWidget() {
    private fun readSystemAutoRotate(context: Context): Boolean = try {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            1,
        ) == 1
    } catch (_: Exception) {
        true
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val systemAutoRotate = readSystemAutoRotate(context)
        val fallbackStyle = QuickToggleStyleRepository.getStyle(context, QuickToggleType.ROTATE)
        provideContent {
            val prefs = currentState<Preferences>()
            val isRotateOn = prefs[KEY_ROTATE_ON] ?: systemAutoRotate
            val style = prefs[KEY_BUTTON_STYLE]?.let(QuickButtonStyle::fromId) ?: fallbackStyle
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_rotate,
                    contentDescription = "Toggle Auto-Rotate",
                    shape = style.toShape(),
                    isActive = isRotateOn,
                    onClick = actionRunCallback<ToggleAutoRotateAction>(),
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val systemAutoRotate = readSystemAutoRotate(context)
        val style = QuickToggleStyleRepository.getStyle(context, QuickToggleType.ROTATE)
        provideContent {
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_rotate,
                    contentDescription = "Toggle Auto-Rotate",
                    shape = style.toShape(),
                    isActive = systemAutoRotate,
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
// 5. Bluetooth Quick Button Widget
// ---------------------------------------------------------------------------

class BluetoothToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BluetoothToggleAppWidget()
}

class BluetoothToggleAppWidget : BaseQuickToggleAppWidget() {
    private fun readSystemBluetoothEnabled(context: Context): Boolean = try {
        context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled ?: true
    } catch (_: Exception) {
        true
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val systemBtEnabled = readSystemBluetoothEnabled(context)
        val fallbackStyle = QuickToggleStyleRepository.getStyle(context, QuickToggleType.BLUETOOTH)
        provideContent {
            val prefs = currentState<Preferences>()
            val isBtOn = prefs[KEY_BLUETOOTH_ON] ?: systemBtEnabled
            val style = prefs[KEY_BUTTON_STYLE]?.let(QuickButtonStyle::fromId) ?: fallbackStyle
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_bluetooth,
                    contentDescription = "Bluetooth Settings",
                    shape = style.toShape(),
                    isActive = isBtOn,
                    onClick = actionRunCallback<ToggleBluetoothAction>(),
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val systemBtEnabled = readSystemBluetoothEnabled(context)
        val style = QuickToggleStyleRepository.getStyle(context, QuickToggleType.BLUETOOTH)
        provideContent {
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = R.drawable.ic_toggle_bluetooth,
                    contentDescription = "Bluetooth Settings",
                    shape = style.toShape(),
                    isActive = systemBtEnabled,
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
// 6. Ringer / Sound Mode Cycle Widget
// ---------------------------------------------------------------------------

class RingerToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RingerToggleAppWidget()
}

class RingerToggleAppWidget : BaseQuickToggleAppWidget() {
    private fun readSystemRingerMode(context: Context): Int = try {
        context.getSystemService(AudioManager::class.java)?.ringerMode
            ?: AudioManager.RINGER_MODE_NORMAL
    } catch (_: Exception) {
        AudioManager.RINGER_MODE_NORMAL
    }

    private fun ringerVisuals(mode: Int): Triple<Int, String, Boolean> = when (mode) {
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

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val systemRingerMode = readSystemRingerMode(context)
        val fallbackStyle = QuickToggleStyleRepository.getStyle(context, QuickToggleType.RINGER)
        provideContent {
            val prefs = currentState<Preferences>()
            val mode = prefs[KEY_RINGER_MODE] ?: systemRingerMode
            val style = prefs[KEY_BUTTON_STYLE]?.let(QuickButtonStyle::fromId) ?: fallbackStyle
            val (iconRes, desc, isActive) = ringerVisuals(mode)
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = iconRes,
                    contentDescription = desc,
                    shape = style.toShape(),
                    isActive = isActive,
                    onClick = actionRunCallback<CycleRingerModeAction>(),
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val systemRingerMode = readSystemRingerMode(context)
        val style = QuickToggleStyleRepository.getStyle(context, QuickToggleType.RINGER)
        val (iconRes, desc, isActive) = ringerVisuals(systemRingerMode)
        provideContent {
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = iconRes,
                    contentDescription = desc,
                    shape = style.toShape(),
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

        val effectiveMode = applySystemRingerMode(
            context = context,
            targetMode = nextMode,
            openPolicySettingsIfBlocked = false,
        )
        val finalMode = if (nextMode == AudioManager.RINGER_MODE_SILENT && effectiveMode != AudioManager.RINGER_MODE_SILENT) {
            val fallback = if (currentSystemMode == AudioManager.RINGER_MODE_NORMAL) {
                AudioManager.RINGER_MODE_VIBRATE
            } else {
                AudioManager.RINGER_MODE_NORMAL
            }
            applySystemRingerMode(context = context, targetMode = fallback, openPolicySettingsIfBlocked = false)
        } else {
            effectiveMode
        }

        syncAllRingerWidgets(context, finalMode)
    }
}

// ---------------------------------------------------------------------------
// 7-9. Dedicated Mute / Vibrate / Ring Quick Button Widgets
// ---------------------------------------------------------------------------

class MuteToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MuteToggleAppWidget()
}

class MuteToggleAppWidget : BaseRingerModeButtonAppWidget(
    targetMode = AudioManager.RINGER_MODE_SILENT,
    toggleType = QuickToggleType.MUTE,
    iconRes = R.drawable.ic_toggle_ringer_silent,
    contentDescription = "Set Mute / Silent Mode",
    fallbackLabel = "Mute",
)

class VibrateToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VibrateToggleAppWidget()
}

class VibrateToggleAppWidget : BaseRingerModeButtonAppWidget(
    targetMode = AudioManager.RINGER_MODE_VIBRATE,
    toggleType = QuickToggleType.VIBRATE,
    iconRes = R.drawable.ic_toggle_ringer_vibrate,
    contentDescription = "Set Vibrate Mode",
    fallbackLabel = "Vibrate",
)

class RingToggleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RingToggleAppWidget()
}

class RingToggleAppWidget : BaseRingerModeButtonAppWidget(
    targetMode = AudioManager.RINGER_MODE_NORMAL,
    toggleType = QuickToggleType.RING,
    iconRes = R.drawable.ic_toggle_ringer_normal,
    contentDescription = "Set Ring / Sound Mode",
    fallbackLabel = "Ring",
)

abstract class BaseRingerModeButtonAppWidget(
    private val targetMode: Int,
    private val toggleType: QuickToggleType,
    @param:DrawableRes private val iconRes: Int,
    private val contentDescription: String,
    private val fallbackLabel: String,
) : BaseQuickToggleAppWidget() {

    private fun readSystemRingerMode(context: Context): Int = try {
        context.getSystemService(AudioManager::class.java)?.ringerMode
            ?: AudioManager.RINGER_MODE_NORMAL
    } catch (_: Exception) {
        AudioManager.RINGER_MODE_NORMAL
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val systemRingerMode = readSystemRingerMode(context)
        val fallbackStyle = QuickToggleStyleRepository.getStyle(context, toggleType)
        val clickAction = actionRunCallback<SetRingerModeAction>(
            actionParametersOf(SetRingerModeAction.KEY_TARGET_MODE to targetMode),
        )
        provideContent {
            val prefs = currentState<Preferences>()
            val mode = prefs[RingerToggleAppWidget.KEY_RINGER_MODE] ?: systemRingerMode
            val style = prefs[KEY_BUTTON_STYLE]?.let(QuickButtonStyle::fromId) ?: fallbackStyle
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = iconRes,
                    contentDescription = contentDescription,
                    shape = style.toShape(),
                    isActive = mode == targetMode,
                    onClick = clickAction,
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val systemRingerMode = readSystemRingerMode(context)
        val style = QuickToggleStyleRepository.getStyle(context, toggleType)
        val clickAction = actionRunCallback<SetRingerModeAction>(
            actionParametersOf(SetRingerModeAction.KEY_TARGET_MODE to targetMode),
        )
        provideContent {
            MaterialYouGlanceTheme {
                QuickToggleButtonContent(
                    iconRes = iconRes,
                    contentDescription = contentDescription,
                    shape = style.toShape(),
                    isActive = systemRingerMode == targetMode,
                    onClick = clickAction,
                )
            }
        }
    }

    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        showWidgetErrorFallback(context, appWidgetId, fallbackLabel)
    }
}

class SetRingerModeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val targetMode = parameters[KEY_TARGET_MODE] ?: AudioManager.RINGER_MODE_NORMAL
        val effectiveMode = applySystemRingerMode(
            context = context,
            targetMode = targetMode,
            openPolicySettingsIfBlocked = true,
        )
        syncAllRingerWidgets(context, effectiveMode)
    }

    companion object {
        val KEY_TARGET_MODE = ActionParameters.Key<Int>("target_ringer_mode")
    }
}

private fun applySystemRingerMode(
    context: Context,
    targetMode: Int,
    openPolicySettingsIfBlocked: Boolean,
): Int {
    val audioManager = context.getSystemService(AudioManager::class.java)
    if (targetMode == AudioManager.RINGER_MODE_SILENT) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (notificationManager != null && !notificationManager.isNotificationPolicyAccessGranted) {
            if (openPolicySettingsIfBlocked) {
                try {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }
            return audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        }
    }

    try {
        audioManager?.ringerMode = targetMode
    } catch (_: SecurityException) {
        if (openPolicySettingsIfBlocked) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    } catch (_: Exception) {
    }

    return try {
        audioManager?.ringerMode ?: targetMode
    } catch (_: Exception) {
        targetMode
    }
}

private suspend fun syncAllRingerWidgets(context: Context, mode: Int) {
    val manager = GlanceAppWidgetManager(context)
    val widgets: List<Pair<Class<out GlanceAppWidget>, () -> GlanceAppWidget>> = listOf(
        RingerToggleAppWidget::class.java to ::RingerToggleAppWidget,
        MuteToggleAppWidget::class.java to ::MuteToggleAppWidget,
        VibrateToggleAppWidget::class.java to ::VibrateToggleAppWidget,
        RingToggleAppWidget::class.java to ::RingToggleAppWidget,
    )
    for ((widgetClass, createWidget) in widgets) {
        for (id in manager.getGlanceIds(widgetClass)) {
            updateAppWidgetState(context, id) { prefs ->
                prefs[RingerToggleAppWidget.KEY_RINGER_MODE] = mode
            }
            createWidget().update(context, id)
        }
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
