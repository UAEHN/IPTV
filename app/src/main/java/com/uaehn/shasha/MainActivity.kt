package com.uaehn.shasha

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.uaehn.shasha.data.AppLanguage
import com.uaehn.shasha.data.Channel
import com.uaehn.shasha.ui.AppScaffold
import com.uaehn.shasha.ui.Destination
import com.uaehn.shasha.ui.player.PlayerScreen
import com.uaehn.shasha.ui.rememberNavigator
import com.uaehn.shasha.ui.screens.BrowseScreen
import com.uaehn.shasha.ui.screens.FavoritesScreen
import com.uaehn.shasha.ui.screens.HomeScreen
import com.uaehn.shasha.ui.screens.SearchScreen
import com.uaehn.shasha.ui.screens.SettingsScreen
import com.uaehn.shasha.ui.theme.Palette
import com.uaehn.shasha.ui.theme.ShashaTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    /**
     * The saved UI language is applied to the base context so Android resolves
     * string resources against it. Default resources are Arabic, so an unset
     * or unrecognised locale lands on Arabic rather than English.
     */
    override fun attachBaseContext(newBase: Context) {
        val tag = newBase
            .getSharedPreferences(LOCALE_PREFS, Context.MODE_PRIVATE)
            .getString(LOCALE_KEY, null)
        super.attachBaseContext(newBase.withLocale(AppLanguage.from(tag)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Live TV: the screen must not dim while a channel is on.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent { ShashaRoot(::persistLanguage) }
    }

    /**
     * The language is mirrored into plain SharedPreferences because
     * [attachBaseContext] runs before any coroutine can read DataStore.
     */
    private fun persistLanguage(language: AppLanguage) {
        val prefs = getSharedPreferences(LOCALE_PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(LOCALE_KEY, null)
        if (stored == language.tag) return
        prefs.edit().putString(LOCALE_KEY, language.tag).apply()
        // First launch has nothing stored yet and the default already matches
        // what was applied in attachBaseContext, so recreating would only make
        // the app flash on startup.
        if (stored != null) recreate()
    }

    private companion object {
        const val LOCALE_PREFS = "shasha_locale"
        const val LOCALE_KEY = "language"
    }
}

private fun Context.withLocale(language: AppLanguage): Context {
    val locale = Locale.forLanguageTag(language.tag)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.setLayoutDirection(locale)
    }
    return createConfigurationContext(config)
}

@OptIn(UnstableApi::class)
@Composable
private fun ShashaRoot(onLanguageChosen: (AppLanguage) -> Unit) {
    val viewModel: AppViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val navigator = rememberNavigator()

    LaunchedEffect(state.settings.language) { onLanguageChosen(state.settings.language) }

    // Arabic is RTL and English is LTR; forcing the direction here rather than
    // relying on the configuration keeps the two consistent after a live
    // language switch.
    val direction = when (state.settings.language) {
        AppLanguage.Arabic -> LayoutDirection.Rtl
        AppLanguage.English -> LayoutDirection.Ltr
    }

    val openPlayer: (Channel, List<Channel>) -> Unit = { channel, queue ->
        viewModel.noteWatched(channel.id)
        navigator.push(Destination.Player(channel.id, queue.map { it.id }))
    }

    // Resume-on-open, once per process. Waits for the catalog so the channel id
    // can actually be resolved, and never fires if the viewer has already
    // navigated somewhere themselves.
    var resumeHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.loading, state.settings.resumeLastChannel, state.recents) {
        if (resumeHandled || state.loading) return@LaunchedEffect
        resumeHandled = true
        if (!state.settings.resumeLastChannel) return@LaunchedEffect
        if (navigator.current != Destination.Home) return@LaunchedEffect
        val last = state.recentChannels.firstOrNull() ?: return@LaunchedEffect
        navigator.push(Destination.Player(last.id, state.recents))
    }

    ShashaTheme(arabic = state.settings.language == AppLanguage.Arabic) {
      CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Box(Modifier.fillMaxSize().background(Palette.Ink)) {
            when (val current = navigator.current) {
                is Destination.Player -> {
                    val channel = state.catalog.channel(current.channelId)
                    if (channel == null) {
                        // The catalog was replaced underneath us by a refresh.
                        LaunchedEffect(current.channelId) { navigator.pop() }
                    } else {
                        PlayerScreen(
                            channel = channel,
                            queue = current.queue.mapNotNull { state.catalog.channel(it) },
                            buffer = state.settings.buffer,
                            isFavorite = channel.id in state.favorites,
                            onChannelChange = { next ->
                                viewModel.noteWatched(next.id)
                                navigator.replace(
                                    Destination.Player(next.id, current.queue),
                                )
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                            onExit = { if (!navigator.pop()) navigator.selectTab(Destination.Home) },
                        )
                    }
                }

                else -> AppScaffold(navigator) { contentModifier ->
                    when (current) {
                        Destination.Home -> HomeScreen(
                            state = state,
                            onOpen = openPlayer,
                            onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                            modifier = contentModifier,
                        )

                        Destination.Browse -> BrowseScreen(
                            state = state,
                            onOpen = openPlayer,
                            onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                            modifier = contentModifier,
                        )

                        Destination.Search -> SearchScreen(
                            state = state,
                            onOpen = openPlayer,
                            onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                            modifier = contentModifier,
                        )

                        Destination.Favorites -> FavoritesScreen(
                            state = state,
                            onOpen = openPlayer,
                            onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                            modifier = contentModifier,
                        )

                        Destination.Settings -> SettingsScreen(
                            state = state,
                            refresh = refresh,
                            versionName = BuildConfig.VERSION_NAME,
                            onRefreshCatalog = viewModel::refreshCatalog,
                            onSetLanguage = viewModel::setLanguage,
                            onSetBuffer = viewModel::setBuffer,
                            onSetResume = viewModel::setResume,
                            onClearRecents = viewModel::clearRecents,
                            modifier = contentModifier,
                        )

                        is Destination.Player -> Unit
                    }
                }
            }
        }
    }
  }
}
