package com.uaehn.shasha.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val CANVAS = 24f
private const val STROKE = 1.8f

// Icons are tinted by the call site, so the geometry is authored in black and
// recoloured by Icon()'s ColorFilter.
private val PEN = SolidColor(Color.Black)

private fun icon(name: String, build: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = CANVAS.dp,
        defaultHeight = CANVAS.dp,
        viewportWidth = CANVAS,
        viewportHeight = CANVAS,
    ).apply(build).build()

/** An open stroke: round caps and joins, uniform weight. */
private fun ImageVector.Builder.line(pathData: String) {
    addPath(
        pathData = addPathNodes(pathData),
        stroke = PEN,
        strokeLineWidth = STROKE,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}

/** A solid shape, for marks that must stay legible at rail size. */
private fun ImageVector.Builder.solid(pathData: String) {
    addPath(pathData = addPathNodes(pathData), fill = PEN)
}

/**
 * The icon set is drawn here rather than pulled from Material because the stock
 * set is instantly recognisable and would flatten the app's character. All of
 * it is built on one grid: 24dp box, 1.8dp stroke, round caps.
 *
 * A few substitutions are deliberate. Settings is a set of sliders, not a
 * cogwheel. Favorites is a bookmark, not a heart or a star. Home is a broadcast
 * mark, not a house — this is a live TV app, and home is what is on air.
 */
object ShashaIcons {

    /** Broadcast: a source dot with arcs radiating from it. */
    val Home: ImageVector by lazy {
        icon("home") {
            solid("M12 13.7a1.7 1.7 0 1 0 0-3.4 1.7 1.7 0 0 0 0 3.4z")
            line("M8.4 8.4a5.1 5.1 0 0 0 0 7.2")
            line("M15.6 8.4a5.1 5.1 0 0 1 0 7.2")
            line("M5.6 5.6a9 9 0 0 0 0 12.8")
            line("M18.4 5.6a9 9 0 0 1 0 12.8")
        }
    }

    /** Channels: a tile wall. */
    val Grid: ImageVector by lazy {
        icon("grid") {
            line("M3.8 5.2h6.9v5.6H3.8z")
            line("M13.3 5.2h6.9v5.6h-6.9z")
            line("M3.8 13.2h6.9v5.6H3.8z")
            line("M13.3 13.2h6.9v5.6h-6.9z")
        }
    }

    val Search: ImageVector by lazy {
        icon("search") {
            line("M11 4.6a6.4 6.4 0 1 0 0 12.8 6.4 6.4 0 0 0 0-12.8z")
            line("M15.7 15.7 20 20")
        }
    }

    /** Bookmark, open at the foot. */
    val Bookmark: ImageVector by lazy {
        icon("bookmark") { line("M6.6 4.6h10.8v14.8L12 15.4l-5.4 4z") }
    }

    val BookmarkFilled: ImageVector by lazy {
        icon("bookmarkFilled") { solid("M6.6 4.6h10.8v14.8L12 15.4l-5.4 4z") }
    }

    /** Sliders, not a cogwheel. */
    val Sliders: ImageVector by lazy {
        icon("sliders") {
            line("M4 7.4h16")
            line("M4 12h16")
            line("M4 16.6h16")
            solid("M15.2 9.1a1.7 1.7 0 1 0 0-3.4 1.7 1.7 0 0 0 0 3.4z")
            solid("M8.4 13.7a1.7 1.7 0 1 0 0-3.4 1.7 1.7 0 0 0 0 3.4z")
            solid("M13.6 18.3a1.7 1.7 0 1 0 0-3.4 1.7 1.7 0 0 0 0 3.4z")
        }
    }

    val Play: ImageVector by lazy {
        icon("play") { solid("M7.6 5.1 19 12 7.6 18.9z") }
    }

    val Pause: ImageVector by lazy {
        icon("pause") {
            solid("M7.4 5.2h3.2v13.6H7.4z")
            solid("M13.4 5.2h3.2v13.6h-3.2z")
        }
    }

    /** Points at the start edge; Compose mirrors it under RTL. */
    val ChevronBack: ImageVector by lazy {
        icon("chevronBack") { line("M14.6 5.4 8 12l6.6 6.6") }
    }

    /** Channel step controls in the player. */
    val ChevronUp: ImageVector by lazy {
        icon("chevronUp") { line("M5.4 14.6 12 8l6.6 6.6") }
    }

    val ChevronDown: ImageVector by lazy {
        icon("chevronDown") { line("M5.4 9.4 12 16l6.6-6.6") }
    }

    val Close: ImageVector by lazy {
        icon("close") {
            line("M6.2 6.2 17.8 17.8")
            line("M17.8 6.2 6.2 17.8")
        }
    }

    val Check: ImageVector by lazy {
        icon("check") { line("M5 12.6 9.8 17.4 19 7.2") }
    }

    val Refresh: ImageVector by lazy {
        icon("refresh") {
            line("M19.2 12a7.2 7.2 0 1 1-2.5-5.5")
            line("M19.4 4.4v4.2h-4.2")
        }
    }

    /** Antenna: the failover / change-source control. */
    val Signal: ImageVector by lazy {
        icon("signal") {
            line("M4.6 19.4 12 5.6l7.4 13.8")
            line("M7.7 14.6h8.6")
        }
    }

    val Info: ImageVector by lazy {
        icon("info") {
            line("M12 4.4a7.6 7.6 0 1 0 0 15.2 7.6 7.6 0 0 0 0-15.2z")
            line("M12 11.4v4.4")
            solid("M12 9.4a1 1 0 1 0 0-2 1 1 0 0 0 0 2z")
        }
    }

    val Trash: ImageVector by lazy {
        icon("trash") {
            line("M4.8 6.6h14.4")
            line("M9.4 6.6V4.8h5.2v1.8")
            line("M6.7 6.6 7.6 19.2h8.8l.9-12.6")
        }
    }

    /** Aspect / fill toggle: four corner brackets. */
    val Expand: ImageVector by lazy {
        icon("expand") {
            line("M4.6 9V4.6H9")
            line("M15 4.6h4.4V9")
            line("M19.4 15v4.4H15")
            line("M9 19.4H4.6V15")
        }
    }
}
