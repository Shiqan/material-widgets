package com.example.materialwidgets.widget.clock

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import com.example.materialwidgets.R
import com.example.materialwidgets.widget.theme.MaterialYouGlanceTheme
import com.example.materialwidgets.widget.util.showWidgetErrorFallback
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ClockAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClockAppWidget()
}

class ClockAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val use24Hour = prefs[KEY_USE_24_HOUR] ?: false
            MaterialYouGlanceTheme {
                ClockWidgetContent(use24Hour = use24Hour)
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        showWidgetErrorFallback(context, appWidgetId, "Clock widget error")
    }

    companion object {
        val KEY_USE_24_HOUR = booleanPreferencesKey("clock_use_24_hour")
    }
}

class ToggleClockFormatAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[ClockAppWidget.KEY_USE_24_HOUR] ?: false
            prefs[ClockAppWidget.KEY_USE_24_HOUR] = !current
        }
        ClockAppWidget().update(context, glanceId)
    }
}

@Composable
fun ClockWidgetContent(
    use24Hour: Boolean = false,
    now: LocalDateTime = LocalDateTime.now(),
) {
    val size = LocalSize.current
    val hourPattern = if (use24Hour) "HH" else "hh"
    val hourText = now.format(DateTimeFormatter.ofPattern(hourPattern, Locale.getDefault()))
    val minuteText = now.format(DateTimeFormatter.ofPattern("mm", Locale.getDefault()))
    val amPmText = now.format(DateTimeFormatter.ofPattern("a", Locale.getDefault())).uppercase()
    val dayOfWeekText = now.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
    val fullDateText = now.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    val zoneName = ZoneId.systemDefault().id.substringAfterLast('/').replace('_', ' ')

    val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    when {
        // Compact 2x1 pill layout
        size.height < 100.dp -> {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(28.dp)
                    .clickable(actionStartActivity(alarmIntent))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "$hourText:$minuteText",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                Column(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = fullDateText,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    if (!use24Hour) {
                        Text(
                            text = amPmText,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSecondaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                        )
                    }
                }
            }
        }

        // Wide 4x2 expressive dual-card layout (Scalloped clock + Date/Format card)
        size.width >= 240.dp -> {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .background(GlanceTheme.colors.widgetBackground)
                    .cornerRadius(28.dp)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val cookieSize = (size.height - 24.dp).coerceAtMost(156.dp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(cookieSize)
                        .background(
                            imageProvider = ImageProvider(R.drawable.bg_scalloped_cookie),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer),
                        )
                        .clickable(actionStartActivity(alarmIntent)),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = hourText,
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = minuteText,
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .background(GlanceTheme.colors.secondaryContainer)
                        .cornerRadius(22.dp)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = GlanceModifier
                                .size(32.dp)
                                .background(GlanceTheme.colors.tertiaryContainer)
                                .cornerRadius(16.dp)
                                .clickable(actionRunCallback<ToggleClockFormatAction>()),
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_clock_alarm),
                                contentDescription = "Toggle 12/24h format",
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                                modifier = GlanceModifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = if (use24Hour) "24H" else amPmText,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSecondaryContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = GlanceModifier.clickable(actionRunCallback<ToggleClockFormatAction>()),
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    Text(
                        text = fullDateText,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = zoneName,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }

        // Standard 2x2 Material You Scalloped Cookie Clock
        else -> {
            val cookieSize = size.width.coerceAtMost(size.height)
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier.fillMaxSize(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(cookieSize)
                        .appWidgetBackground()
                        .background(
                            imageProvider = ImageProvider(R.drawable.bg_scalloped_cookie),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer),
                        )
                        .clickable(actionRunCallback<ToggleClockFormatAction>()),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.tertiaryContainer)
                                .cornerRadius(12.dp)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = dayOfWeekText.uppercase(),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onTertiaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = hourText,
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = minuteText,
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 160)
@Preview(widthDp = 280, heightDp = 140)
@Preview(widthDp = 180, heightDp = 72)
@Composable
fun ClockWidgetPreview() {
    MaterialYouGlanceTheme {
        ClockWidgetContent(use24Hour = false)
    }
}
