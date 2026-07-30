package com.uaehn.shasha.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusGroup
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uaehn.shasha.R
import com.uaehn.shasha.ui.components.Hairline
import com.uaehn.shasha.ui.components.pressable
import com.uaehn.shasha.ui.theme.Dims
import com.uaehn.shasha.ui.theme.Palette
import com.uaehn.shasha.ui.theme.Type

private data class TabSpec(val destination: Destination, val icon: ImageVector, val labelRes: Int)

private val TAB_SPECS = listOf(
    TabSpec(Destination.Home, ShashaIcons.Home, R.string.nav_home),
    TabSpec(Destination.Browse, ShashaIcons.Grid, R.string.nav_browse),
    TabSpec(Destination.Search, ShashaIcons.Search, R.string.nav_search),
    TabSpec(Destination.Favorites, ShashaIcons.Bookmark, R.string.nav_favorites),
    TabSpec(Destination.Settings, ShashaIcons.Sliders, R.string.nav_settings),
)

/**
 * One navigation surface, two shapes.
 *
 * A phone gets a bottom bar because that is where a thumb is. A television
 * gets a side rail that stays collapsed to icons until something in it takes
 * focus, then widens to show labels — a D-pad user needs the labels only while
 * they are actually in the rail, and the rest of the time that width belongs
 * to the channel wall.
 */
@Composable
fun AppScaffold(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val dims = Dims
    val selected = navigator.currentTab

    if (dims.isTv || dims.widthDp >= 720) {
        Row(modifier.fillMaxSize().background(Palette.Ink)) {
            NavRail(selected, navigator::selectTab)
            Box(Modifier.fillMaxHeight().width(1.dp).background(Palette.Hairline))
            content(Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(modifier.fillMaxSize().background(Palette.Ink)) {
            content(Modifier.weight(1f).fillMaxWidth())
            Hairline()
            BottomBar(selected, navigator::selectTab)
        }
    }
}

@Composable
private fun NavRail(selected: Destination, onSelect: (Destination) -> Unit) {
    var anyFocused by remember { mutableStateOf(false) }
    val width by animateDpAsState(
        if (anyFocused) 232.dp else 92.dp, tween(180), label = "railWidth",
    )

    Column(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(Palette.InkRaised)
            .focusGroup()
            .onFocusChanged { anyFocused = it.hasFocus }
            .statusBarsPadding()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Wordmark(
            expanded = anyFocused,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
        )
        Spacer(Modifier.height(18.dp))

        TAB_SPECS.forEach { spec ->
            RailItem(
                spec = spec,
                selected = spec.destination == selected,
                expanded = anyFocused,
                onClick = { onSelect(spec.destination) },
            )
        }
    }
}

@Composable
private fun RailItem(
    spec: TabSpec,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(9.dp)

    val background by animateColorAsState(
        if (focused) Palette.PlateHi else Color.Transparent, tween(140), label = "railItemBg",
    )
    val tint = when {
        focused -> Palette.BrassBright
        selected -> Palette.Brass
        else -> Palette.PaperDim
    }

    Row(
        Modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .pressable(onClick = onClick, interaction = interaction)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A 3dp brass stub marks the selected tab. It sits inside the row, so
        // it reads as a marker rather than as a stripe applied to a card.
        Box(
            Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) Palette.Brass else Color.Transparent),
        )
        Spacer(Modifier.width(12.dp))
        Icon(spec.icon, contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
        if (expanded) {
            Spacer(Modifier.width(14.dp))
            Text(
                stringResource(spec.labelRes),
                style = Type.label.copy(color = tint),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BottomBar(selected: Destination, onSelect: (Destination) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Palette.InkRaised)
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TAB_SPECS.forEach { spec ->
            BottomBarItem(
                spec = spec,
                selected = spec.destination == selected,
                onClick = { onSelect(spec.destination) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    spec: TabSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val tint by animateColorAsState(
        if (selected) Palette.Brass else Palette.PaperFaint, tween(140), label = "tabTint",
    )

    Column(
        modifier
            .pressable(onClick = onClick, interaction = interaction)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(spec.icon, contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(5.dp))
        Text(
            stringResource(spec.labelRes),
            style = Type.meta.copy(color = tint),
            maxLines = 1,
        )
        Spacer(Modifier.height(5.dp))
        // Selection is a short brass underline, not a filled pill behind the icon.
        Box(
            Modifier
                .width(if (selected) 16.dp else 0.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (selected) Palette.Brass else Color.Transparent),
        )
    }
}

/** The app's name, set as type. The ش mark is reserved for the launcher icon. */
@Composable
fun Wordmark(expanded: Boolean = true, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            stringResource(R.string.home_greeting),
            // Collapsed, the rail is only 92dp wide, which the display size
            // would overrun.
            style = if (expanded) Type.display else Type.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (expanded) {
            Spacer(Modifier.height(3.dp))
            Text(stringResource(R.string.home_tagline), style = Type.meta, maxLines = 1)
        }
    }
}
