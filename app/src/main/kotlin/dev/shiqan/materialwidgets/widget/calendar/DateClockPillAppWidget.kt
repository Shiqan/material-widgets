package dev.shiqan.materialwidgets.widget.calendar

import android.appwidget.AppWidgetManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
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
import dev.shiqan.materialwidgets.R
import dev.shiqan.materialwidgets.widget.clock.ClockAppWidget
import dev.shiqan.materialwidgets.widget.theme.MaterialYouGlanceTheme
import dev.shiqan.materialwidgets.widget.util.showWidgetErrorFallback
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class DateClockPillAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DateClockPillAppWidget()
}

/**
 * 4x1 Wide Stadium Pill Date & Scalloped Clock Widget:
 * - Full-rounded stadium capsule (`secondaryContainer`)
 * - Left: Day + Day number ("Thursday 28") above Month + Year ("JAN 2026")
 * - Right: Dual 12-lobe scalloped badges for Hours ("02") and Minutes ("30")
 */
class DateClockPillAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(DpSize(320.dp, 76.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val isPreviewPin = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id) == AppWidgetManager.INVALID_APPWIDGET_ID
        }.getOrDefault(false)
        val default24Hour = DateFormat.is24HourFormat(context)

        provideContent {
            val prefs = currentState<Preferences>()
            val use24Hour = prefs[ClockAppWidget.KEY_USE_24_HOUR] ?: default24Hour
            MaterialYouGlanceTheme {
                DateClockPillWidgetContent(
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
                DateClockPillWidgetContent(
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
        showWidgetErrorFallback(context, appWidgetId, "Date & clock pill error")
    }
}

class ToggleDateClockPillFormatAction : ActionCallback {
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
        DateClockPillAppWidget().update(context, glanceId)
    }
}

@Composable
fun DateClockPillWidgetContent(
    use24Hour: Boolean = false,
    packageName: String? = null,
    now: LocalDateTime = LocalDateTime.now(),
) {
    val size = LocalSize.current
    val pillRadius = (size.height / 2).coerceAtLeast(32.dp)
    val badgeSize = (size.height - 16.dp).coerceIn(44.dp, 64.dp)

    val dayTitle = now.format(DateTimeFormatter.ofPattern("EEEE d", Locale.getDefault()))
    val monthYear = now.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())).uppercase()
    val hourPattern = if (use24Hour) "HH" else "hh"
    val hourText = now.format(DateTimeFormatter.ofPattern(hourPattern, Locale.getDefault()))
    val minuteText = now.format(DateTimeFormatter.ofPattern("mm", Locale.getDefault()))

    val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
        data = ContentUris.withAppendedId(
            CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build(),
            System.currentTimeMillis(),
        )
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(pillRadius)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Date Stack (tap opens Calendar)
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(calendarIntent)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (packageName != null) {
                AndroidRemoteViews(remoteViews = RemoteViews(packageName, R.layout.widget_date_clock_pill_day))
                AndroidRemoteViews(remoteViews = RemoteViews(packageName, R.layout.widget_date_clock_pill_month_year))
            } else {
                Text(
                    text = dayTitle,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = monthYear,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(10.dp))

        // Right: Two contrasting scalloped clock badges (tap toggles 12/24h format)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.clickable(actionRunCallback<ToggleDateClockPillFormatAction>()),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .size(badgeSize)
                    .background(
                        imageProvider = ImageProvider(R.drawable.bg_scalloped_badge_12),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer),
                    ),
            ) {
                if (packageName != null) {
                    val hourViews = RemoteViews(packageName, R.layout.widget_date_clock_pill_hour).apply {
                        setCharSequence(R.id.widget_text_clock_hour, "setFormat12Hour", hourPattern)
                        setCharSequence(R.id.widget_text_clock_hour, "setFormat24Hour", hourPattern)
                    }
                    AndroidRemoteViews(remoteViews = hourViews)
                } else {
                    Text(
                        text = hourText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .size(badgeSize)
                    .background(
                        imageProvider = ImageProvider(R.drawable.bg_scalloped_badge_12),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiaryContainer),
                    ),
            ) {
                if (packageName != null) {
                    AndroidRemoteViews(remoteViews = RemoteViews(packageName, R.layout.widget_date_clock_pill_minute))
                } else {
                    Text(
                        text = minuteText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onTertiaryContainer,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 76)
@Composable
fun DateClockPillWidgetPreview() {
    MaterialYouGlanceTheme {
        DateClockPillWidgetContent()
    }
}
