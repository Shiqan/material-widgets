package dev.shiqan.materialwidgets.widget.calendar

import android.appwidget.AppWidgetManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.PreviewSizeMode
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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.shiqan.materialwidgets.R
import dev.shiqan.materialwidgets.widget.theme.MaterialYouGlanceTheme
import dev.shiqan.materialwidgets.widget.util.showWidgetErrorFallback
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DatePillAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DatePillAppWidget()
}

/**
 * Compact 2x1 Stadium Date Pill Widget:
 * - Full-rounded stadium capsule (`secondaryContainer`)
 * - Left: 12-lobe scalloped day badge ("31")
 * - Right: Short month ("Jan") stacked above weekday ("Sunday")
 */
class DatePillAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(DpSize(190.dp, 76.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val isPreviewPin = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id) == AppWidgetManager.INVALID_APPWIDGET_ID
        }.getOrDefault(false)

        provideContent {
            MaterialYouGlanceTheme {
                DatePillWidgetContent(
                    packageName = if (isPreviewPin) null else context.packageName,
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            MaterialYouGlanceTheme {
                DatePillWidgetContent(packageName = null)
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        showWidgetErrorFallback(context, appWidgetId, "Date pill error")
    }
}

@Composable
fun DatePillWidgetContent(
    packageName: String? = null,
    today: LocalDate = LocalDate.now(),
) {
    val size = LocalSize.current
    val pillRadius = (size.height / 2).coerceAtLeast(32.dp)
    val badgeSize = (size.height - 16.dp).coerceIn(44.dp, 64.dp)

    val dayNum = today.format(DateTimeFormatter.ofPattern("dd", Locale.getDefault()))
    val monthText = today.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
    val weekdayText = today.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))

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
            .clickable(actionStartActivity(calendarIntent))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
                AndroidRemoteViews(remoteViews = RemoteViews(packageName, R.layout.widget_date_pill_day_num))
            } else {
                Text(
                    text = dayNum,
                    style = TextStyle(
                        color = GlanceTheme.colors.onTertiaryContainer,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (packageName != null) {
                AndroidRemoteViews(remoteViews = RemoteViews(packageName, R.layout.widget_date_pill_month))
                AndroidRemoteViews(remoteViews = RemoteViews(packageName, R.layout.widget_date_pill_weekday))
            } else {
                Text(
                    text = monthText,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = weekdayText,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 190, heightDp = 76)
@Composable
fun DatePillWidgetPreview() {
    MaterialYouGlanceTheme {
        DatePillWidgetContent()
    }
}
