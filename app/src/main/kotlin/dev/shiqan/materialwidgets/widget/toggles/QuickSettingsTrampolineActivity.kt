package dev.shiqan.materialwidgets.widget.toggles

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Transparent trampoline activity launched via `actionStartActivity` from Quick Toggle widgets.
 * Using an Activity PendingIntent avoids Android 12+ Background Activity Launch restrictions
 * while letting us fall back across OEM-specific settings intents when a Settings Panel is
 * unavailable (e.g., `ACTION_INTERNET_CONNECTIVITY` on devices without Google's Internet panel).
 */
class QuickSettingsTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val target = intent?.data?.lastPathSegment
            ?: intent?.getStringExtra(EXTRA_TARGET)
            ?: TARGET_MOBILE_DATA

        val candidates = when (target) {
            TARGET_WIFI -> wifiSettingsIntents()
            else -> mobileDataSettingsIntents()
        }

        for (candidate in candidates) {
            val launched = try {
                startActivity(candidate)
                true
            } catch (_: Exception) {
                false
            }
            if (launched) break
        }

        val appContext = applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                val manager = GlanceAppWidgetManager(appContext)
                when (target) {
                    TARGET_WIFI -> {
                        val widget = WifiToggleAppWidget()
                        manager.getGlanceIds(WifiToggleAppWidget::class.java)
                            .forEach { widget.update(appContext, it) }
                    }
                    else -> {
                        val widget = DataToggleAppWidget()
                        manager.getGlanceIds(DataToggleAppWidget::class.java)
                            .forEach { widget.update(appContext, it) }
                    }
                }
            }
        }

        finish()
    }

    private fun mobileDataSettingsIntents(): List<Intent> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(Intent(Settings.ACTION_DATA_USAGE_SETTINGS))
        }
        add(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS))
        add(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
        add(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        add(Intent(Settings.ACTION_SETTINGS))
    }

    private fun wifiSettingsIntents(): List<Intent> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Intent(Settings.Panel.ACTION_WIFI))
        }
        add(Intent(Settings.ACTION_WIFI_SETTINGS))
        add(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        add(Intent(Settings.ACTION_SETTINGS))
    }

    companion object {
        const val EXTRA_TARGET = "quick_settings_target"
        const val TARGET_MOBILE_DATA = "data"
        const val TARGET_WIFI = "wifi"

        fun createIntent(context: Context, target: String): Intent =
            Intent(context, QuickSettingsTrampolineActivity::class.java).apply {
                // Distinct URI per target so PendingIntent.filterEquals() does not conflate widgets.
                data = Uri.parse("materialwidgets://quick-toggle/$target")
                putExtra(EXTRA_TARGET, target)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
    }
}
