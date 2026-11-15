package dev.shiqan.materialwidgets.widget.clock

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
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
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import dev.shiqan.materialwidgets.R
import dev.shiqan.materialwidgets.widget.system.BatteryStatusReader
import dev.shiqan.materialwidgets.widget.theme.MaterialYouGlanceTheme
import dev.shiqan.materialwidgets.widget.util.showWidgetErrorFallback
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ClockBatteryPillAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClockBatteryPillAppWidget()
}

/**
 * Stadium Pill Clock & Battery Widget:
 * - Full-rounded stadium capsule (`secondaryContainer`)
 * - Top: Date ("Thu, Jan 30")
 * - Center: Large digital time ("2:30") powered by live TextClock
 * - Bottom: Live battery percentage ("75%")
 */
class ClockBatteryPillAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(DpSize(240.dp, 140.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val isPreviewPin = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id) == AppWidgetManager.INVALID_APPWIDGET_ID
        }.getOrDefault(false)
        val default24Hour = DateFormat.is24HourFormat(context)

        provideContent {
            val prefs = currentState<Preferences>()
            val use24Hour = prefs[ClockAppWidget.KEY_USE_24_HOUR] ?: default24Hour
            @Suppress("UNUSED_VARIABLE")
            val revision = prefs[KEY_REVISION] ?: 0
            val batteryPercent = BatteryStatusReader.read(context).levelPercent

            MaterialYouGlanceTheme {
                ClockBatteryPillWidgetContent(
                    use24Hour = use24Hour,
                    batteryPercent = batteryPercent,
                    packageName = if (isPreviewPin) null else context.packageName,
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val default24Hour = DateFormat.is24HourFormat(context)
        val batteryPercent = BatteryStatusReader.read(context).levelPercent
        provideContent {
            MaterialYouGlanceTheme {
                ClockBatteryPillWidgetContent(
                    use24Hour = default24Hour,
                    batteryPercent = batteryPercent,
                    packageName = null,
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
        showWidgetErrorFallback(context, appWidgetId, "Clock pill widget error")
    }

    companion object {
        val KEY_REVISION = intPreferencesKey("clock_battery_pill_revision")
    }
}

class RefreshClockBatteryPillAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val default24Hour = DateFormat.is24HourFormat(context)
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[ClockAppWidget.KEY_USE_24_HOUR] ?: default24Hour
            prefs[ClockAppWidget.KEY_USE_24_HOUR] = !current
            prefs[ClockBatteryPillAppWidget.KEY_REVISION] =
                (prefs[ClockBatteryPillAppWidget.KEY_REVISION] ?: 0) + 1
        }
        ClockBatteryPillAppWidget().update(context, glanceId)
    }
}

@Composable
fun ClockBatteryPillWidgetContent(
    use24Hour: Boolean = false,
    batteryPercent: Int = 75,
    packageName: String? = null,
    now: LocalDateTime = LocalDateTime.now(),
) {
    val size = LocalSize.current
    val pillRadius = (size.height / 2).coerceAtLeast(36.dp)
    val dateText = now.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    val timePattern = if (use24Hour) "HH:mm" else "h:mm"
    val timeText = now.format(DateTimeFormatter.ofPattern(timePattern, Locale.getDefault()))

    val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(pillRadius)
            .clickable(actionRunCallback<RefreshClockBatteryPillAction>())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (packageName != null) {
            val dateViews = RemoteViews(packageName, R.layout.widget_clock_battery_pill_date)
            AndroidRemoteViews(
                remoteViews = dateViews,
                modifier = GlanceModifier.clickable(actionStartActivity(alarmIntent)),
            )

            Spacer(modifier = GlanceModifier.height(2.dp))

            val timeViews = RemoteViews(packageName, R.layout.widget_clock_battery_pill_time).apply {
                setCharSequence(R.id.widget_text_clock, "setFormat12Hour", timePattern)
                setCharSequence(R.id.widget_text_clock, "setFormat24Hour", timePattern)
            }
            AndroidRemoteViews(remoteViews = timeViews)
        } else {
            Text(
                text = dateText,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                modifier = GlanceModifier.clickable(actionStartActivity(alarmIntent)),
            )

            Spacer(modifier = GlanceModifier.height(2.dp))

            Text(
                text = timeText,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }

        Spacer(modifier = GlanceModifier.height(2.dp))

        Text(
            text = "$batteryPercent%",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 240, heightDp = 140)
@Composable
fun ClockBatteryPillWidgetPreview() {
    MaterialYouGlanceTheme {
        ClockBatteryPillWidgetContent(
            use24Hour = false,
            batteryPercent = 75,
        )
    }
}
