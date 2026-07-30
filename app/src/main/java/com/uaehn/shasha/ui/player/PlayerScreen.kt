package com.uaehn.shasha.ui.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.uaehn.shasha.R
import com.uaehn.shasha.data.BufferProfile
import com.uaehn.shasha.data.Channel
import com.uaehn.shasha.ui.ShashaIcons
import com.uaehn.shasha.ui.components.Hairline
import com.uaehn.shasha.ui.components.IconAction
import com.uaehn.shasha.ui.components.LiveBadge
import com.uaehn.shasha.ui.components.pressable
import com.uaehn.shasha.ui.theme.Palette
import com.uaehn.shasha.ui.theme.Type

/**
 * Full-screen live playback.
 *
 * The overlay is Compose over a bare [PlayerView]; Media3's stock controller is
 * switched off entirely. A live channel has no timeline to scrub, so the
 * controls that matter are: which channel, which source, and how to get out.
 *
 * On a remote: up/down change channel within the list the viewer came from,
 * left/right switch to another source for the current channel, centre toggles
 * the overlay, back leaves. On a phone the same actions are buttons.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel,
    queue: List<Channel>,
    buffer: BufferProfile,
    isFavorite: Boolean,
    onChannelChange: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = rememberPlaybackController(buffer)
    val state = controller.state
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(channel.id) { controller.play(channel) }

    // Playback must stop when the app goes to the background: a live stream
    // left running is a data bill, and audio-only TV is not a feature anyone
    // asked for here.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> controller.pause()
                Lifecycle.Event.ON_START -> controller.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val index = queue.indexOfFirst { it.id == channel.id }
    fun step(delta: Int) {
        if (queue.size < 2) return
        val from = if (index >= 0) index else 0
        onChannelChange(queue[(from + delta + queue.size) % queue.size])
    }

    BackHandler { onExit() }

    Box(
        modifier
            .fillMaxSize()
            .background(Palette.Ink)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp, Key.ChannelUp -> { step(-1); true }
                    Key.DirectionDown, Key.ChannelDown -> { step(1); true }
                    Key.DirectionLeft -> { controller.previousSource(); true }
                    Key.DirectionRight -> { controller.nextSource(); true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        controller.toggleControls(); true
                    }
                    Key.MediaPlay, Key.MediaPause, Key.MediaPlayPause -> {
                        controller.togglePlay(); true
                    }
                    else -> false
                }
            },
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { view -> view.player = controller.player },
            onRelease = { view -> view.player = null },
            modifier = Modifier.fillMaxSize(),
        )

        if (state.error != null) {
            ErrorPanel(
                channel = channel,
                sourceLabel = state.sourceLabel,
                hasAnotherSource = state.sourceCount > 1,
                onRetry = controller::retry,
                onNextSource = controller::nextSource,
                onExit = onExit,
            )
        } else if (state.buffering) {
            BufferingIndicator()
        }

        AnimatedVisibility(
            visible = state.controlsVisible && state.error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopBar(
                channel = channel,
                sourceLabel = state.sourceLabel,
                sourceCount = state.sourceCount,
                isFavorite = isFavorite,
                onBack = onExit,
                onToggleFavorite = { onToggleFavorite(channel) },
                onNextSource = controller::nextSource,
            )
        }

        AnimatedVisibility(
            visible = state.controlsVisible && state.error == null && queue.size > 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomHint(
                position = if (index >= 0) index + 1 else 1,
                total = queue.size,
                onPrevious = { step(-1) },
                onNext = { step(1) },
            )
        }

        // A tap anywhere that is not a control toggles the overlay.
        Box(
            Modifier
                .fillMaxSize()
                .pressable(
                    onClick = controller::toggleControls,
                    interaction = remember { MutableInteractionSource() },
                ),
        )
    }
}

@Composable
private fun TopBar(
    channel: Channel,
    sourceLabel: String,
    sourceCount: Int,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onNextSource: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(Palette.Scrim)) {
        Row(
            Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(ShashaIcons.ChevronBack, stringResource(R.string.nav_home), onBack)
            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    channel.ar,
                    style = Type.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(
                        channel.en.takeIf { it.isNotBlank() },
                        channel.countryAr.takeIf { it.isNotBlank() },
                        sourceLabel.takeIf { it.isNotBlank() },
                    ).joinToString("  ·  "),
                    style = Type.meta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            LiveBadge(stringResource(R.string.live))
            Spacer(Modifier.width(10.dp))

            if (sourceCount > 1) {
                IconAction(
                    ShashaIcons.Signal,
                    stringResource(R.string.player_next_source),
                    onNextSource,
                )
            }
            IconAction(
                if (isFavorite) ShashaIcons.BookmarkFilled else ShashaIcons.Bookmark,
                stringResource(
                    if (isFavorite) R.string.remove_favorite else R.string.add_favorite,
                ),
                onToggleFavorite,
                tint = if (isFavorite) Palette.Brass else Palette.Paper,
            )
        }
        Hairline(color = Palette.HairlineHi)
    }
}

@Composable
private fun BottomHint(
    position: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Hairline(color = Palette.HairlineHi)
        Row(
            Modifier
                .fillMaxWidth()
                .background(Palette.Scrim)
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconAction(ShashaIcons.ChevronUp, stringResource(R.string.nav_browse), onPrevious)
            IconAction(ShashaIcons.ChevronDown, stringResource(R.string.nav_browse), onNext)
            Spacer(Modifier.width(6.dp))
            Text("$position / $total", style = Type.meta)
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun BufferingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // A brass bar rather than a spinner: a spinning ring is the most
            // over-used loading affordance there is.
            Box(Modifier.width(38.dp).height(3.dp).background(Palette.Brass))
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.player_buffering), style = Type.meta)
        }
    }
}

@Composable
private fun ErrorPanel(
    channel: Channel,
    sourceLabel: String,
    hasAnotherSource: Boolean,
    onRetry: () -> Unit,
    onNextSource: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Palette.Ink),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.width(30.dp).height(2.dp).background(Palette.Signal))
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.player_error_title), style = Type.title)
            Spacer(Modifier.height(6.dp))
            Text(
                listOfNotNull(channel.ar, sourceLabel.takeIf { it.isNotBlank() })
                    .joinToString("  ·  "),
                style = Type.meta,
            )
            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineButton(stringResource(R.string.player_retry), onRetry)
                if (hasAnotherSource) {
                    OutlineButton(stringResource(R.string.player_next_source), onNextSource)
                }
                OutlineButton(stringResource(R.string.nav_home), onExit)
            }
        }
    }
}

@Composable
private fun OutlineButton(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (focused) Palette.Brass else Palette.InkRaised)
            .border(1.dp, if (focused) Palette.Brass else Palette.HairlineHi, shape)
            .pressable(onClick = onClick, interaction = interaction)
            .padding(horizontal = 18.dp, vertical = 11.dp),
    ) {
        Text(
            label,
            style = Type.label.copy(
                color = if (focused) Palette.Ink else Palette.Paper,
            ),
        )
    }
}
