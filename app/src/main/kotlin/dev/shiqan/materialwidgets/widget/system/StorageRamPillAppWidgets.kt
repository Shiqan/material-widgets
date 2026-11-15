package dev.shiqan.materialwidgets.widget.system

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.shiqan.materialwidgets.R
import dev.shiqan.materialwidgets.widget.theme.MaterialYouGlanceTheme
import dev.shiqan.materialwidgets.widget.util.showWidgetErrorFallback
import java.util.Locale

// ---------------------------------------------------------------------------
// 1. 2x1 Storage Pill Widget (Left icon + Flush-right capacity pill)
// ---------------------------------------------------------------------------

class StoragePillAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StoragePillAppWidget()
}

class StoragePillAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(DpSize(190.dp, 76.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val revision = currentState<Preferences>()[KEY_REVISION] ?: 0
            val snapshot = StorageRamReader.read(context)
            MaterialYouGlanceTheme {
                StoragePillWidgetContent(
                    snapshot = snapshot,
                    revision = revision,
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val snapshot = StorageRamReader.read(context)
        provideContent {
            MaterialYouGlanceTheme {
                StoragePillWidgetContent(snapshot = snapshot)
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        showWidgetErrorFallback(context, appWidgetId, "Storage pill error")
    }

    companion object {
        val KEY_REVISION = intPreferencesKey("storage_pill_widget_revision")
    }
}

@Composable
fun StoragePillWidgetContent(
    snapshot: StorageRamSnapshot,
    @Suppress("UNUSED_PARAMETER") revision: Int = 0,
) {
    val size = LocalSize.current
    val pillHeight = (size.height * 0.56f).coerceIn(34.dp, 54.dp)
    val storageIntent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val label = String.format(
        Locale.getDefault(),
        "%.0f GB/%.0f GB",
        snapshot.storageUsedGb,
        snapshot.storageTotalGb,
    )

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity(storageIntent)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Storage Icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight(),
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_storage),
                contentDescription = "Internal Storage",
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(22.dp),
            )
        }

        // Right: Flush-right rounded pill with usage text
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .height(pillHeight)
                .background(
                    imageProvider = ImageProvider(R.drawable.bg_pill_flush_right),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiaryContainer),
                )
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = GlanceTheme.colors.onTertiaryContainer,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 2. 2x1 RAM Pill Widget (Flush-left usage pill + Right chip icon)
// ---------------------------------------------------------------------------

class RamPillAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RamPillAppWidget()
}

class RamPillAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(DpSize(190.dp, 76.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val revision = currentState<Preferences>()[KEY_REVISION] ?: 0
            val snapshot = StorageRamReader.read(context)
            MaterialYouGlanceTheme {
                RamPillWidgetContent(
                    snapshot = snapshot,
                    revision = revision,
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val snapshot = StorageRamReader.read(context)
        provideContent {
            MaterialYouGlanceTheme {
                RamPillWidgetContent(snapshot = snapshot)
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        showWidgetErrorFallback(context, appWidgetId, "RAM pill error")
    }

    companion object {
        val KEY_REVISION = intPreferencesKey("ram_pill_widget_revision")
    }
}

class RefreshRamPillAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[RamPillAppWidget.KEY_REVISION] = (prefs[RamPillAppWidget.KEY_REVISION] ?: 0) + 1
        }
        RamPillAppWidget().update(context, glanceId)
    }
}

@Composable
fun RamPillWidgetContent(
    snapshot: StorageRamSnapshot,
    @Suppress("UNUSED_PARAMETER") revision: Int = 0,
) {
    val size = LocalSize.current
    val pillHeight = (size.height * 0.56f).coerceIn(34.dp, 54.dp)
    val label = String.format(
        Locale.getDefault(),
        "%.1f GB / %.0f GB",
        snapshot.ramUsedGb,
        snapshot.ramTotalGb,
    )

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(24.dp)
            .clickable(actionRunCallback<RefreshRamPillAction>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Flush-left rounded pill with RAM usage text
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .height(pillHeight)
                .background(
                    imageProvider = ImageProvider(R.drawable.bg_pill_flush_left),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer),
                )
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }

        // Right: Memory Chip Icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight(),
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_memory),
                contentDescription = "Memory / RAM",
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(22.dp),
            )
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 190, heightDp = 76)
@Composable
fun StorageRamPillWidgetsPreview() {
    MaterialYouGlanceTheme {
        StoragePillWidgetContent(
            snapshot = StorageRamSnapshot(
                storageUsedGb = 76f,
                storageTotalGb = 128f,
                storagePercent = 59,
                ramUsedGb = 5.4f,
                ramTotalGb = 8f,
                ramPercent = 68,
            ),
        )
    }
}
