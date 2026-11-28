package dev.shiqan.materialwidgets.widget.clock

import android.appwidget.AppWidgetManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import dev.shiqan.materialwidgets.R
import dev.shiqan.materialwidgets.widget.theme.MaterialYouGlanceTheme
import dev.shiqan.materialwidgets.widget.util.showWidgetErrorFallback
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class VerticalClockPillAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VerticalClockPillAppWidget()
}

/**
 * 2x2 Vertical Rounded Pill Clock Widget (inspired by the Tilted Weather Pill):
 * - Full-height vertical rounded rectangle (`secondaryContainer`)
 * - Top: Oversized digital time breaking past the pill edges (tap toggles 12/24h format)
 * - Bottom: Compact stacked weekday & date inside the pill (tap opens Calendar)
 */
class VerticalClockPillAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(DpSize(160.dp, 160.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val isPreviewPin = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id) == AppWidgetManager.INVALID_APPWIDGET_ID
        }.getOrDefault(false)
        val default24Hour = DateFormat.is24HourFormat(context)

        provideContent {
            val prefs = currentState<Preferences>()
            val use24Hour = prefs[ClockAppWidget.KEY_USE_24_HOUR] ?: default24Hour
            MaterialYouGlanceTheme {
                VerticalClockPillWidgetContent(
                    use24Hour = use24Hour,
                    packageName = if (isPreviewPin) null else context.packageName,
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val default24Hour = DateFormat.is24HourFormat(context)
        provideContent {
            MaterialYouGlanceTheme {
                VerticalClockPillWidgetContent(
                    use24Hour = default24Hour,
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
        showWidgetErrorFallback(context, appWidgetId, "Vertical clock pill error")
    }
}

class ToggleVerticalClockPillFormatAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val default24Hour = DateFormat.is24HourFormat(context)
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[ClockAppWidget.KEY_USE_24_HOUR] ?: default24Hour
            prefs[ClockAppWidget.KEY_USE_24_HOUR] = !current
        }
        VerticalClockPillAppWidget().update(context, glanceId)
    }
}

@Composable
fun VerticalClockPillWidgetContent(
    use24Hour: Boolean = false,
    packageName: String? = null,
    now: LocalDateTime = LocalDateTime.now(),
) {
    val size = LocalSize.current
    val containerSize = size.width.coerceAtMost(size.height).coerceAtLeast(110.dp)
    val pillWidth = (containerSize * 0.56f).coerceIn(68.dp, 140.dp)
    val pillCornerRadius = (pillWidth * 0.34f).coerceIn(22.dp, 42.dp)
    val topPadding = (containerSize * 0.15f).coerceIn(14.dp, 32.dp)
    val bottomPadding = (containerSize * 0.11f).coerceIn(10.dp, 24.dp)

    val timeSp = (containerSize.value * 0.29f).coerceIn(32f, 64f)
    val weekdaySp = (containerSize.value * 0.082f).coerceIn(11f, 18f)
    val dateSp = (containerSize.value * 0.095f).coerceIn(13f, 21f)

    val timePattern = if (use24Hour) "HH:mm" else "h:mm"
    val timeText = now.format(DateTimeFormatter.ofPattern(timePattern, Locale.getDefault()))
    val weekdayText = now.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
    val dateText = now.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))

    val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
        data = ContentUris.withAppendedId(
            CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build(),
            System.currentTimeMillis(),
        )
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.fillMaxSize(),
    ) {
        // Layer 1: Full-height vertical rounded pill with date at the bottom
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = GlanceModifier
                .width(pillWidth)
                .fillMaxHeight()
                .appWidgetBackground()
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(pillCornerRadius)
                .clickable(actionStartActivity(calendarIntent))
                .padding(horizontal = 8.dp, vertical = bottomPadding),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (packageName != null) {
                    val weekdayViews = RemoteViews(packageName, R.layout.widget_vertical_clock_pill_weekday).apply {
                        setTextViewTextSize(R.id.widget_text_clock_weekday, TypedValue.COMPLEX_UNIT_SP, weekdaySp)
                    }
                    val dateViews = RemoteViews(packageName, R.layout.widget_vertical_clock_pill_date).apply {
                        setTextViewTextSize(R.id.widget_text_clock_date, TypedValue.COMPLEX_UNIT_SP, dateSp)
                    }
                    AndroidRemoteViews(remoteViews = weekdayViews)
                    AndroidRemoteViews(remoteViews = dateViews)
                } else {
                    Text(
                        text = weekdayText,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = weekdaySp.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = dateText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = dateSp.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }

        // Layer 2: Oversized time at the top breaking past the pill edges
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(top = topPadding),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionRunCallback<ToggleVerticalClockPillFormatAction>()),
            ) {
                if (packageName != null) {
                    val timeViews = RemoteViews(packageName, R.layout.widget_vertical_clock_pill_time).apply {
                        setCharSequence(R.id.widget_text_clock_time, "setFormat12Hour", timePattern)
                        setCharSequence(R.id.widget_text_clock_time, "setFormat24Hour", timePattern)
                        setTextViewTextSize(R.id.widget_text_clock_time, TypedValue.COMPLEX_UNIT_SP, timeSp)
                    }
                    AndroidRemoteViews(remoteViews = timeViews)
                } else {
                    Text(
                        text = timeText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = timeSp.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 160)
@Composable
fun VerticalClockPillWidgetPreview() {
    MaterialYouGlanceTheme {
        VerticalClockPillWidgetContent(use24Hour = false)
    }
}
