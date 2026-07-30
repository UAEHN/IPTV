package com.uaehn.shasha.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("shasha")

/** How much the player buffers before it starts. */
enum class BufferProfile(val key: String, val minMs: Int, val maxMs: Int, val startMs: Int) {
    /** Snappy channel changes on a good connection. */
    Low("low", minMs = 6_000, maxMs = 20_000, startMs = 1_200),

    /** The default: tolerates a normal mobile connection without stalling. */
    Balanced("balanced", minMs = 15_000, maxMs = 50_000, startMs = 2_500),

    /** For weak or congested links; slower to start, far less likely to stall. */
    High("high", minMs = 30_000, maxMs = 90_000, startMs = 5_000);

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key } ?: Balanced
    }
}

enum class AppLanguage(val tag: String) {
    Arabic("ar"),
    English("en");

    companion object {
        fun from(tag: String?) = entries.firstOrNull { it.tag == tag } ?: Arabic
    }
}

data class Settings(
    val language: AppLanguage = AppLanguage.Arabic,
    val buffer: BufferProfile = BufferProfile.Balanced,
    val resumeLastChannel: Boolean = false,
)

class Prefs(private val context: Context) {

    val favorites: Flow<Set<String>> =
        context.dataStore.data.map { it[FAVORITES] ?: emptySet() }

    /** Most recent first. */
    val recents: Flow<List<String>> =
        context.dataStore.data.map { prefs ->
            prefs[RECENTS]?.split(RECENT_SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
        }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            language = AppLanguage.from(prefs[LANGUAGE]),
            buffer = BufferProfile.from(prefs[BUFFER]),
            resumeLastChannel = prefs[RESUME] ?: false,
        )
    }

    suspend fun toggleFavorite(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES] ?: emptySet()
            prefs[FAVORITES] = if (id in current) current - id else current + id
        }
    }

    suspend fun noteWatched(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[RECENTS]?.split(RECENT_SEPARATOR).orEmpty()
                .filter { it.isNotBlank() && it != id }
            prefs[RECENTS] = (listOf(id) + current)
                .take(MAX_RECENTS)
                .joinToString(RECENT_SEPARATOR)
        }
    }

    suspend fun clearRecents() {
        context.dataStore.edit { it.remove(RECENTS) }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[LANGUAGE] = language.tag }
    }

    suspend fun setBuffer(profile: BufferProfile) {
        context.dataStore.edit { it[BUFFER] = profile.key }
    }

    suspend fun setResumeLastChannel(enabled: Boolean) {
        context.dataStore.edit { it[RESUME] = enabled }
    }

    private companion object {
        val FAVORITES = stringSetPreferencesKey("favorites")

        // Stored as one delimited string rather than a Set so the order, which
        // is the whole point of a recents list, survives a round trip.
        val RECENTS = stringPreferencesKey("recents")
        val LANGUAGE = stringPreferencesKey("language")
        val BUFFER = stringPreferencesKey("buffer")
        val RESUME = booleanPreferencesKey("resume_last")

        /** ASCII unit separator: cannot occur in a channel id. */
        const val RECENT_SEPARATOR = "\u001F"
        const val MAX_RECENTS = 20
    }
}
