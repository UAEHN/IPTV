package com.uaehn.shasha.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Where the app can be.
 *
 * The five tabs are peers — moving between them replaces the destination
 * rather than stacking it, which is what both a bottom bar and a TV side rail
 * imply. The player is the only thing that stacks.
 */
sealed interface Destination {
    data object Home : Destination
    data object Browse : Destination
    data object Search : Destination
    data object Favorites : Destination
    data object Settings : Destination

    /** [queue] is the list the viewer was looking at, so up/down can walk it. */
    data class Player(val channelId: String, val queue: List<String>) : Destination
}

val TABS = listOf(
    Destination.Home,
    Destination.Browse,
    Destination.Search,
    Destination.Favorites,
    Destination.Settings,
)

/**
 * A back stack in about thirty lines.
 *
 * Navigation-compose would work, but this app has five peers and one pushed
 * screen; a hand-rolled stack keeps full control over TV back behaviour and
 * removes a dependency whose graph builder would be longer than the thing it
 * replaces.
 */
class Navigator internal constructor(initial: List<Destination>) {

    private val stack: SnapshotStateList<Destination> =
        mutableStateListOf<Destination>().also { it.addAll(initial) }

    val current: Destination get() = stack.last()

    /** The tab currently showing, ignoring anything pushed on top of it. */
    val currentTab: Destination
        get() = stack.lastOrNull { it in TABS } ?: Destination.Home

    fun selectTab(tab: Destination) {
        // Tabs replace one another; only the player is ever stacked.
        stack.clear()
        stack.add(tab)
    }

    fun push(destination: Destination) {
        stack.add(destination)
    }

    /**
     * Swaps the top of the stack. Stepping through channels inside the player
     * must not deepen the stack, or back has to be pressed once per channel
     * the viewer flicked past.
     */
    fun replace(destination: Destination) {
        if (stack.isEmpty()) stack.add(destination) else stack[stack.lastIndex] = destination
    }

    /** Returns false when there is nothing left to pop, so the OS can exit. */
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    internal fun snapshot(): List<Destination> = stack.toList()

    companion object {
        /** Only tab destinations survive process death; a live stream should not resume blind. */
        val Saver = listSaver<Navigator, String>(
            save = { nav ->
                nav.snapshot().mapNotNull { destination ->
                    when (destination) {
                        Destination.Home -> "home"
                        Destination.Browse -> "browse"
                        Destination.Search -> "search"
                        Destination.Favorites -> "favorites"
                        Destination.Settings -> "settings"
                        is Destination.Player -> null
                    }
                }
            },
            restore = { keys ->
                val restored = keys.mapNotNull { key ->
                    when (key) {
                        "home" -> Destination.Home
                        "browse" -> Destination.Browse
                        "search" -> Destination.Search
                        "favorites" -> Destination.Favorites
                        "settings" -> Destination.Settings
                        else -> null
                    }
                }
                Navigator(restored.ifEmpty { listOf(Destination.Home) })
            },
        )
    }
}

@Composable
fun rememberNavigator(): Navigator =
    rememberSaveable(saver = Navigator.Saver) { Navigator(listOf(Destination.Home)) }
