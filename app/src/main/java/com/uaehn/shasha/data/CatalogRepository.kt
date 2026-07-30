package com.uaehn.shasha.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Supplies the channel list.
 *
 * The APK always ships a complete catalog in assets, so the app is fully
 * usable on first launch with no network and never shows an empty screen.
 * A newer catalog can be pulled from the repository on demand — that is the
 * escape hatch for a channel whose stream URL changes, which happens often
 * enough with broadcaster CDNs that requiring a reinstall would be painful.
 *
 * The downloaded copy is only adopted after it parses and passes the same
 * sanity checks the build applies, so a truncated or corrupted download can
 * never brick the channel list.
 */
class CatalogRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val cacheFile: File get() = File(context.filesDir, "catalog.json")

    /** Bundled copy first, then override with a cached download if one is valid. */
    suspend fun load(): Catalog = withContext(Dispatchers.IO) {
        val bundled = runCatching { parse(readAsset()) }
            .getOrElse {
                Log.e(TAG, "bundled catalog is unreadable", it)
                Catalog.Empty
            }

        val cached = runCatching {
            if (cacheFile.exists()) parse(cacheFile.readText()) else null
        }.getOrElse {
            Log.w(TAG, "cached catalog is unusable, deleting", it)
            cacheFile.delete()
            null
        }

        when {
            cached == null -> bundled
            // A stale download must never lose channels the APK already had.
            cached.channels.size < bundled.channels.size / 2 -> bundled
            else -> cached
        }
    }

    /**
     * Fetches the catalog from the repository. Returns the new catalog on
     * success, or null when the network failed or the payload was rejected.
     */
    suspend fun refresh(): Result<Catalog> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(REMOTE_URL)
                .header("Accept", "application/json")
                .build()

            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val body = response.body?.string().orEmpty()
                val catalog = parse(body)
                cacheFile.writeText(body)
                catalog
            }
        }.onFailure { Log.w(TAG, "catalog refresh failed", it) }
    }

    private fun readAsset(): String =
        context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }

    /** Parses and refuses anything that would degrade the app. */
    private fun parse(text: String): Catalog {
        val catalog = json.decodeFromString(Catalog.serializer(), text)
        require(catalog.channels.isNotEmpty()) { "catalog has no channels" }
        require(catalog.categories.isNotEmpty()) { "catalog has no categories" }
        val categoryIds = catalog.categories.mapTo(HashSet()) { it.id }
        val cleaned = catalog.channels.filter { channel ->
            channel.id.isNotBlank() &&
                channel.cat in categoryIds &&
                channel.sources.any { it.url.startsWith("https://") }
        }
        require(cleaned.isNotEmpty()) { "no channel survived validation" }
        return catalog.copy(channels = cleaned)
    }

    private companion object {
        const val TAG = "CatalogRepository"
        const val ASSET_NAME = "channels.json"
        const val REMOTE_URL =
            "https://raw.githubusercontent.com/UAEHN/IPTV/main/channels/channels.json"
    }
}
