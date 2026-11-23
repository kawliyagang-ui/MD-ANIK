package com.example

import android.app.Application
import android.content.ComponentCallbacks2
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.imageLoader
import coil.memory.MemoryCache

/**
 * Application entry point that keeps Coil's bitmap memory footprint tightly bounded
 * so Chromium's isolated WebView renderer process always retains ample headroom.
 */
class CinetraApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.10)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(32L * 1024L * 1024L)
                    .build()
            }
            .allowRgb565(true)
            .respectCacheHeaders(false)
            .build()
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        ) {
            runCatching { imageLoader.memoryCache?.clear() }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        runCatching { imageLoader.memoryCache?.clear() }
    }
}
