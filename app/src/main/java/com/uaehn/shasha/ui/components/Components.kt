package com.uaehn.shasha.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.uaehn.shasha.data.Channel
import com.uaehn.shasha.ui.ShashaIcons
import com.uaehn.shasha.ui.theme.Dims
import com.uaehn.shasha.ui.theme.Palette
import com.uaehn.shasha.ui.theme.Type

/**
 * A 1px rule. Used constantly: the layout is separated by hairlines rather than
 * by boxes inside boxes, which keeps the channel artwork the only thing on
 * screen carrying visual weight.
 */
@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = Palette.Hairline) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

/**
 * Section heading: label, a rule running out to the trailing edge, then an
 * optional count. Borrowed from editorial layout rather than app chrome — it
 * separates without introducing another container.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Type.section, maxLines = 1)
        Spacer(Modifier.width(14.dp))
        Hairline(Modifier.weight(1f))
        if (trailing != null) {
            Spacer(Modifier.width(14.dp))
            Text(trailing, style = Type.meta, maxLines = 1)
        }
    }
}

/** Dot plus label marking a live stream. A chip, never a band across a card. */
@Composable
fun LiveBadge(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Palette.Ink.copy(alpha = 0.78f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Palette.Signal))
        Spacer(Modifier.width(6.dp))
        Text(label, style = Type.overline.copy(color = Palette.Paper), maxLines = 1)
    }
}

/**
 * Filter chip. Unselected is a hairline outline over nothing; selected is brass
 * text in a brass outline. Deliberately not a filled pill: a row of solid
 * coloured pills would fight the logos for attention.
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(7.dp)
    val border by animateColorAsState(
        when {
            selected -> Palette.Brass
            focused -> Palette.HairlineHi
            else -> Palette.Hairline
        },
        tween(120), label = "chipBorder",
    )
    val content = when {
        selected -> Palette.BrassBright
        focused -> Palette.Paper
        else -> Palette.PaperDim
    }

    Box(
        modifier
            .clip(shape)
            .background(if (focused && !selected) Palette.Plate else Color.Transparent)
            .border(if (selected || focused) 1.5.dp else 1.dp, border, shape)
            .pressable(onClick = onClick, interaction = interaction)
            .padding(horizontal = 15.dp, vertical = 9.dp),
    ) {
        Text(label, style = Type.label.copy(color = content), maxLines = 1)
    }
}

/**
 * The channel tile.
 *
 * Every channel ships a 16:10 artwork tile, so the wall stays even whether the
 * broadcaster's logo was available or the name had to be set as a wordmark.
 * There is no caption under the tile and no coloured band across it: the
 * artwork carries the identity, and the category is conveyed by which rail the
 * tile sits in.
 *
 * Focus is a brass edge, a lighter plate and a small scale step. No glow, no
 * coloured shadow, no lift.
 */
@Composable
fun ChannelTile(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isFavorite: Boolean = false,
) {
    val dims = Dims
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(dims.corner)

    val scale by animateFloatAsState(
        if (focused) dims.focusScale else 1f, tween(140), label = "tileScale",
    )
    val plate by animateColorAsState(
        if (focused) Palette.PlateHi else Palette.Plate, tween(140), label = "tilePlate",
    )
    val edge by animateColorAsState(
        if (focused) Palette.Brass else Palette.Hairline, tween(140), label = "tileEdge",
    )

    Box(
        modifier
            .width(dims.tile)
            .aspectRatio(1.6f)
            .scale(scale)
            .clip(shape)
            .background(plate)
            .border(if (focused) 2.dp else 1.dp, edge, shape)
            .pressable(onClick, onLongClick, interaction),
    ) {
        ChannelArtwork(
            channel,
            Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp),
        )

        if (isFavorite) {
            Icon(
                imageVector = ShashaIcons.BookmarkFilled,
                contentDescription = null,
                tint = Palette.Brass,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(13.dp),
            )
        }
    }
}

/**
 * Draws the bundled tile, falling back to the channel's name if the asset is
 * missing — which only happens for a channel introduced by a downloaded
 * catalog after this APK was built.
 */
@Composable
fun ChannelArtwork(channel: Channel, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = channel.logoModel,
        contentDescription = channel.ar,
        contentScale = ContentScale.Fit,
        modifier = modifier,
        loading = { Box(Modifier.fillMaxSize()) },
        error = { Monogram(channel) },
    )
}

@Composable
private fun Monogram(channel: Channel) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                channel.ar,
                style = Type.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(26.dp).height(2.dp).background(Palette.Brass))
        }
    }
}

/** A horizontal run of tiles beneath a section header. */
@Composable
fun Rail(
    title: String,
    channels: List<Channel>,
    favorites: Set<String>,
    onOpen: (Channel) -> Unit,
    onLongPress: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (channels.isEmpty()) return
    val dims = Dims

    Column(modifier) {
        SectionHeader(
            title,
            Modifier.padding(horizontal = dims.gutter),
            trailing = channels.size.toString(),
        )
        Spacer(Modifier.height(14.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(dims.gap),
            contentPadding = PaddingValues(horizontal = dims.gutter),
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelTile(
                    channel = channel,
                    onClick = { onOpen(channel) },
                    onLongClick = { onLongPress(channel) },
                    isFavorite = channel.id in favorites,
                )
            }
        }
    }
}

@Composable
fun IconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Palette.Paper,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    val bg by animateColorAsState(
        if (focused) Palette.PlateHi else Color.Transparent, tween(120), label = "actionBg",
    )
    Box(
        modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, if (focused) Palette.Brass else Color.Transparent, shape)
            .pressable(onClick, null, interaction)
            .padding(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (focused) Palette.BrassBright else tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Clickable plus focusable with the indication stripped out. The Material
 * ripple is another design language's signature; here, press and focus are
 * expressed by the component's own edge and plate.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pressable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    interaction: MutableInteractionSource,
): Modifier = this
    .combinedClickable(
        interactionSource = interaction,
        indication = null,
        onLongClick = onLongClick,
        onClick = onClick,
    )
    .focusable(interactionSource = interaction)
