package dev.shiqan.materialwidgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.PixelCopy
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.glance.appwidget.compose
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.shiqan.materialwidgets.widget.music.MusicAppWidget
import dev.shiqan.materialwidgets.widget.music.MusicAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.music.MusicPillAppWidgetReceiver
import dev.shiqan.materialwidgets.widget.util.WidgetPreviewRegistry
import dev.shiqan.materialwidgets.widget.util.publishAllWidgetPreviews
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class WidgetScreenshotCaptureTest {

    @Test
    fun captureAllWidgetScreenshots() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        val outputDir = File(instrumentation.targetContext.getExternalFilesDir(null), "widget_screenshots").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        scenario.onActivity { activity ->
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
            runBlocking {
                publishAllWidgetPreviews(activity)
            }
        }

        val appWidgetManager = AppWidgetManager.getInstance(instrumentation.targetContext)

        for (spec in WidgetPreviewRegistry.specs) {
            val fileSlug = spec.receiverClass.simpleName
                .removeSuffix("AppWidgetReceiver")
                .replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
                .lowercase()

            val renderSize: DpSize = when (spec.receiverClass) {
                MusicAppWidgetReceiver::class.java -> DpSize(360.dp, 140.dp)
                else -> spec.previewSize
            }

            val customPrefs = when (spec.receiverClass) {
                MusicPillAppWidgetReceiver::class.java,
                MusicAppWidgetReceiver::class.java -> mutablePreferencesOf(
                    MusicAppWidget.KEY_TRACK_INDEX to 0,
                    MusicAppWidget.KEY_IS_PLAYING to true,
                )
                else -> null
            }

            var remoteViews = if (
                customPrefs == null &&
                spec.receiverClass != MusicAppWidgetReceiver::class.java &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
            ) {
                runCatching {
                    appWidgetManager.getWidgetPreview(
                        ComponentName(instrumentation.targetContext, spec.receiverClass),
                        Process.myUserHandle(),
                        AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                    )
                }.getOrNull()
            } else {
                null
            }

            if (remoteViews == null) {
                remoteViews = runBlocking {
                    val widget = spec.createWidget()
                    val previewState = customPrefs
                        ?: if (widget.stateDefinition != null) emptyPreferences() else null
                    widget.compose(
                        context = instrumentation.targetContext,
                        size = renderSize,
                        state = previewState,
                    )
                }
            }

            lateinit var backdrop: FrameLayout
            lateinit var widgetContainer: FrameLayout
            var widthPx = 1
            var heightPx = 1

            scenario.onActivity { activity ->
                val dm = activity.resources.displayMetrics
                widthPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    renderSize.width.value,
                    dm,
                ).roundToInt().coerceAtLeast(1)
                heightPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    renderSize.height.value,
                    dm,
                ).roundToInt().coerceAtLeast(1)

                backdrop = FrameLayout(activity).apply {
                    setBackgroundColor(Color.WHITE)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
                widgetContainer = FrameLayout(activity).apply {
                    clipChildren = false
                    clipToPadding = false
                }
                val applied = remoteViews.apply(activity, widgetContainer)
                widgetContainer.addView(
                    applied,
                    FrameLayout.LayoutParams(widthPx, heightPx),
                )
                backdrop.addView(
                    widgetContainer,
                    FrameLayout.LayoutParams(widthPx, heightPx, Gravity.CENTER),
                )
                activity.setContentView(backdrop)
            }

            instrumentation.waitForIdleSync()
            SystemClock.sleep(150)

            val bmpWhite = captureRegionWithPixelCopy(scenario, widgetContainer, widthPx, heightPx)

            scenario.onActivity {
                backdrop.setBackgroundColor(Color.BLACK)
                backdrop.invalidate()
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(150)

            val bmpBlack = captureRegionWithPixelCopy(scenario, widgetContainer, widthPx, heightPx)
            val matted = differenceMatte(bmpWhite, bmpBlack)
            bmpWhite.recycle()
            bmpBlack.recycle()

            val outFile = File(outputDir, "$fileSlug.png")
            FileOutputStream(outFile).use { fos ->
                matted.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            matted.recycle()
        }

        scenario.close()
    }

    private fun captureRegionWithPixelCopy(
        scenario: ActivityScenario<ComponentActivity>,
        targetView: FrameLayout,
        widthPx: Int,
        heightPx: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        var copyResult = -1

        scenario.onActivity { activity ->
            val location = IntArray(2)
            targetView.getLocationInWindow(location)
            val srcRect = Rect(
                location[0],
                location[1],
                location[0] + widthPx,
                location[1] + heightPx,
            )
            PixelCopy.request(
                activity.window,
                srcRect,
                bitmap,
                { result ->
                    copyResult = result
                    latch.countDown()
                },
                Handler(Looper.getMainLooper()),
            )
        }

        check(latch.await(5, TimeUnit.SECONDS)) { "PixelCopy timed out" }
        check(copyResult == PixelCopy.SUCCESS) { "PixelCopy failed with code $copyResult" }
        return bitmap
    }

    private fun differenceMatte(bmpWhite: Bitmap, bmpBlack: Bitmap): Bitmap {
        val w = bmpWhite.width
        val h = bmpWhite.height
        val whitePixels = IntArray(w * h)
        val blackPixels = IntArray(w * h)
        val outPixels = IntArray(w * h)

        bmpWhite.getPixels(whitePixels, 0, w, 0, 0, w, h)
        bmpBlack.getPixels(blackPixels, 0, w, 0, 0, w, h)

        for (i in whitePixels.indices) {
            val wp = whitePixels[i]
            val bp = blackPixels[i]

            val wr = Color.red(wp)
            val wg = Color.green(wp)
            val wb = Color.blue(wp)

            val br = Color.red(bp)
            val bg = Color.green(bp)
            val bb = Color.blue(bp)

            val diffAvg = ((wr - br) + (wg - bg) + (wb - bb)) / 3.0
            val alpha = (255.0 - diffAvg).roundToInt().coerceIn(0, 255)

            outPixels[i] = if (alpha == 0) {
                Color.TRANSPARENT
            } else {
                val r = (br * 255.0 / alpha).roundToInt().coerceIn(0, 255)
                val g = (bg * 255.0 / alpha).roundToInt().coerceIn(0, 255)
                val b = (bb * 255.0 / alpha).roundToInt().coerceIn(0, 255)
                Color.argb(alpha, r, g, b)
            }
        }

        return Bitmap.createBitmap(outPixels, w, h, Bitmap.Config.ARGB_8888)
    }
}
