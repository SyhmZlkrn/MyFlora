package com.example.flora

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Configures a Coil [ImageLoader] with a custom OkHttp client that sends a
 * Flora-specific User-Agent. Wikimedia Commons (used for disease reference
 * images) blocks requests without a UA, which is why thumbnails were appearing
 * as empty placeholders.
 *
 * Also enables aggressive disk caching so each disease image is fetched once
 * and then served offline.
 */
class FloraApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val ua = "MyFlora-Android/1.0 (myfloraapp.com; support@myfloraapp.com)"
        val okHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", ua)
                        .build()
                )
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttp)
            .memoryCache {
                MemoryCache.Builder(this).maxSizePercent(0.20).build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024)  // 64MB
                    .build()
            }
            .respectCacheHeaders(false)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }
}
