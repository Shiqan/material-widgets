package com.example.materialwidgets.widget.music

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.KeyEvent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Listens for active system MediaSessions (Spotify, YouTube Music, Apple Music, etc.)
 * and synchronizes track metadata, playback state, and album art to the Music Glance widget.
 */
class MediaSessionListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSessionManager: MediaSessionManager? = null
    private var trackedController: MediaController? = null

    private val activeSessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            attachToPrimaryController(controllers)
        }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            syncCurrentControllerToWidgets()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            syncCurrentControllerToWidgets()
        }

        override fun onSessionDestroyed() {
            refreshSessionsAndSync()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val manager = getSystemService(MediaSessionManager::class.java)
        mediaSessionManager = manager
        val component = ComponentName(this, MediaSessionListenerService::class.java)
        try {
            manager?.addOnActiveSessionsChangedListener(activeSessionsChangedListener, component)
            val controllers = manager?.getActiveSessions(component)
            attachToPrimaryController(controllers)
        } catch (_: SecurityException) {
            // Notification access not yet granted
        }
    }

    override fun onListenerDisconnected() {
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
        } catch (_: Exception) {
        }
        trackedController?.unregisterCallback(controllerCallback)
        trackedController = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshSessionsAndSync()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun refreshSessionsAndSync() {
        val component = ComponentName(this, MediaSessionListenerService::class.java)
        try {
            val controllers = mediaSessionManager?.getActiveSessions(component)
            attachToPrimaryController(controllers)
        } catch (_: SecurityException) {
        }
    }

    private fun attachToPrimaryController(controllers: List<MediaController>?) {
        trackedController?.unregisterCallback(controllerCallback)
        val chosen = pickBestController(controllers)
        trackedController = chosen
        chosen?.registerCallback(controllerCallback)
        syncCurrentControllerToWidgets()
    }

    private fun syncCurrentControllerToWidgets() {
        val context = applicationContext
        serviceScope.launch(Dispatchers.IO) {
            MediaSessionControllerHelper.syncFromActiveSession(context)
        }
    }
}

object MediaSessionControllerHelper {

    const val ALBUM_ART_FILENAME = "music_widget_album_art.webp"

    /**
     * Checks whether the user has enabled Notification Access for this app.
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val myComponent = ComponentName(context, MediaSessionListenerService::class.java).flattenToString()
        return flat.split(":").any { it.equals(myComponent, ignoreCase = true) }
    }

    /**
     * Finds the best active MediaController (preferring one currently playing, or the most recent session).
     */
    fun getPrimaryMediaController(context: Context): MediaController? {
        if (!isNotificationListenerEnabled(context)) return null
        return try {
            val manager = context.getSystemService(MediaSessionManager::class.java) ?: return null
            val component = ComponentName(context, MediaSessionListenerService::class.java)
            val controllers = manager.getActiveSessions(component)
            pickBestController(controllers)
        } catch (_: SecurityException) {
            null
        }
    }

    /**
     * Reads metadata, playback state, and album art from the active MediaController
     * and pushes an update to all MusicAppWidget instances.
     */
    suspend fun syncFromActiveSession(context: Context): Boolean {
        val controller = getPrimaryMediaController(context) ?: return false
        val metadata = controller.metadata ?: return false
        val playbackState = controller.playbackState

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        if (title.isNullOrBlank()) return false

        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: "Unknown Artist"

        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: resolveAppLabel(context, controller.packageName)

        val durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L)
        val positionMs = (playbackState?.position ?: 0L).coerceAtLeast(0L)
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING ||
            playbackState?.state == PlaybackState.STATE_BUFFERING

        val progress = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Save album art bitmap to internal filesDir for downsampled widget decoding
        val artBitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val artFile = File(context.filesDir, ALBUM_ART_FILENAME)
        val hasArt = if (artBitmap != null && !artBitmap.isRecycled) {
            saveAlbumArtBitmap(artBitmap, artFile)
        } else {
            if (artFile.exists()) artFile.delete()
            false
        }

        val manager = GlanceAppWidgetManager(context)
        val musicWidgets = listOf(
            MusicAppWidget() to manager.getGlanceIds(MusicAppWidget::class.java),
            MusicPillAppWidget() to manager.getGlanceIds(MusicPillAppWidget::class.java),
        )
        for ((widget, glanceIds) in musicWidgets) {
            for (glanceId in glanceIds) {
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[MusicAppWidget.KEY_HAS_LIVE_SESSION] = true
                    prefs[MusicAppWidget.KEY_LIVE_TITLE] = title
                    prefs[MusicAppWidget.KEY_LIVE_ARTIST] = artist
                    prefs[MusicAppWidget.KEY_LIVE_ALBUM] = album
                    prefs[MusicAppWidget.KEY_LIVE_ELAPSED] = formatDurationMs(positionMs)
                    prefs[MusicAppWidget.KEY_LIVE_DURATION] = if (durationMs > 0L) formatDurationMs(durationMs) else "--:--"
                    prefs[MusicAppWidget.KEY_LIVE_PROGRESS] = progress
                    prefs[MusicAppWidget.KEY_LIVE_PACKAGE] = controller.packageName
                    prefs[MusicAppWidget.KEY_HAS_ALBUM_ART] = hasArt
                    prefs[MusicAppWidget.KEY_IS_PLAYING] = isPlaying
                    prefs[MusicAppWidget.KEY_ART_REVISION] = (prefs[MusicAppWidget.KEY_ART_REVISION] ?: 0) + 1
                }
                widget.update(context, glanceId)
            }
        }
        return true
    }

    /**
     * Dispatches Play/Pause to the active MediaSession (or falls back to system AudioManager media key).
     */
    fun togglePlayPause(context: Context): Boolean {
        val controller = getPrimaryMediaController(context)
        if (controller != null) {
            val state = controller.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
            return true
        }
        return dispatchMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    /**
     * Dispatches Skip to Next to the active MediaSession (or falls back to system AudioManager media key).
     */
    fun skipToNext(context: Context): Boolean {
        val controller = getPrimaryMediaController(context)
        if (controller != null) {
            controller.transportControls.skipToNext()
            return true
        }
        return dispatchMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    /**
     * Dispatches Skip to Previous to the active MediaSession (or falls back to system AudioManager media key).
     */
    fun skipToPrevious(context: Context): Boolean {
        val controller = getPrimaryMediaController(context)
        if (controller != null) {
            controller.transportControls.skipToPrevious()
            return true
        }
        return dispatchMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    private fun dispatchMediaKeyEvent(context: Context, keyCode: Int): Boolean {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return false
        val now = SystemClock.uptimeMillis()
        val downEvent = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
        val upEvent = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0)
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
        return true
    }

    private fun saveAlbumArtBitmap(bitmap: Bitmap, destFile: File): Boolean {
        return try {
            FileOutputStream(destFile).use { out ->
                val fmt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                bitmap.compress(fmt, 85, out)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveAppLabel(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    private fun formatDurationMs(ms: Long): String {
        val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }
}

private fun pickBestController(controllers: List<MediaController>?): MediaController? {
    if (controllers.isNullOrEmpty()) return null
    return controllers.firstOrNull {
        it.playbackState?.state == PlaybackState.STATE_PLAYING
    } ?: controllers.firstOrNull {
        it.metadata != null
    } ?: controllers.first()
}
