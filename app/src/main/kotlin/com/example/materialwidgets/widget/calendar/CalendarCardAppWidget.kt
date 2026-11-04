package com.example.materialwidgets.widget.calendar

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.materialwidgets.R
import com.example.materialwidgets.widget.theme.MaterialYouGlanceTheme
import com.example.materialwidgets.widget.util.showWidgetErrorFallback
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarCardAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarCardAppWidget()
}

/**
 * 2x2 Material You Date Card Widget matching the reference showcase:
 * - Rounded card background (`inverseSurface`)
 * - Year ("2026") and Month ("November") stacked at top center
 * - 3-day horizontal strip at bottom: Yesterday | Scalloped Today Badge | Tomorrow
 */
class CalendarCardAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            MaterialYouGlanceTheme {
                CalendarCardWidgetContent()
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        showWidgetErrorFallback(context, appWidgetId, "Calendar widget error")
    }
}

@Composable
fun CalendarCardWidgetContent(today: LocalDate = LocalDate.now()) {
    val yesterday = today.minusDays(1)
    val tomorrow = today.plusDays(1)

    val yearText = today.format(DateTimeFormatter.ofPattern("yyyy", Locale.getDefault()))
    val monthText = today.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))

    val dayNameFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val dayNumFormatter = DateTimeFormatter.ofPattern("dd", Locale.getDefault())

    val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
        data = ContentUris.withAppendedId(
            CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build(),
            System.currentTimeMillis(),
        )
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.inverseSurface)
            .cornerRadius(28.dp)
            .clickable(actionStartActivity(calendarIntent))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Year & Month Header
        Text(
            text = yearText,
            style = TextStyle(
                color = GlanceTheme.colors.inverseOnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
        )
        Text(
            text = monthText,
            style = TextStyle(
                color = GlanceTheme.colors.inversePrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )

        Spacer(modifier = GlanceModifier.height(10.dp))

        // 3-Day Strip: Yesterday | Today (Scalloped Badge) | Tomorrow
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Yesterday
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = yesterday.format(dayNameFormatter),
                    style = TextStyle(
                        color = GlanceTheme.colors.inverseOnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                )
                Text(
                    text = yesterday.format(dayNumFormatter),
                    style = TextStyle(
                        color = GlanceTheme.colors.inverseOnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            // Today inside 12-Lobe Scalloped Badge
            val size = LocalSize.current
            val badgeSize = (size.width / 2.6f).coerceIn(52.dp, 68.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .size(badgeSize)
                    .background(
                        imageProvider = ImageProvider(R.drawable.bg_scalloped_badge_12),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiaryContainer),
                    ),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = today.format(dayNameFormatter),
                        style = TextStyle(
                            color = GlanceTheme.colors.onTertiaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Text(
                        text = today.format(dayNumFormatter),
                        style = TextStyle(
                            color = GlanceTheme.colors.onTertiaryContainer,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            // Tomorrow
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = tomorrow.format(dayNameFormatter),
                    style = TextStyle(
                        color = GlanceTheme.colors.inverseOnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                )
                Text(
                    text = tomorrow.format(dayNumFormatter),
                    style = TextStyle(
                        color = GlanceTheme.colors.inverseOnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 160)
@Composable
fun CalendarCardWidgetPreview() {
    MaterialYouGlanceTheme {
        CalendarCardWidgetContent()
    }
}
