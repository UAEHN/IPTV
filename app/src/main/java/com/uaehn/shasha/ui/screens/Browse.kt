package com.uaehn.shasha.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uaehn.shasha.AppState
import com.uaehn.shasha.R
import com.uaehn.shasha.data.Channel
import com.uaehn.shasha.ui.components.ChannelTile
import com.uaehn.shasha.ui.components.FilterChip
import com.uaehn.shasha.ui.components.SectionHeader
import com.uaehn.shasha.ui.theme.Dims
import com.uaehn.shasha.ui.theme.isArabicUi
import com.uaehn.shasha.ui.theme.Palette
import com.uaehn.shasha.ui.theme.Type

/**
 * The full wall. Category chips across the top, then an adaptive grid that
 * keeps the tile size constant and lets the column count fall out of the
 * screen width, so a phone, a tablet and a television all show tiles at the
 * same optical size rather than stretching three columns across a TV.
 */
@Composable
fun BrowseScreen(
    state: AppState,
    onOpen: (Channel, List<Channel>) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = Dims
    val arabic = isArabicUi
    var filter by rememberSaveable { mutableStateOf(ALL) }

    val channels = remember(state.catalog, filter) {
        if (filter == ALL) state.catalog.channels else state.catalog.inCategory(filter)
    }

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = dims.gutter)
                .padding(top = 18.dp, bottom = 14.dp),
        ) {
            SectionHeader(
                stringResource(R.string.section_all),
                trailing = channels.size.toString(),
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                FilterChip(
                    label = stringResource(R.string.cat_all),
                    selected = filter == ALL,
                    onClick = { filter = ALL },
                )
                state.catalog.categories.forEach { category ->
                    FilterChip(
                        label = category.label(arabic),
                        selected = filter == category.id,
                        onClick = { filter = category.id },
                    )
                }
            }
        }

        ChannelGrid(
            channels = channels,
            favorites = state.favorites,
            onOpen = { onOpen(it, channels) },
            onToggleFavorite = onToggleFavorite,
        )
    }
}

@Composable
fun ChannelGrid(
    channels: List<Channel>,
    favorites: Set<String>,
    onOpen: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = Dims
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = dims.tile),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = dims.gutter,
            end = dims.gutter,
            bottom = dims.sectionGap,
        ),
        horizontalArrangement = Arrangement.spacedBy(dims.gap),
        verticalArrangement = Arrangement.spacedBy(dims.gap),
    ) {
        items(channels, key = { it.id }) { channel ->
            ChannelTile(
                channel = channel,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpen(channel) },
                onLongClick = { onToggleFavorite(channel) },
                isFavorite = channel.id in favorites,
            )
        }
    }
}

/** Shown when a list is legitimately empty, so the screen still says something. */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Same brass rule the wordmark tiles use, so an empty screen still
            // looks like part of the app rather than a blank.
            Box(Modifier.width(30.dp).height(2.dp).background(Palette.Brass))
            Spacer(Modifier.height(18.dp))
            Text(title, style = Type.title, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                style = Type.body.copy(color = Palette.PaperFaint),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val ALL = "__all__"
