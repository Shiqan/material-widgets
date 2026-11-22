package dev.shiqan.materialwidgets.widget.clock

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.provider.AlarmClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import dev.shiqan.materialwidgets.R
import dev.shiqan.materialwidgets.widget.theme.MaterialYouGlanceTheme
import dev.shiqan.materialwidgets.widget.util.calculateWidgetMemoryCapBytes
import dev.shiqan.materialwidgets.widget.util.showWidgetErrorFallback
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class AnalogClockAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AnalogClockAppWidget()
}

/**
 * 2x2 Scalloped Analog Clock Widget:
 * - 12-lobe scalloped cookie face (`secondaryContainer`)
 * - Curved top date text ("Thu 31") & 6-o'clock accent dot
 * - Live rotating rounded-cap Material You hour & minute hands via AnalogClock
 */
class AnalogClockAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(DpSize(160.dp, 160.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val isPreviewPin = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id) == AppWidgetManager.INVALID_APPWIDGET_ID
        }.getOrDefault(false)

        val dateLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d", Locale.getDefault()))
        val textColor = ContextCompat.getColor(context, R.color.widget_preview_on_secondary_container)
        val dotColor = ContextCompat.getColor(context, R.color.widget_preview_primary)
        val hourColor = ContextCompat.getColor(context, R.color.widget_preview_on_secondary_container)
        val minuteColor = ContextCompat.getColor(context, R.color.widget_preview_primary)

        // Decode/render the shared dial overlay bitmap once before provideContent so SizeMode.Exact
        // deduplicates the bitmap across orientations and stays well within the widget memory cap.
        val overlayBitmap = createAnalogDialOverlayBitmap(
            context = context,
            dateLabel = dateLabel,
            textColor = textColor,
            dotColor = dotColor,
            staticHandsTime = if (isPreviewPin) LocalTime.now() else null,
            hourHandColor = hourColor,
            minuteHandColor = minuteColor,
        )

        provideContent {
            MaterialYouGlanceTheme {
                AnalogClockWidgetContent(
                    dateLabel = dateLabel,
                    overlayBitmap = overlayBitmap,
                    packageName = if (isPreviewPin) null else context.packageName,
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val dateLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d", Locale.getDefault()))
        val textColor = ContextCompat.getColor(context, R.color.widget_preview_on_secondary_container)
        val dotColor = ContextCompat.getColor(context, R.color.widget_preview_primary)
        val hourColor = ContextCompat.getColor(context, R.color.widget_preview_on_secondary_container)
        val minuteColor = ContextCompat.getColor(context, R.color.widget_preview_primary)

        val previewBitmap = createAnalogDialOverlayBitmap(
            context = context,
            dateLabel = dateLabel,
            textColor = textColor,
            dotColor = dotColor,
            staticHandsTime = LocalTime.of(10, 10),
            hourHandColor = hourColor,
            minuteHandColor = minuteColor,
        )

        provideContent {
            MaterialYouGlanceTheme {
                AnalogClockWidgetContent(
                    dateLabel = dateLabel,
                    overlayBitmap = previewBitmap,
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
        showWidgetErrorFallback(context, appWidgetId, "Analog clock error")
    }
}

private fun createAnalogDialOverlayBitmap(
    context: Context,
    dateLabel: String,
    textColor: Int,
    dotColor: Int,
    staticHandsTime: LocalTime? = null,
    hourHandColor: Int = textColor,
    minuteHandColor: Int = dotColor,
): Bitmap {
    val density = context.resources.displayMetrics.density
    val desiredPx = (160f * density).toInt().coerceIn(160, 400)
    val maxBytes = calculateWidgetMemoryCapBytes(context) / 2
    val maxSideFromCap = sqrt((maxBytes / 4L).toDouble()).toInt().coerceAtLeast(120)
    val sizePx = min(desiredPx, maxSideFromCap)

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // Curved date text ("Thu 31") along the top inner arc of the scalloped face
    val arcRadius = sizePx * 0.34f
    val arcRect = RectF(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius)
    val arcPath = Path().apply {
        addArc(arcRect, 220f, 100f)
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = sizePx * 0.088f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawTextOnPath(dateLabel, arcPath, 0f, 0f, textPaint)

    // Bottom 6-o'clock dot
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dotColor
        alpha = 200
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, sizePx * 0.84f, sizePx * 0.036f, dotPaint)

    // Optional static hands for previews / pin sheets
    if (staticHandsTime != null) {
        val minuteAngleRad = Math.toRadians((staticHandsTime.minute * 6.0) - 90.0)
        val hourAngleRad = Math.toRadians(((staticHandsTime.hour % 12) * 30.0 + staticHandsTime.minute * 0.5) - 90.0)

        val minutePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = minuteHandColor
            alpha = 225
            strokeWidth = sizePx * 0.085f
            strokeCap = Paint.Cap.ROUND
        }
        val minuteLength = sizePx * 0.27f
        canvas.drawLine(
            cx,
            cy,
            cx + (cos(minuteAngleRad) * minuteLength).toFloat(),
            cy + (sin(minuteAngleRad) * minuteLength).toFloat(),
            minutePaint,
        )

        val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = hourHandColor
            strokeWidth = sizePx * 0.095f
            strokeCap = Paint.Cap.ROUND
        }
        val hourLength = sizePx * 0.20f
        canvas.drawLine(
            cx,
            cy,
            cx + (cos(hourAngleRad) * hourLength).toFloat(),
            cy + (sin(hourAngleRad) * hourLength).toFloat(),
            hourPaint,
        )
    }

    return bitmap
}

@Composable
fun AnalogClockWidgetContent(
    dateLabel: String,
    overlayBitmap: Bitmap? = null,
    packageName: String? = null,
) {
    val size = LocalSize.current
    val cookieSize = size.width.coerceAtMost(size.height)
    val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

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
                    imageProvider = ImageProvider(R.drawable.bg_scalloped_badge_12),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.secondaryContainer),
                )
                .clickable(actionStartActivity(alarmIntent)),
        ) {
            if (overlayBitmap != null) {
                Image(
                    provider = ImageProvider(overlayBitmap),
                    contentDescription = dateLabel,
                    modifier = GlanceModifier.fillMaxSize(),
                )
            }

            if (packageName != null) {
                AndroidRemoteViews(
                    remoteViews = RemoteViews(packageName, R.layout.widget_analog_clock_hands),
                    modifier = GlanceModifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 160)
@Composable
fun AnalogClockWidgetPreview() {
    MaterialYouGlanceTheme {
        AnalogClockWidgetContent(dateLabel = "Thu 31")
    }
}
