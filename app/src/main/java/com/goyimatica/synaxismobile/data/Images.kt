package com.goyimatica.synaxismobile.data

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/*
 * One image loader for the whole app.
 *
 * The User-Agent is the entire fix for the missing icons. Wikimedia's user
 * agent policy refuses requests from generic library defaults, and Coil's
 * default is OkHttp's, so every portrait was coming back 403 and drawing as
 * nothing. A descriptive agent with a contact address is what the policy asks
 * for and what a browser effectively sends.
 */
object Images {

    const val AGENT =
        "Synaxis/6.0 (Android; an Orthodox reader; https://github.com/Goyimatica/SynaxisMobile)"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", AGENT)
                    .header("Accept", "image/avif,image/webp,image/jpeg,image/png,*/*")
                    .header("Accept-Language", "en")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    fun loader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { client }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("icons"))
                    .maxSizeBytes(96L * 1024 * 1024)
                    .build()
            }
            .crossfade(180)
            .build()
}