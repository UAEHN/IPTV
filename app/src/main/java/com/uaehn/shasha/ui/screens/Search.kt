package com.uaehn.shasha.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.uaehn.shasha.AppState
import com.uaehn.shasha.R
import com.uaehn.shasha.data.Channel
import com.uaehn.shasha.data.matches
import com.uaehn.shasha.ui.ShashaIcons
import com.uaehn.shasha.ui.components.SectionHeader
import com.uaehn.shasha.ui.components.pressable
import com.uaehn.shasha.ui.theme.Dims
import com.uaehn.shasha.ui.theme.Palette
import com.uaehn.shasha.ui.theme.Type

@Composable
fun SearchScreen(
    state: AppState,
    onOpen: (Channel, List<Channel>) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = Dims
    var query by rememberSaveable { mutableStateOf("") }

    val results = remember(state.catalog, query) {
        if (query.isBlank()) state.catalog.channels
        else state.catalog.channels.filter { it.matches(query) }
    }

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = dims.gutter)
                .padding(top = 18.dp, bottom = 14.dp),
        ) {
            SearchField(query, { query = it })
            Spacer(Modifier.height(16.dp))
            SectionHeader(
                if (query.isBlank()) stringResource(R.string.section_all)
                else stringResource(R.string.nav_search),
                trailing = results.size.toString(),
            )
        }

        if (results.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.search_empty),
                body = query,
            )
        } else {
            ChannelGrid(
                channels = results,
                favorites = state.favorites,
                onOpen = { onOpen(it, results) },
                onToggleFavorite = onToggleFavorite,
            )
        }
    }
}

/**
 * A single hairline-bounded field. No filled background, no floating label:
 * the search box should not look heavier than the channel tiles beneath it.
 */
@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(9.dp)

    val edge by animateColorAsState(
        if (focused) Palette.Brass else Palette.Hairline, tween(140), label = "searchEdge",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.InkRaised)
            .border(if (focused) 1.5.dp else 1.dp, edge, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            ShashaIcons.Search,
            contentDescription = null,
            tint = if (focused) Palette.Brass else Palette.PaperFaint,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(11.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(stringResource(R.string.search_hint), style = Type.body)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = Type.label,
                singleLine = true,
                cursorBrush = SolidColor(Palette.Brass),
                interactionSource = interaction,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            ClearButton(onClick = { onValueChange("") })
        }
    }
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .pressable(onClick = onClick, interaction = interaction)
            .padding(3.dp),
    ) {
        Icon(
            ShashaIcons.Close,
            contentDescription = null,
            tint = if (focused) Palette.BrassBright else Palette.PaperFaint,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
fun FavoritesScreen(
    state: AppState,
    onOpen: (Channel, List<Channel>) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = Dims
    val favorites = state.favoriteChannels

    Column(modifier.fillMaxSize()) {
        Box(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = dims.gutter)
                .padding(top = 18.dp, bottom = 14.dp),
        ) {
            SectionHeader(
                stringResource(R.string.nav_favorites),
                trailing = favorites.size.toString(),
            )
        }

        if (favorites.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.favorites_empty_title),
                body = stringResource(R.string.favorites_empty_body),
            )
        } else {
            ChannelGrid(
                channels = favorites,
                favorites = state.favorites,
                onOpen = { onOpen(it, favorites) },
                onToggleFavorite = onToggleFavorite,
            )
        }
    }
}
