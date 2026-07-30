package com.uaehn.shasha.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uaehn.shasha.AppState
import com.uaehn.shasha.R
import com.uaehn.shasha.data.Channel
import com.uaehn.shasha.ui.ShashaIcons
import com.uaehn.shasha.ui.Wordmark
import com.uaehn.shasha.ui.components.ChannelArtwork
import com.uaehn.shasha.ui.components.LiveBadge
import com.uaehn.shasha.ui.components.Rail
import com.uaehn.shasha.ui.components.pressable
import com.uaehn.shasha.ui.theme.Dims
import com.uaehn.shasha.ui.theme.Palette
import com.uaehn.shasha.ui.theme.Type

/**
 * Home is a wall of rails, in the order someone actually reaches for: what
 * they were last watching, what they pinned, then the catalog by category.
 * There is no editorialised hero image, because this app has no artwork to
 * put in one that would not just be a logo blown up.
 */
@Composable
fun HomeScreen(
    state: AppState,
    onOpen: (Channel, List<Channel>) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = Dims
    val catalog = state.catalog
    val recents = state.recentChannels
    val resume = recents.firstOrNull()

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = dims.sectionGap),
        verticalArrangement = Arrangement.spacedBy(dims.sectionGap),
    ) {
        if (!dims.isTv) {
            item("header") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = dims.gutter)
                        .padding(top = 18.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Wordmark(modifier = Modifier.weight(1f))
                    Text(
                        "${catalog.channels.size}",
                        style = Type.meta,
                        maxLines = 1,
                    )
                }
            }
        } else {
            item("tvTopSpace") { Spacer(Modifier.height(dims.gutter)) }
        }

        if (resume != null) {
            item("resume") {
                NowPanel(
                    channel = resume,
                    categoryLabel = catalog.category(resume.cat)?.ar.orEmpty(),
                    onClick = { onOpen(resume, recents) },
                    modifier = Modifier.padding(horizontal = dims.gutter),
                )
            }
        }

        if (recents.size > 1) {
            item("recents") {
                Rail(
                    title = stringResource(R.string.section_continue),
                    channels = recents.drop(1),
                    favorites = state.favorites,
                    onOpen = { onOpen(it, recents.drop(1)) },
                    onLongPress = onToggleFavorite,
                )
            }
        }

        val favorites = state.favoriteChannels
        if (favorites.isNotEmpty()) {
            item("favorites") {
                Rail(
                    title = stringResource(R.string.section_favorites),
                    channels = favorites,
                    favorites = state.favorites,
                    onOpen = { onOpen(it, favorites) },
                    onLongPress = onToggleFavorite,
                )
            }
        }

        items(catalog.categories.size) { index ->
            val category = catalog.categories[index]
            val channels = catalog.inCategory(category.id)
            Rail(
                title = category.ar,
                channels = channels,
                favorites = state.favorites,
                onOpen = { onOpen(it, channels) },
                onLongPress = onToggleFavorite,
            )
        }
    }
}

/**
 * The resume panel. A wide plate holding the channel's own artwork, its name
 * and category, and a live badge — the one place on the home screen where a
 * channel is named in full.
 */
@Composable
private fun NowPanel(
    channel: Channel,
    categoryLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = Dims
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(dims.corner)

    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (focused) Palette.PlateHi else Palette.InkRaised)
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Palette.Brass else Palette.Hairline,
                shape,
            )
            .pressable(onClick = onClick, interaction = interaction)
            .padding(dims.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(if (dims.isTv) 190.dp else 128.dp)
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(dims.corner - 2.dp))
                .background(Palette.Plate),
        ) {
            ChannelArtwork(
                channel,
                Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }

        Spacer(Modifier.width(dims.gap + 4.dp))

        Column(Modifier.weight(1f)) {
            LiveBadge(stringResource(R.string.live))
            Spacer(Modifier.height(9.dp))
            Text(channel.ar, style = Type.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(
                listOfNotNull(
                    categoryLabel.takeIf { it.isNotBlank() },
                    channel.countryAr.takeIf { it.isNotBlank() },
                ).joinToString("  ·  "),
                style = Type.meta,
                maxLines = 1,
            )
        }

        Spacer(Modifier.width(dims.gap))

        // The play affordance is an outline, not a filled button: a solid
        // brass block here would be the loudest thing on the screen.
        Box(
            Modifier
                .size(if (dims.isTv) 52.dp else 44.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (focused) Palette.Brass else Color.Transparent)
                .border(
                    1.5.dp,
                    if (focused) Palette.Brass else Palette.HairlineHi,
                    RoundedCornerShape(9.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                ShashaIcons.Play,
                contentDescription = stringResource(R.string.watch),
                tint = if (focused) Palette.Ink else Palette.Paper,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}
