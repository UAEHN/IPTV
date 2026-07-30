package com.uaehn.shasha.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uaehn.shasha.R

/**
 * Ink & Brass.
 *
 * One accent, drawn from the gilding in Arabic manuscript work, over a cool
 * near-black. Deliberately no second hue: colour in this app belongs to the
 * channel logos and the video, not to the chrome around them. There is no
 * violet anywhere in the palette, no gradient fills, and nothing glows.
 */
object Palette {
    /** Page background. */
    val Ink = Color(0xFF0A0B0D)

    /** Bars, sheets, anything sitting directly on the page. */
    val InkRaised = Color(0xFF131519)

    /** The plate a channel logo sits on. */
    val Plate = Color(0xFF1B1E23)

    /** The same plate, focused or pressed. */
    val PlateHi = Color(0xFF23272E)

    /** Hairline rules and unfocused card edges. */
    val Hairline = Color(0xFF262A31)
    val HairlineHi = Color(0xFF3A414A)

    /** Warm off-white — reads as paper rather than as screen white. */
    val Paper = Color(0xFFF3F1EC)
    val PaperDim = Color(0xFF9AA1A9)
    val PaperFaint = Color(0xFF6A727A)

    /** The single accent. */
    val Brass = Color(0xFFC8A45D)
    val BrassBright = Color(0xFFE7CB92)

    /** Live indicator only. Never used for surfaces or text runs. */
    val Signal = Color(0xFFD9503F)

    /** Scrim over video when controls are showing. */
    val Scrim = Color(0xE60A0B0D)
}

val PlexArabic = FontFamily(
    Font(R.font.plex_arabic_regular, FontWeight.Normal),
    Font(R.font.plex_arabic_medium, FontWeight.Medium),
    Font(R.font.plex_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.plex_arabic_bold, FontWeight.Bold),
)

private val trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/**
 * Arabic script joins its letters, so tracking is only ever applied to styles
 * that are guaranteed to hold Latin text ([overline]). Applying it to Arabic
 * pulls the joins apart and makes the type look broken.
 */
@Immutable
class ShashaTypography(private val scale: Float) {
    private fun style(
        size: Float,
        weight: FontWeight,
        color: Color,
        height: Float = size * 1.45f,
        tracking: Float = 0f,
    ) = TextStyle(
        fontFamily = PlexArabic,
        fontWeight = weight,
        fontSize = (size * scale).sp,
        lineHeight = (height * scale).sp,
        letterSpacing = tracking.sp,
        color = color,
        lineHeightStyle = trim,
    )

    /** Screen titles and the wordmark. */
    val display = style(30f, FontWeight.Bold, Palette.Paper)

    /** Panel headings, the channel name in the player. */
    val title = style(20f, FontWeight.SemiBold, Palette.Paper)

    /** Rail and section headings. */
    val section = style(15f, FontWeight.SemiBold, Palette.Paper)

    /** Card captions and list rows. */
    val label = style(14f, FontWeight.Medium, Palette.Paper)

    /** Running text. */
    val body = style(15f, FontWeight.Normal, Palette.PaperDim, height = 24f)

    /** Counts, country names, secondary lines. */
    val meta = style(12.5f, FontWeight.Normal, Palette.PaperFaint)

    /** Latin-only. The one style allowed to carry tracking. */
    val overline = style(11f, FontWeight.Medium, Palette.PaperFaint, tracking = 1.1f)
}

/**
 * Sizes differ enough between a phone held at 30 cm and a TV watched from
 * three metres that a single scale cannot serve both. Everything measured in
 * the UI comes from here rather than from literals at the call site.
 */
@Immutable
class Metrics(val isTv: Boolean, val widthDp: Int) {
    val gutter: Dp = if (isTv) 48.dp else 20.dp
    val gap: Dp = if (isTv) 20.dp else 12.dp
    val sectionGap: Dp = if (isTv) 40.dp else 28.dp

    /** Width of one channel tile. Tiles are 16:10, captions live outside. */
    val tile: Dp = when {
        isTv -> 208.dp
        widthDp >= 840 -> 190.dp
        widthDp >= 600 -> 168.dp
        else -> 150.dp
    }

    val corner: Dp = if (isTv) 12.dp else 10.dp
    val railHeight: Dp get() = tile * 0.625f

    /** How far a focused tile grows. Restrained on purpose: no lift, no glow. */
    val focusScale: Float = if (isTv) 1.07f else 1.03f
}

val LocalMetrics: ProvidableCompositionLocal<Metrics> =
    staticCompositionLocalOf { Metrics(isTv = false, widthDp = 400) }

val LocalTypography: ProvidableCompositionLocal<ShashaTypography> =
    staticCompositionLocalOf { ShashaTypography(1f) }

/** Shorthand so screens read `Type.section` rather than `LocalTypography.current.section`. */
val Type: ShashaTypography
    @Composable @ReadOnlyComposable get() = LocalTypography.current

val Dims: Metrics
    @Composable @ReadOnlyComposable get() = LocalMetrics.current

fun Context.isTelevision(): Boolean {
    val mode = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    if (mode?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
    return packageManager.hasSystemFeature("android.software.leanback")
}

@Composable
fun ShashaTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val isTv = remember(context) { context.isTelevision() }

    val metrics = remember(isTv, config.screenWidthDp) {
        Metrics(isTv = isTv, widthDp = config.screenWidthDp)
    }
    // TV text is read from across a room; phone text is not.
    val typography = remember(isTv) { ShashaTypography(scale = if (isTv) 1.15f else 1f) }

    CompositionLocalProvider(
        LocalMetrics provides metrics,
        LocalTypography provides typography,
        content = content,
    )
}
