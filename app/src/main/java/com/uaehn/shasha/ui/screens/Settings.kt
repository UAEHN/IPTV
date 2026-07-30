package com.uaehn.shasha.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uaehn.shasha.AppState
import com.uaehn.shasha.R
import com.uaehn.shasha.RefreshState
import com.uaehn.shasha.data.AppLanguage
import com.uaehn.shasha.data.BufferProfile
import com.uaehn.shasha.ui.ShashaIcons
import com.uaehn.shasha.ui.components.FilterChip
import com.uaehn.shasha.ui.components.Hairline
import com.uaehn.shasha.ui.components.SectionHeader
import com.uaehn.shasha.ui.components.pressable
import com.uaehn.shasha.ui.theme.Dims
import com.uaehn.shasha.ui.theme.Palette
import com.uaehn.shasha.ui.theme.Type

/**
 * Settings is a plain stack of rows separated by hairlines. No cards, no
 * grouped rounded boxes — the content here is text, and boxing text is the
 * fastest way to make a settings screen look generated.
 */
@Composable
fun SettingsScreen(
    state: AppState,
    refresh: RefreshState,
    versionName: String,
    onRefreshCatalog: () -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetBuffer: (BufferProfile) -> Unit,
    onSetResume: (Boolean) -> Unit,
    onClearRecents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dims = Dims
    val settings = state.settings

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = dims.sectionGap),
    ) {
        item("title") {
            Box(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = dims.gutter)
                    .padding(top = 18.dp, bottom = 18.dp),
            ) {
                SectionHeader(stringResource(R.string.nav_settings))
            }
        }

        item("playback") {
            Group(stringResource(R.string.settings_playback)) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.settings_buffer),
                        style = Type.label,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    BufferProfile.entries.forEach { profile ->
                        FilterChip(
                            label = stringResource(profile.labelRes()),
                            selected = settings.buffer == profile,
                            onClick = { onSetBuffer(profile) },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Hairline()
                ToggleRow(
                    label = stringResource(R.string.settings_autoplay),
                    checked = settings.resumeLastChannel,
                    onToggle = { onSetResume(!settings.resumeLastChannel) },
                )
            }
        }

        item("catalog") {
            Group(stringResource(R.string.settings_catalog)) {
                ActionRow(
                    icon = ShashaIcons.Refresh,
                    label = if (refresh == RefreshState.Running) {
                        stringResource(R.string.settings_updating)
                    } else {
                        stringResource(R.string.settings_update_catalog)
                    },
                    detail = "${state.catalog.channels.size} ${stringResource(R.string.settings_channels_count)}" +
                        when (refresh) {
                            RefreshState.Done -> "  ·  ${state.catalog.updated}"
                            RefreshState.Failed -> "  ·  ✕"
                            else -> ""
                        },
                    onClick = onRefreshCatalog,
                )
                Hairline()
                ActionRow(
                    icon = ShashaIcons.Trash,
                    label = stringResource(R.string.settings_clear_recents),
                    detail = state.recents.size.toString(),
                    onClick = onClearRecents,
                )
            }
        }

        item("language") {
            Group(stringResource(R.string.settings_language)) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    FilterChip(
                        label = stringResource(R.string.lang_arabic),
                        selected = settings.language == AppLanguage.Arabic,
                        onClick = { onSetLanguage(AppLanguage.Arabic) },
                    )
                    FilterChip(
                        label = stringResource(R.string.lang_english),
                        selected = settings.language == AppLanguage.English,
                        onClick = { onSetLanguage(AppLanguage.English) },
                    )
                }
            }
        }

        item("about") {
            Group(stringResource(R.string.settings_about)) {
                Text(
                    stringResource(R.string.about_legal_title),
                    style = Type.label,
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                )
                Text(stringResource(R.string.about_legal_body), style = Type.body)
                Spacer(Modifier.height(18.dp))
                Hairline()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.about_version),
                        style = Type.label,
                        modifier = Modifier.weight(1f),
                    )
                    Text(versionName, style = Type.meta)
                }
            }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    val dims = Dims
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.gutter)
            .padding(bottom = dims.sectionGap),
    ) {
        Text(
            title,
            style = Type.overline.copy(color = Palette.Brass),
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Hairline()
        content()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = onToggle, interaction = interaction)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = Type.label.copy(
                color = if (focused) Palette.BrassBright else Palette.Paper,
            ),
            modifier = Modifier.weight(1f),
        )
        Switch(checked, focused)
    }
}

/**
 * A rectangular switch with a square knob. Deliberately not the Material pill:
 * that shape is the single most recognisable component in the stock library.
 */
@Composable
private fun Switch(checked: Boolean, focused: Boolean) {
    val track by animateColorAsState(
        if (checked) Palette.Brass else Palette.Hairline, tween(140), label = "switchTrack",
    )
    val edge = if (focused) Palette.BrassBright else Color.Transparent

    Box(
        Modifier
            .width(46.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (checked) track else Palette.Plate)
            .border(1.dp, if (focused) edge else Palette.Hairline, RoundedCornerShape(5.dp)),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .size(18.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (checked) Palette.Ink else Palette.PaperFaint),
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    detail: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val tint = if (focused) Palette.BrassBright else Palette.Paper

    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick, interaction = interaction)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = Type.label.copy(color = tint), modifier = Modifier.weight(1f))
        Text(detail, style = Type.meta)
    }
}

private fun BufferProfile.labelRes(): Int = when (this) {
    BufferProfile.Low -> R.string.settings_buffer_low
    BufferProfile.Balanced -> R.string.settings_buffer_normal
    BufferProfile.High -> R.string.settings_buffer_high
}
