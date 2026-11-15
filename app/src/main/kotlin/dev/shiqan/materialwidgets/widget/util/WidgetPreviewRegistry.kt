package dev.shiqan.materialwidgets.widget.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.calendar.CalendarCardAppWidget
import dev.shiqan.materialwidgets.widget.calendar.CalendarCardAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.calendar.CircleClockAppWidget
import dev.shiqan.materialwidgets.widget.calendar.CircleClockAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.clock.ClockAppWidget
import dev.shiqan.materialwidgets.widget.clock.ClockAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.music.MusicAppWidget
import dev.shiqan.materialwidgets.widget.music.MusicAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.music.MusicPillAppWidget
import dev.shiqan.materialwidgets.widget.music.MusicPillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.system.BatteryAppWidget
import dev.shiqan.materialwidgets.widget.system.BatteryAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.system.StorageRamAppWidget
import dev.shiqan.materialwidgets.widget.system.StorageRamAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.BluetoothToggleAppWidget
import dev.shiqan.materialwidgets.widget.toggles.BluetoothToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.DataToggleAppWidget
import dev.shiqan.materialwidgets.widget.toggles.DataToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.FlashlightToggleAppWidget
import dev.shiqan.materialwidgets.widget.toggles.FlashlightToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.RingerToggleAppWidget
import dev.shiqan.materialwidgets.widget.toggles.RingerToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.RotateToggleAppWidget
import dev.shiqan.materialwidgets.widget.toggles.RotateToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.WifiToggleAppWidget
import dev.shiqan.materialwidgets.widget.toggles.WifiToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.weather.WeatherAppWidget
import dev.shiqan.materialwidgets.widget.weather.WeatherAppWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

data class WidgetPreviewSpec(
    val receiverClass: Class<out GlanceAppWidgetReceiver>,
    val previewSize: DpSize,
    val createWidget: () -> GlanceAppWidget,
)

object WidgetPreviewRegistry {
    val specs: List<WidgetPreviewSpec> = listOf(
        WidgetPreviewSpec(ClockAppWidgetReceiver::class.java, DpSize(160.dp, 160.dp)) { ClockAppWidget() },
        WidgetPreviewSpec(CalendarCardAppWidgetReceiver::class.java, DpSize(160.dp, 160.dp)) { CalendarCardAppWidget() },
        WidgetPreviewSpec(CircleClockAppWidgetReceiver::class.java, DpSize(160.dp, 160.dp)) { CircleClockAppWidget() },
        WidgetPreviewSpec(WeatherAppWidgetReceiver::class.java, DpSize(160.dp, 160.dp)) { WeatherAppWidget() },
        WidgetPreviewSpec(MusicAppWidgetReceiver::class.java, DpSize(280.dp, 140.dp)) { MusicAppWidget() },
        WidgetPreviewSpec(MusicPillAppWidgetReceiver::class.java, DpSize(320.dp, 64.dp)) { MusicPillAppWidget() },
        WidgetPreviewSpec(BatteryAppWidgetReceiver::class.java, DpSize(160.dp, 160.dp)) { BatteryAppWidget() },
        WidgetPreviewSpec(StorageRamAppWidgetReceiver::class.java, DpSize(160.dp, 160.dp)) { StorageRamAppWidget() },
        WidgetPreviewSpec(WifiToggleAppWidgetReceiver::class.java, DpSize(76.dp, 76.dp)) { WifiToggleAppWidget() },
        WidgetPreviewSpec(DataToggleAppWidgetReceiver::class.java, DpSize(76.dp, 76.dp)) { DataToggleAppWidget() },
        WidgetPreviewSpec(FlashlightToggleAppWidgetReceiver::class.java, DpSize(76.dp, 76.dp)) { FlashlightToggleAppWidget() },
        WidgetPreviewSpec(RotateToggleAppWidgetReceiver::class.java, DpSize(76.dp, 76.dp)) { RotateToggleAppWidget() },
        WidgetPreviewSpec(BluetoothToggleAppWidgetReceiver::class.java, DpSize(76.dp, 76.dp)) { BluetoothToggleAppWidget() },
        WidgetPreviewSpec(RingerToggleAppWidgetReceiver::class.java, DpSize(76.dp, 76.dp)) { RingerToggleAppWidget() },
    )

    private val specsByReceiver: Map<Class<out GlanceAppWidgetReceiver>, WidgetPreviewSpec> =
        specs.associateBy { it.receiverClass }

    fun specFor(receiverClass: Class<out GlanceAppWidgetReceiver>): WidgetPreviewSpec? =
        specsByReceiver[receiverClass]
}

/**
 * Requests the system launcher to pin [receiverClass], supplying a live Glance-composed
 * [RemoteViews] preview in [AppWidgetManager.EXTRA_APPWIDGET_PREVIEW] so the confirmation
 * sheet renders the actual widget instead of a generic placeholder.
 */
suspend fun requestPinGlanceWidget(
    context: Context,
    receiverClass: Class<out GlanceAppWidgetReceiver>,
): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (!appWidgetManager.isRequestPinAppWidgetSupported) return false

    val spec = WidgetPreviewRegistry.specFor(receiverClass)
    if (spec != null) {
        val pinnedWithPreview = runCatching {
            val widget = spec.createWidget()
            val previewState = if (widget.stateDefinition != null) emptyPreferences() else null
            withContext(Dispatchers.Default) {
                GlanceAppWidgetManager(context).requestPinGlanceAppWidget(
                    receiver = receiverClass,
                    preview = widget,
                    previewSize = spec.previewSize,
                    previewState = previewState,
                )
            }
        }.getOrNull()
        if (pinnedWithPreview != null) return pinnedWithPreview
    }

    return appWidgetManager.requestPinAppWidget(ComponentName(context, receiverClass), null, null)
}

/**
 * Registers generated Glance widget previews with [GlanceAppWidgetManager] on Android 15+ (API 35+)
 * so the system widget picker shows live, widget-specific previews instead of placeholders.
 */
suspend fun publishAllWidgetPreviews(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
    withContext(Dispatchers.Default) {
        val manager = GlanceAppWidgetManager(context)
        for (spec in WidgetPreviewRegistry.specs) {
            runCatching {
                manager.setWidgetPreviews(spec.receiverClass.kotlin)
            }
        }
    }
}

/**
 * Refreshes the generated launcher picker preview for a single widget receiver on Android 15+.
 */
suspend fun refreshWidgetPreview(
    context: Context,
    receiverClass: KClass<out GlanceAppWidgetReceiver>,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
    withContext(Dispatchers.Default) {
        runCatching {
            GlanceAppWidgetManager(context).setWidgetPreviews(receiverClass)
        }
    }
}
