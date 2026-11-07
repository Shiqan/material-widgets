package com.example.materialwidgets.widget.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
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
import java.util.Locale

data class BatterySnapshot(
    val levelPercent: Int,
    val isCharging: Boolean,
    val sourceLabel: String,
    val temperatureCelsius: Float,
    val isPowerSave: Boolean,
)

object BatteryStatusReader {
    fun read(context: Context): BatterySnapshot {
        val batteryStatus: Intent? = try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {
            null
        }

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale.toFloat()).toInt().coerceIn(0, 100)
        } else {
            val bm = context.getSystemService(BatteryManager::class.java)
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 85
        }

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val sourceLabel = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            BatteryManager.BATTERY_PLUGGED_DOCK -> "Dock"
            else -> if (isCharging) "Charging" else "On Battery"
        }

        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val tempC = (tempTenths / 10f).coerceIn(0f, 99f)

        val powerManager = context.getSystemService(PowerManager::class.java)
        val isPowerSave = powerManager?.isPowerSaveMode == true

        return BatterySnapshot(
            levelPercent = percent,
            isCharging = isCharging,
            sourceLabel = sourceLabel,
            temperatureCelsius = tempC,
            isPowerSave = isPowerSave,
        )
    }
}

class BatteryAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteryAppWidget()
}

class BatteryAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Observe revision so manual refresh triggers live recomposition
            val revision = currentState<Preferences>()[KEY_REVISION] ?: 0
            val snapshot = BatteryStatusReader.read(context)
            MaterialYouGlanceTheme {
                BatteryWidgetContent(
                    snapshot = snapshot,
                    revision = revision,
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
        showWidgetErrorFallback(context, appWidgetId, "Battery widget error")
    }

    companion object {
        val KEY_REVISION = intPreferencesKey("battery_widget_revision")
    }
}

class RefreshBatteryAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[BatteryAppWidget.KEY_REVISION] = (prefs[BatteryAppWidget.KEY_REVISION] ?: 0) + 1
        }
        BatteryAppWidget().update(context, glanceId)
    }
}

@Composable
fun BatteryWidgetContent(
    snapshot: BatterySnapshot,
    @Suppress("UNUSED_PARAMETER") revision: Int = 0,
) {
    val size = LocalSize.current
    val batteryIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val iconRes = if (snapshot.isCharging) {
        R.drawable.ic_battery_charging
    } else {
        R.drawable.ic_battery_full
    }

    val statusText = when {
        snapshot.isCharging && snapshot.levelPercent >= 100 -> "Fully Charged"
        snapshot.isCharging -> "Charging • ${snapshot.sourceLabel}"
        snapshot.isPowerSave -> "Battery Saver"
        snapshot.levelPercent <= 20 -> "Low Battery"
        else -> snapshot.sourceLabel
    }

    val tempText = String.format(Locale.getDefault(), "%.1f°C", snapshot.temperatureCelsius)

    when {
        // Compact 2x1 Stadium Pill
        size.height < 100.dp -> {
            val pillContainer = if (snapshot.isCharging) {
                GlanceTheme.colors.tertiaryContainer
            } else {
                GlanceTheme.colors.secondaryContainer
            }
            val pillContent = if (snapshot.isCharging) {
                GlanceTheme.colors.onTertiaryContainer
            } else {
                GlanceTheme.colors.onSecondaryContainer
            }

            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .background(pillContainer)
                    .cornerRadius(28.dp)
                    .clickable(actionRunCallback<RefreshBatteryAction>())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(38.dp)
                        .background(
                            imageProvider = ImageProvider(R.drawable.bg_scalloped_badge_12),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer),
                        ),
                ) {
                    Image(
                        provider = ImageProvider(iconRes),
                        contentDescription = statusText,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
                        modifier = GlanceModifier.size(20.dp),
                    )
                }

                Spacer(modifier = GlanceModifier.width(10.dp))

                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${snapshot.levelPercent}%",
                        style = TextStyle(
                            color = pillContent,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = statusText,
                        style = TextStyle(
                            color = pillContent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }

        // Wide 4x2 Expressive Dual-Card Layout
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
                val badgeContainer = if (snapshot.isCharging) {
                    GlanceTheme.colors.tertiaryContainer
                } else {
                    GlanceTheme.colors.primaryContainer
                }
                val badgeContent = if (snapshot.isCharging) {
                    GlanceTheme.colors.onTertiaryContainer
                } else {
                    GlanceTheme.colors.onPrimaryContainer
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(cookieSize)
                        .background(
                            imageProvider = ImageProvider(R.drawable.bg_scalloped_cookie),
                            colorFilter = ColorFilter.tint(badgeContainer),
                        )
                        .clickable(actionStartActivity(batteryIntent)),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            provider = ImageProvider(iconRes),
                            contentDescription = statusText,
                            colorFilter = ColorFilter.tint(badgeContent),
                            modifier = GlanceModifier.size(26.dp),
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "${snapshot.levelPercent}%",
                            style = TextStyle(
                                color = badgeContent,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = if (snapshot.isCharging) "CHARGING" else "BATTERY",
                            style = TextStyle(
                                color = badgeContent,
                                fontSize = 10.sp,
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
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = statusText,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSecondaryContainer,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                maxLines = 1,
                            )
                            Text(
                                text = "Temp $tempText" + if (snapshot.isPowerSave) " • Saver On" else "",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                maxLines = 1,
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = GlanceModifier
                                .size(32.dp)
                                .background(GlanceTheme.colors.tertiaryContainer)
                                .cornerRadius(16.dp)
                                .clickable(actionRunCallback<RefreshBatteryAction>()),
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_refresh),
                                contentDescription = "Refresh battery status",
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                                modifier = GlanceModifier.size(16.dp),
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(12.dp))

                    SegmentedMeterRow(
                        percent = snapshot.levelPercent,
                        segments = 8,
                        activeColor = if (snapshot.isCharging) {
                            GlanceTheme.colors.tertiary
                        } else {
                            GlanceTheme.colors.primary
                        },
                        inactiveColor = GlanceTheme.colors.surfaceVariant,
                    )
                }
            }
        }

        // Standard 2x2 Material You Scalloped Cookie Battery Gauge
        else -> {
            val cookieSize = size.width.coerceAtMost(size.height)
            val cookieContainer = if (snapshot.isCharging) {
                GlanceTheme.colors.tertiaryContainer
            } else {
                GlanceTheme.colors.primaryContainer
            }
            val cookieContent = if (snapshot.isCharging) {
                GlanceTheme.colors.onTertiaryContainer
            } else {
                GlanceTheme.colors.onPrimaryContainer
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
                            imageProvider = ImageProvider(R.drawable.bg_scalloped_cookie),
                            colorFilter = ColorFilter.tint(cookieContainer),
                        )
                        .clickable(actionRunCallback<RefreshBatteryAction>()),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.secondaryContainer)
                                .cornerRadius(12.dp)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .clickable(actionStartActivity(batteryIntent)),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    provider = ImageProvider(iconRes),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
                                    modifier = GlanceModifier.size(13.dp),
                                )
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                Text(
                                    text = if (snapshot.isCharging) "CHARGING" else tempText,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSecondaryContainer,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                    ),
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        Text(
                            text = "${snapshot.levelPercent}%",
                            style = TextStyle(
                                color = cookieContent,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )

                        Text(
                            text = snapshot.sourceLabel,
                            style = TextStyle(
                                color = cookieContent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SegmentedMeterRow(
    percent: Int,
    segments: Int,
    activeColor: androidx.glance.unit.ColorProvider,
    inactiveColor: androidx.glance.unit.ColorProvider,
) {
    val activeSegments = ((percent.coerceIn(0, 100) * segments) / 100f)
        .toInt()
        .coerceIn(if (percent > 0) 1 else 0, segments)

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until segments) {
            val isFilled = i < activeSegments
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(12.dp)
                    .background(if (isFilled) activeColor else inactiveColor)
                    .cornerRadius(6.dp),
            ) {}
            if (i < segments - 1) {
                Spacer(modifier = GlanceModifier.width(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 160, heightDp = 160)
@Preview(widthDp = 280, heightDp = 140)
@Preview(widthDp = 180, heightDp = 72)
@Composable
fun BatteryWidgetPreview() {
    MaterialYouGlanceTheme {
        BatteryWidgetContent(
            snapshot = BatterySnapshot(
                levelPercent = 84,
                isCharging = true,
                sourceLabel = "AC Adapter",
                temperatureCelsius = 29.2f,
                isPowerSave = false,
            ),
        )
    }
}
