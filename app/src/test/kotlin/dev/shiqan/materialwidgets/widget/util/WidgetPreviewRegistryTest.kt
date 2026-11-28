package dev.shiqan.materialwidgets.widget.util

import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import dev.shiqan.materialwidgets.widget.calendar.CalendarCardAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.calendar.CircleClockAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.calendar.DateClockPillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.calendar.DatePillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.clock.AnalogClockAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.clock.ClockAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.clock.ClockBatteryPillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.clock.VerticalClockPillAppWidgetReceiver
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
import dev.shiqan.materialwidgets.widget.toggles.RingToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.RingerToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.RotateToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.VibrateToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.toggles.WifiToggleAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.weather.WeatherAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.weather.WeatherPillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.weather.WeatherTiltedPillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.weather.WeatherTiltedPillReverseAppWidgetReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPreviewRegistryTest {

    private val expectedReceivers: Set<Class<out GlanceAppWidgetReceiver>> = setOf(
        ClockAppWidgetReceiver::class.java,
        ClockBatteryPillAppWidgetReceiver::class.java,
        VerticalClockPillAppWidgetReceiver::class.java,
        AnalogClockAppWidgetReceiver::class.java,
        CalendarCardAppWidgetReceiver::class.java,
        DateClockPillAppWidgetReceiver::class.java,
        DatePillAppWidgetReceiver::class.java,
        CircleClockAppWidgetReceiver::class.java,
        WeatherAppWidgetReceiver::class.java,
        WeatherPillAppWidgetReceiver::class.java,
        WeatherTiltedPillAppWidgetReceiver::class.java,
        WeatherTiltedPillReverseAppWidgetReceiver::class.java,
        MusicAppWidgetReceiver::class.java,
        MusicPillAppWidgetReceiver::class.java,
        BatteryAppWidgetReceiver::class.java,
        StorageRamAppWidgetReceiver::class.java,
        StoragePillAppWidgetReceiver::class.java,
        RamPillAppWidgetReceiver::class.java,
        WifiToggleAppWidgetReceiver::class.java,
        DataToggleAppWidgetReceiver::class.java,
        FlashlightToggleAppWidgetReceiver::class.java,
        RotateToggleAppWidgetReceiver::class.java,
        BluetoothToggleAppWidgetReceiver::class.java,
        RingerToggleAppWidgetReceiver::class.java,
        MuteToggleAppWidgetReceiver::class.java,
        VibrateToggleAppWidgetReceiver::class.java,
        RingToggleAppWidgetReceiver::class.java,
    )

    @Test
    fun registryCoversAllWidgetsWithoutDuplicates() {
        val registered = WidgetPreviewRegistry.specs.map { it.receiverClass }
        assertEquals(expectedReceivers.size, registered.size)
        assertEquals(expectedReceivers, registered.toSet())
    }

    @Test
    fun specLookupReturnsMatchingSpecForEveryRegisteredReceiver() {
        for (spec in WidgetPreviewRegistry.specs) {
            assertEquals(spec, WidgetPreviewRegistry.specFor(spec.receiverClass))
            assertTrue(spec.previewSize.width > 0.dp)
            assertTrue(spec.previewSize.height > 0.dp)
        }
        assertNull(WidgetPreviewRegistry.specFor(GlanceAppWidgetReceiver::class.java))
    }

    @Test
    fun everyWidgetConfiguresResponsivePreviewSizeModeMatchingItsRegistrySize() {
        for (spec in WidgetPreviewRegistry.specs) {
            val widget = spec.createWidget()
            assertNotNull(widget)
            val previewMode = widget.previewSizeMode as? SizeMode.Responsive
            assertNotNull(
                "Expected SizeMode.Responsive previewSizeMode for ${spec.receiverClass.simpleName}",
                previewMode,
            )
            assertTrue(
                "Expected previewSize ${spec.previewSize} in ${spec.receiverClass.simpleName}.previewSizeMode",
                previewMode!!.sizes.contains(spec.previewSize),
            )
        }
    }
}
