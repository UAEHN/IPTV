package com.uaehn.shasha.ui.player

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import com.uaehn.shasha.data.BufferProfile
import com.uaehn.shasha.data.Channel
import com.uaehn.shasha.data.Source
import kotlinx.coroutines.delay
import java.net.URI

@Immutable
data class PlaybackState(
    val buffering: Boolean = true,
    val playing: Boolean = false,
    val error: String? = null,
    val sourceIndex: Int = 0,
    val sourceCount: Int = 1,
    val sourceLabel: String = "",
    val controlsVisible: Boolean = true,
)

/** How long the overlay stays up after the last interaction. */
private const val CONTROLS_TIMEOUT_MS = 4_000L

/**
 * Owns the ExoPlayer instance and the failover logic.
 *
 * Broadcaster CDNs fail in ways a normal VOD player never sees: an endpoint
 * that resolves but 403s from one network, a variant that stalls, a stream
 * that goes down for an hour. So every channel carries more than one source
 * where one exists, and a playback error walks to the next one automatically
 * before it is ever shown to the viewer. Only when every source has failed
 * does the error surface.
 */
@UnstableApi
class PlaybackController internal constructor(
    private val context: Context,
    private val buffer: BufferProfile,
) {
    var state by mutableStateOf(PlaybackState())
        private set

    private var channel: Channel? = null
    private var attemptsThisChannel = 0

    /** Bumped on every interaction; the auto-hide effect keys off it. */
    var interactionTick by mutableStateOf(0)
        private set

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    buffer.minMs,
                    buffer.maxMs,
                    buffer.startMs,
                    // Rebuffer target is deliberately larger than the initial
                    // start target: recovering from a stall should be less
                    // eager than the first play, or a weak link ping-pongs.
                    buffer.startMs * 2,
                )
                .build(),
        )
        .setMediaSourceFactory(DefaultMediaSourceFactory(context))
        .build()
        .apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    state = state.copy(
                        buffering = playbackState == Player.STATE_BUFFERING,
                        playing = playbackState == Player.STATE_READY && playWhenReady,
                    )
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    state = state.copy(playing = isPlaying)
                    if (isPlaying) attemptsThisChannel = 0
                }

                override fun onPlayerError(error: PlaybackException) {
                    onSourceFailed(error)
                }
            })
        }

    fun play(channel: Channel, sourceIndex: Int = 0) {
        this.channel = channel
        attemptsThisChannel = 0
        state = state.copy(
            error = null,
            buffering = true,
            sourceIndex = sourceIndex,
            sourceCount = channel.sources.size.coerceAtLeast(1),
            sourceLabel = channel.sources.getOrNull(sourceIndex)?.label.orEmpty(),
            controlsVisible = true,
        )
        interactionTick++
        load(channel, sourceIndex)
    }

    private fun load(channel: Channel, index: Int) {
        val source = channel.sources.getOrNull(index) ?: return
        player.setMediaSource(mediaSource(channel, source))
        player.prepare()
        player.playWhenReady = true
    }

    private fun mediaSource(channel: Channel, source: Source): MediaSource {
        val factory = DefaultHttpDataSource.Factory()
            // ExoPlayer's default UA gets refused by a few broadcaster CDNs
            // that only expect browsers and set-top boxes.
            .setUserAgent(USER_AGENT)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)

        if (source.referer && channel.site.isNotBlank()) {
            val origin = runCatching {
                val uri = URI(channel.site)
                "${uri.scheme}://${uri.host}"
            }.getOrNull()
            val headers = buildMap {
                put("Referer", channel.site)
                origin?.let { put("Origin", it) }
            }
            factory.setDefaultRequestProperties(headers)
        }

        val item = MediaItem.Builder()
            .setUri(source.url)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    // Live edge chasing is what makes a channel change feel
                    // instant; without it ExoPlayer sits several segments back.
                    .setTargetOffsetMs(6_000)
                    .setMinPlaybackSpeed(0.97f)
                    .setMaxPlaybackSpeed(1.03f)
                    .build(),
            )
            .build()

        return HlsMediaSource.Factory(factory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(item)
    }

    private fun onSourceFailed(error: PlaybackException) {
        val current = channel ?: return
        val next = state.sourceIndex + 1
        attemptsThisChannel++

        // Walk the remaining sources once. The attempt counter stops a channel
        // whose sources all fail from looping forever.
        if (next < current.sources.size && attemptsThisChannel <= current.sources.size) {
            state = state.copy(
                sourceIndex = next,
                sourceLabel = current.sources[next].label,
                buffering = true,
                error = null,
            )
            load(current, next)
            return
        }

        state = state.copy(
            error = error.errorCodeName,
            buffering = false,
            playing = false,
            controlsVisible = true,
        )
    }

    fun retry() {
        val current = channel ?: return
        play(current, 0)
    }

    fun nextSource() {
        val current = channel ?: return
        if (current.sources.size < 2) return
        play(current, (state.sourceIndex + 1) % current.sources.size)
    }

    fun previousSource() {
        val current = channel ?: return
        if (current.sources.size < 2) return
        val size = current.sources.size
        play(current, (state.sourceIndex - 1 + size) % size)
    }

    fun togglePlay() {
        player.playWhenReady = !player.playWhenReady
        showControls()
    }

    fun toggleControls() {
        state = state.copy(controlsVisible = !state.controlsVisible)
        interactionTick++
    }

    fun showControls() {
        state = state.copy(controlsVisible = true)
        interactionTick++
    }

    internal fun hideControls() {
        if (state.error == null) state = state.copy(controlsVisible = false)
    }

    fun pause() {
        player.playWhenReady = false
    }

    fun resume() {
        val current = channel ?: return
        player.playWhenReady = true
        // A live stream paused for any length of time is stale; re-prepare so
        // playback resumes at the live edge rather than where it stopped.
        if (player.playbackState == Player.STATE_IDLE) load(current, state.sourceIndex)
        else player.seekToDefaultPosition()
    }

    fun release() {
        player.release()
    }

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

@UnstableApi
@Composable
fun rememberPlaybackController(buffer: BufferProfile): PlaybackController {
    val context = LocalContext.current
    // A buffer-profile change has to rebuild the player: LoadControl is fixed
    // at construction.
    val controller = remember(buffer) { PlaybackController(context.applicationContext, buffer) }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    LaunchedEffect(controller.interactionTick, controller.state.controlsVisible) {
        if (controller.state.controlsVisible) {
            delay(CONTROLS_TIMEOUT_MS)
            controller.hideControls()
        }
    }

    return controller
}
