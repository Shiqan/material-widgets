package com.example.materialwidgets.widget.music

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
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
import com.example.materialwidgets.widget.util.decodeDownsampledWidgetBitmap
import com.example.materialwidgets.widget.util.showWidgetErrorFallback
import java.io.File

class MusicPillAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicPillAppWidget()
}

/**
 * 4x1 Stadium Pill Music Widget inspired by Material You Widgets Pro:
 * - Full-rounded dark/inverse surface capsule bar
 * - Circular album art avatar on the left
 * - Track title / "No Music Playing" & Artist / "Tap to Play" in the middle
 * - Nested tertiary pill on the right with Previous / Next and an overlapping
 *   12-lobe scalloped primary play/pause badge in the center.
 */
class MusicPillAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        MediaSessionControllerHelper.syncFromActiveSession(context)

        val artFile = File(context.filesDir, MediaSessionControllerHelper.ALBUM_ART_FILENAME)
        var cachedRevision = -1
        var cachedAlbumArt: Bitmap? = decodeDownsampledWidgetBitmap(
            context = context,
            sourceFile = artFile,
            distinctImageCount = 1,
            maxWidthDp = 72,
            maxHeightDp = 72,
            config = Bitmap.Config.RGB_565,
        )

        provideContent {
            val prefs = currentState<Preferences>()
            val hasLive = prefs[MusicAppWidget.KEY_HAS_LIVE_SESSION] ?: false
            val hasArt = prefs[MusicAppWidget.KEY_HAS_ALBUM_ART] ?: false
            val revision = prefs[MusicAppWidget.KEY_ART_REVISION] ?: 0

            if (revision != cachedRevision) {
                cachedRevision = revision
                cachedAlbumArt = if (hasArt) {
                    decodeDownsampledWidgetBitmap(
                        context = context,
                        sourceFile = artFile,
                        distinctImageCount = 1,
                        maxWidthDp = 72,
                        maxHeightDp = 72,
                        config = Bitmap.Config.RGB_565,
                    )
                } else {
                    null
                }
            }

            val isPlaying = prefs[MusicAppWidget.KEY_IS_PLAYING] ?: false
            val liveTitle = prefs[MusicAppWidget.KEY_LIVE_TITLE]
            val liveArtist = prefs[MusicAppWidget.KEY_LIVE_ARTIST]
            val trackIndex = prefs[MusicAppWidget.KEY_TRACK_INDEX]

            val title: String
            val subtitle: String
            when {
                hasLive && !liveTitle.isNullOrBlank() -> {
                    title = liveTitle
                    subtitle = liveArtist ?: "Tap to Play"
                }
                trackIndex != null -> {
                    val sample = SAMPLE_MUSIC_QUEUE[trackIndex.mod(SAMPLE_MUSIC_QUEUE.size)]
                    title = sample.title
                    subtitle = sample.artist
                }
                else -> {
                    title = "No Music Playing"
                    subtitle = "Tap to Play"
                }
            }

            MaterialYouGlanceTheme {
                MusicPillWidgetContent(
                    title = title,
                    subtitle = subtitle,
                    albumArtBitmap = cachedAlbumArt,
                    isPlaying = isPlaying,
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
        showWidgetErrorFallback(context, appWidgetId, "Music pill widget error")
    }
}

@Composable
fun MusicPillWidgetContent(
    title: String,
    subtitle: String,
    albumArtBitmap: Bitmap? = null,
    isPlaying: Boolean = false,
) {
    val size = LocalSize.current
    val pillHeight = size.height.coerceIn(52.dp, 84.dp)
    val pillRadius = pillHeight / 2
    val avatarSize = (pillHeight - 12.dp).coerceIn(40.dp, 64.dp)
    val playBadgeSize = (pillHeight - 12.dp).coerceIn(42.dp, 60.dp)
    val innerPillHeight = (playBadgeSize - 8.dp).coerceAtLeast(34.dp)
    val playPauseIcon = if (isPlaying) R.drawable.ic_music_pause else R.drawable.ic_music_play

    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier.fillMaxSize(),
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .height(pillHeight)
                .appWidgetBackground()
                .background(GlanceTheme.colors.inverseSurface)
                .cornerRadius(pillRadius)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left Circular Album Art Avatar
            if (albumArtBitmap != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(avatarSize)
                        .cornerRadius(avatarSize / 2)
                        .clickable(actionRunCallback<PlayPauseMusicAction>()),
                ) {
                    Image(
                        provider = ImageProvider(albumArtBitmap),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(avatarSize / 2),
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(avatarSize)
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(avatarSize / 2)
                        .clickable(actionRunCallback<PlayPauseMusicAction>()),
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_note),
                        contentDescription = title,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
                        modifier = GlanceModifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            // Middle Track Title + Subtitle ("No Music Playing" / "Tap to Play")
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionRunCallback<PlayPauseMusicAction>()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = GlanceTheme.colors.inverseOnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = subtitle,
                    style = TextStyle(
                        color = GlanceTheme.colors.inversePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    maxLines = 1,
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Right Nested Transport Pill with Overlapping Scalloped Play/Pause Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .width(114.dp)
                    .height(playBadgeSize),
            ) {
                // Background capsule holding Prev (left) and Next (right)
                Row(
                    modifier = GlanceModifier
                        .width(114.dp)
                        .height(innerPillHeight)
                        .background(GlanceTheme.colors.tertiaryContainer)
                        .cornerRadius(innerPillHeight / 2)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(innerPillHeight)
                            .clickable(actionRunCallback<PreviousTrackMusicAction>()),
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_music_prev),
                            contentDescription = "Previous track",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                            modifier = GlanceModifier.size(18.dp),
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(playBadgeSize - 8.dp))

                    Box(
                        contentAlignment = Alignment.CenterEnd,
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(innerPillHeight)
                            .clickable(actionRunCallback<NextTrackMusicAction>()),
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_music_next),
                            contentDescription = "Next track",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                            modifier = GlanceModifier.size(18.dp),
                        )
                    }
                }

                // Overlapping 12-Lobe Scalloped Center Play/Pause Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(playBadgeSize)
                        .background(
                            imageProvider = ImageProvider(R.drawable.bg_scalloped_badge_12),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer),
                        )
                        .clickable(actionRunCallback<PlayPauseMusicAction>()),
                ) {
                    Image(
                        provider = ImageProvider(playPauseIcon),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
                        modifier = GlanceModifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 64)
@Composable
fun MusicPillWidgetPreview() {
    MaterialYouGlanceTheme {
        MusicPillWidgetContent(
            title = "No Music Playing",
            subtitle = "Tap to Play",
            isPlaying = false,
        )
    }
}
