package com.example.materialwidgets.widget.calendar

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.materialwidgets.widget.clock.ClockAppWidget
import com.example.materialwidgets.widget.clock.ToggleClockFormatAction
import com.example.materialwidgets.widget.theme.MaterialYouGlanceTheme
import com.example.materialwidgets.widget.util.showWidgetErrorFallback
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class CircleClockAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CircleClockAppWidget()
}

/**
 * Circular Clock & Date Widget matching the reference showcase:
 * - Outer circular frame (`inverseSurface`)
 * - Top pill: Day of week ("Thursday")
 * - Center bold pill: Current Time ("02 30")
 * - Bottom pill: Day + Month ("31 January")
 */
class CircleClockAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val use24Hour = prefs[ClockAppWidget.KEY_USE_24_HOUR] ?: false
            MaterialYouGlanceTheme {
                CircleClockWidgetContent(use24Hour = use24Hour)
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        showWidgetErrorFallback(context, appWidgetId, "Circle clock widget error")
    }
}

@Composable
fun CircleClockWidgetContent(
    use24Hour: Boolean = false,
    now: LocalDateTime = LocalDateTime.now(),
) {
    val size = LocalSize.current
    val diameter = size.width.coerceAtMost(size.height).coerceAtLeast(110.dp)
    val hourPattern = if (use24Hour) "HH" else "hh"
    val hourText = now.format(DateTimeFormatter.ofPattern(hourPattern, Locale.getDefault()))
    val minuteText = now.format(DateTimeFormatter.ofPattern("mm", Locale.getDefault()))
    val dayOfWeek = now.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
    val dateMonth = now.format(DateTimeFormatter.ofPattern("dd MMMM", Locale.getDefault()))

    val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.fillMaxSize(),
    ) {
        Column(
            modifier = GlanceModifier
                .size(diameter)
                .appWidgetBackground()
                .background(GlanceTheme.colors.inverseSurface)
                .cornerRadius(diameter / 2)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Top Day-of-Week Pill ("Thursday")
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(14.dp)
                    .clickable(actionStartActivity(alarmIntent)),
            ) {
                Text(
                    text = dayOfWeek,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Center Expressive Time Pill ("02 30")
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(24.dp)
                    .clickable(actionRunCallback<ToggleClockFormatAction>()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = hourText,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = minuteText,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Bottom Date Pill ("31 January")
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(14.dp)
                    .clickable(actionStartActivity(alarmIntent)),
            ) {
                Text(
                    text = dateMonth,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 160)
@Composable
fun CircleClockWidgetPreview() {
    MaterialYouGlanceTheme {
        CircleClockWidgetContent()
    }
}
