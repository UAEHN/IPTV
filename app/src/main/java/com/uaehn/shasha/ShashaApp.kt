package com.uaehn.shasha

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Channel artwork is bundled in assets, so the loader is tuned for decoding
 * local WebP quickly rather than for network fetching: a generous memory cache
 * keeps the whole wall resident after one scroll, and the disk cache exists
 * only for the remote logos a downloaded catalog might introduce.
 */
class ShashaApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("artwork"))
                    .maxSizeBytes(24L * 1024 * 1024)
                    .build()
            }
            .crossfade(false)
            .build()
}
