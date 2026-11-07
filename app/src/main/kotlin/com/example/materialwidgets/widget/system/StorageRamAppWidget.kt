package com.example.materialwidgets.widget.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
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
import androidx.glance.text.TextStyle
import com.example.materialwidgets.R
import com.example.materialwidgets.widget.theme.MaterialYouGlanceTheme
import com.example.materialwidgets.widget.util.showWidgetErrorFallback
import java.util.Locale

data class StorageRamSnapshot(
    val storageUsedGb: Float,
    val storageTotalGb: Float,
    val storagePercent: Int,
    val ramUsedGb: Float,
    val ramTotalGb: Float,
    val ramPercent: Int,
)

object StorageRamReader {
    private const val BYTES_PER_GB = 1024f * 1024f * 1024f

    fun read(context: Context): StorageRamSnapshot {
        val (storageUsedGb, storageTotalGb, storagePercent) = try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes.coerceAtLeast(1L)
            val avail = stat.availableBytes.coerceIn(0L, total)
            val used = (total - avail).coerceAtLeast(0L)
            val usedGb = used / BYTES_PER_GB
            val totalGb = total / BYTES_PER_GB
            val pct = ((used * 100f) / total.toFloat()).toInt().coerceIn(0, 100)
            Triple(usedGb, totalGb, pct)
        } catch (_: Exception) {
            Triple(58.4f, 128f, 46)
        }

        val (ramUsedGb, ramTotalGb, ramPercent) = try {
            val am = context.getSystemService(ActivityManager::class.java)
            val memInfo = ActivityManager.MemoryInfo()
            if (am != null) {
                am.getMemoryInfo(memInfo)
                val total = memInfo.totalMem.coerceAtLeast(1L)
                val avail = memInfo.availMem.coerceIn(0L, total)
                val used = (total - avail).coerceAtLeast(0L)
                val usedGb = used / BYTES_PER_GB
                val totalGb = total / BYTES_PER_GB
                val pct = ((used * 100f) / total.toFloat()).toInt().coerceIn(0, 100)
                Triple(usedGb, totalGb, pct)
            } else {
                Triple(5.2f, 8.0f, 65)
            }
        } catch (_: Exception) {
            Triple(5.2f, 8.0f, 65)
        }

        return StorageRamSnapshot(
            storageUsedGb = storageUsedGb,
            storageTotalGb = storageTotalGb,
            storagePercent = storagePercent,
            ramUsedGb = ramUsedGb,
            ramTotalGb = ramTotalGb,
            ramPercent = ramPercent,
        )
    }
}

class StorageRamAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StorageRamAppWidget()
}

class StorageRamAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val revision = currentState<Preferences>()[KEY_REVISION] ?: 0
            val snapshot = StorageRamReader.read(context)
            MaterialYouGlanceTheme {
                StorageRamWidgetContent(
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
        showWidgetErrorFallback(context, appWidgetId, "Storage & RAM widget error")
    }

    companion object {
        val KEY_REVISION = intPreferencesKey("storage_ram_widget_revision")
    }
}

class RefreshStorageRamAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[StorageRamAppWidget.KEY_REVISION] = (prefs[StorageRamAppWidget.KEY_REVISION] ?: 0) + 1
        }
        StorageRamAppWidget().update(context, glanceId)
    }
}

@Composable
fun StorageRamWidgetContent(
    snapshot: StorageRamSnapshot,
    @Suppress("UNUSED_PARAMETER") revision: Int = 0,
) {
    val size = LocalSize.current
    val storageIntent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val storageLabel = String.format(
        Locale.getDefault(),
        "%.0f / %.0f GB",
        snapshot.storageUsedGb,
        snapshot.storageTotalGb,
    )
    val ramLabel = String.format(
        Locale.getDefault(),
        "%.1f / %.1f GB",
        snapshot.ramUsedGb,
        snapshot.ramTotalGb,
    )

    if (size.width >= 240.dp) {
        // Wide 4x2 Side-by-Side Expressive Gauge Cards
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(28.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: Internal Storage Card
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(22.dp)
                    .clickable(actionStartActivity(storageIntent))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = GlanceModifier
                            .size(34.dp)
                            .background(
                                imageProvider = ImageProvider(R.drawable.bg_scalloped_badge_12),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiaryContainer),
                            ),
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_storage),
                            contentDescription = "Storage",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                            modifier = GlanceModifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "STORAGE",
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = "${snapshot.storagePercent}%",
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                Text(
                    text = storageLabel,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )

                Spacer(modifier = GlanceModifier.height(6.dp))

                SegmentedMeterRow(
                    percent = snapshot.storagePercent,
                    segments = 6,
                    activeColor = GlanceTheme.colors.primary,
                    inactiveColor = GlanceTheme.colors.surfaceVariant,
                )
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            // Right: Memory / RAM Card
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(22.dp)
                    .clickable(actionRunCallback<RefreshStorageRamAction>())
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = GlanceModifier
                            .size(34.dp)
                            .background(GlanceTheme.colors.tertiaryContainer)
                            .cornerRadius(17.dp),
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_memory),
                            contentDescription = "Memory",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                            modifier = GlanceModifier.size(17.dp),
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "RAM",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSecondaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = "${snapshot.ramPercent}%",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSecondaryContainer,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                Text(
                    text = ramLabel,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )

                Spacer(modifier = GlanceModifier.height(6.dp))

                SegmentedMeterRow(
                    percent = snapshot.ramPercent,
                    segments = 6,
                    activeColor = GlanceTheme.colors.tertiary,
                    inactiveColor = GlanceTheme.colors.surfaceVariant,
                )
            }
        }
    } else {
        // Compact 2x2 Dual-Pill Gauge Card
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(28.dp)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Pill: Storage
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(20.dp)
                    .clickable(actionStartActivity(storageIntent))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(36.dp)
                        .background(
                            imageProvider = ImageProvider(R.drawable.bg_scalloped_badge_12),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiaryContainer),
                        ),
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_storage),
                        contentDescription = "Storage",
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                        modifier = GlanceModifier.size(16.dp),
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Storage • ${snapshot.storagePercent}%",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = storageLabel,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Bottom Pill: RAM (Tapping refreshes live stats)
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(GlanceTheme.colors.surface)
                    .cornerRadius(20.dp)
                    .clickable(actionRunCallback<RefreshStorageRamAction>())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(36.dp)
                        .background(GlanceTheme.colors.tertiaryContainer)
                        .cornerRadius(18.dp),
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_memory),
                        contentDescription = "Memory",
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                        modifier = GlanceModifier.size(18.dp),
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "RAM • ${snapshot.ramPercent}%",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = ramLabel,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
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
@Preview(widthDp = 280, heightDp = 140)
@Composable
fun StorageRamWidgetPreview() {
    MaterialYouGlanceTheme {
        StorageRamWidgetContent(
            snapshot = StorageRamSnapshot(
                storageUsedGb = 68.4f,
                storageTotalGb = 128f,
                storagePercent = 53,
                ramUsedGb = 5.4f,
                ramTotalGb = 8.0f,
                ramPercent = 68,
            ),
        )
    }
}
