package com.example.materialwidgets.widget.music

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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

data class MusicTrack(
    val title: String,
    val artist: String,
    val album: String,
    val elapsedLabel: String,
    val durationLabel: String,
    val progress: Float,
)

val SAMPLE_MUSIC_QUEUE = listOf(
    MusicTrack(
        title = "Midnight City",
        artist = "M83",
        album = "Hurry Up, We're Dreaming",
        elapsedLabel = "2:14",
        durationLabel = "4:03",
        progress = 0.55f,
    ),
    MusicTrack(
        title = "Veridis Quo",
        artist = "Daft Punk",
        album = "Discovery",
        elapsedLabel = "3:42",
        durationLabel = "5:45",
        progress = 0.64f,
    ),
    MusicTrack(
        title = "Everything In Its Right Place",
        artist = "Radiohead",
        album = "Kid A",
        elapsedLabel = "1:28",
        durationLabel = "4:11",
        progress = 0.35f,
    ),
)

class MusicAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicAppWidget()
}

class MusicAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val trackIndex = (prefs[KEY_TRACK_INDEX] ?: 0).mod(SAMPLE_MUSIC_QUEUE.size)
            val isPlaying = prefs[KEY_IS_PLAYING] ?: true
            val isFavorite = prefs[KEY_IS_FAVORITE] ?: false

            MaterialYouGlanceTheme {
                MusicWidgetContent(
                    track = SAMPLE_MUSIC_QUEUE[trackIndex],
                    isPlaying = isPlaying,
                    isFavorite = isFavorite,
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
        showWidgetErrorFallback(context, appWidgetId, "Music widget error")
    }

    companion object {
        val KEY_TRACK_INDEX = intPreferencesKey("music_track_index")
        val KEY_IS_PLAYING = booleanPreferencesKey("music_is_playing")
        val KEY_IS_FAVORITE = booleanPreferencesKey("music_is_favorite")
    }
}

class PlayPauseMusicAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[MusicAppWidget.KEY_IS_PLAYING] ?: true
            prefs[MusicAppWidget.KEY_IS_PLAYING] = !current
        }
        MusicAppWidget().update(context, glanceId)
    }
}

class NextTrackMusicAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[MusicAppWidget.KEY_TRACK_INDEX] ?: 0
            prefs[MusicAppWidget.KEY_TRACK_INDEX] = (current + 1).mod(SAMPLE_MUSIC_QUEUE.size)
            prefs[MusicAppWidget.KEY_IS_PLAYING] = true
        }
        MusicAppWidget().update(context, glanceId)
    }
}

class PreviousTrackMusicAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[MusicAppWidget.KEY_TRACK_INDEX] ?: 0
            prefs[MusicAppWidget.KEY_TRACK_INDEX] = (current - 1).mod(SAMPLE_MUSIC_QUEUE.size)
            prefs[MusicAppWidget.KEY_IS_PLAYING] = true
        }
        MusicAppWidget().update(context, glanceId)
    }
}

class ToggleFavoriteMusicAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[MusicAppWidget.KEY_IS_FAVORITE] ?: false
            prefs[MusicAppWidget.KEY_IS_FAVORITE] = !current
        }
        MusicAppWidget().update(context, glanceId)
    }
}

@Composable
fun MusicWidgetContent(
    track: MusicTrack,
    isPlaying: Boolean = true,
    isFavorite: Boolean = false,
) {
    val size = LocalSize.current
    val playPauseIcon = if (isPlaying) R.drawable.ic_music_pause else R.drawable.ic_music_play
    val favoriteIcon = if (isFavorite) R.drawable.ic_music_heart_filled else R.drawable.ic_music_heart_outline

    if (size.width >= 240.dp) {
        // Expressive 4x2 Material You Media Player Widget
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(28.dp)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Scalloped Vinyl / Album Art Medallion
            val artSize = (size.height - 28.dp).coerceAtMost(112.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .size(artSize)
                    .background(
                        imageProvider = ImageProvider(R.drawable.bg_scalloped_cookie),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiaryContainer),
                    )
                    .clickable(actionRunCallback<PlayPauseMusicAction>()),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(44.dp)
                        .background(GlanceTheme.colors.primary)
                        .cornerRadius(22.dp),
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_note),
                        contentDescription = track.album,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                        modifier = GlanceModifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(14.dp))

            // Track Info, Progress Bar, and Transport Controls
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = track.title,
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                        Text(
                            text = "${track.artist} • ${track.album}",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            maxLines = 1,
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = GlanceModifier
                            .size(34.dp)
                            .background(GlanceTheme.colors.secondaryContainer)
                            .cornerRadius(17.dp)
                            .clickable(actionRunCallback<ToggleFavoriteMusicAction>()),
                    ) {
                        Image(
                            provider = ImageProvider(favoriteIcon),
                            contentDescription = "Favorite track",
                            colorFilter = ColorFilter.tint(
                                if (isFavorite) GlanceTheme.colors.tertiary else GlanceTheme.colors.onSecondaryContainer
                            ),
                            modifier = GlanceModifier.size(18.dp),
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                LinearProgressIndicator(
                    progress = track.progress,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .cornerRadius(3.dp),
                    color = GlanceTheme.colors.primary,
                    backgroundColor = GlanceTheme.colors.surfaceVariant,
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${track.elapsedLabel} / ${track.durationLabel}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        modifier = GlanceModifier.defaultWeight(),
                    )

                    // Previous button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = GlanceModifier
                            .size(36.dp)
                            .background(GlanceTheme.colors.secondaryContainer)
                            .cornerRadius(18.dp)
                            .clickable(actionRunCallback<PreviousTrackMusicAction>()),
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_music_prev),
                            contentDescription = "Previous track",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
                            modifier = GlanceModifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // Expressive Pill Play/Pause FAB
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = GlanceModifier
                            .width(52.dp)
                            .height(36.dp)
                            .background(GlanceTheme.colors.primary)
                            .cornerRadius(18.dp)
                            .clickable(actionRunCallback<PlayPauseMusicAction>()),
                    ) {
                        Image(
                            provider = ImageProvider(playPauseIcon),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                            modifier = GlanceModifier.size(22.dp),
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // Next button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = GlanceModifier
                            .size(36.dp)
                            .background(GlanceTheme.colors.secondaryContainer)
                            .cornerRadius(18.dp)
                            .clickable(actionRunCallback<NextTrackMusicAction>()),
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_music_next),
                            contentDescription = "Next track",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
                            modifier = GlanceModifier.size(20.dp),
                        )
                    }
                }
            }
        }
    } else {
        // Compact 2x2 Material You Mini Media Controller
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(28.dp)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(40.dp)
                        .background(
                            imageProvider = ImageProvider(R.drawable.bg_scalloped_cookie),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.tertiaryContainer),
                        ),
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_note),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                        modifier = GlanceModifier.size(20.dp),
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(32.dp)
                        .background(GlanceTheme.colors.secondaryContainer)
                        .cornerRadius(16.dp)
                        .clickable(actionRunCallback<ToggleFavoriteMusicAction>()),
                ) {
                    Image(
                        provider = ImageProvider(favoriteIcon),
                        contentDescription = "Favorite",
                        colorFilter = ColorFilter.tint(
                            if (isFavorite) GlanceTheme.colors.tertiary else GlanceTheme.colors.onSecondaryContainer
                        ),
                        modifier = GlanceModifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Text(
                text = track.title,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Text(
                text = track.artist,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(32.dp)
                        .background(GlanceTheme.colors.secondaryContainer)
                        .cornerRadius(16.dp)
                        .clickable(actionRunCallback<PreviousTrackMusicAction>()),
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_prev),
                        contentDescription = "Previous",
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
                        modifier = GlanceModifier.size(18.dp),
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(32.dp)
                        .background(GlanceTheme.colors.primary)
                        .cornerRadius(16.dp)
                        .clickable(actionRunCallback<PlayPauseMusicAction>()),
                ) {
                    Image(
                        provider = ImageProvider(playPauseIcon),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                        modifier = GlanceModifier.size(18.dp),
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(32.dp)
                        .background(GlanceTheme.colors.secondaryContainer)
                        .cornerRadius(16.dp)
                        .clickable(actionRunCallback<NextTrackMusicAction>()),
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_next),
                        contentDescription = "Next",
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
                        modifier = GlanceModifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 280, heightDp = 140)
@Preview(widthDp = 155, heightDp = 155)
@Composable
fun MusicWidgetPreview() {
    MaterialYouGlanceTheme {
        MusicWidgetContent(track = SAMPLE_MUSIC_QUEUE.first(), isPlaying = true, isFavorite = true)
    }
}
