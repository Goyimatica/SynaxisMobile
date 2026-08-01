package com.goyimatica.synaxismobile.data

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/*
 * One HTTP client and one image loader for the whole app.
 *
 * The User-Agent is what makes any of this work at all: Wikimedia's policy
 * refuses requests carrying a library default, and a refused image draws as
 * nothing, silently. Everything else here is about speed - one connection
 * pool, kept alive, shared by the pictures and by WikiRepo's API calls.
 */
object Images {

    const val AGENT =
        "Synaxis/7.0 (Android; an Orthodox reader; https://github.com/Goyimatica/SynaxisMobile)"

    /** Public on purpose: WikiRepo borrows it, so a sync reuses sockets. */
    val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .dispatcher(
                Dispatcher().apply {
                    maxRequests = 64
                    maxRequestsPerHost = 16
                }
            )
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", AGENT)
                    .header("Accept-Language", "en")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    fun loader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { http }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    /* V10: filesDir, not cacheDir. The OS may clear cacheDir
                       whenever it pleases; these icons are part of the
                       downloaded lives and should stay on the phone. */
                    .directory(context.filesDir.resolve("icons"))
                    .maxSizeBytes(128L * 1024 * 1024)
                    .build()
            }
            .crossfade(160)
            .build()
}