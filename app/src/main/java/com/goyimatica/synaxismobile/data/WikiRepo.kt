package com.goyimatica.synaxismobile.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * A life, as fetched. "intro" is the lead section, "full" the whole article.
 * Both are plain text; the reader does its own typesetting.
 */
data class Doc(
    val id: String,
    val title: String,
    val intro: String,
    val full: String,
    val image: String?,
    val imageFull: String?,
    val wikiUrl: String?,
    val fromOrthodoxWiki: Boolean,
    val missing: Boolean,
    val at: Long
) {
    val hasFull: Boolean get() = full.length > intro.length + 200
    val words: Int get() = if (full.isBlank()) 0 else full.split(' ').size

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("intro", intro)
        .put("full", full)
        .put("img", image ?: JSONObject.NULL)
        .put("imgFull", imageFull ?: JSONObject.NULL)
        .put("wpUrl", wikiUrl ?: JSONObject.NULL)
        .put("ow", fromOrthodoxWiki)
        .put("missing", missing)
        .put("at", at)

    companion object {
        fun from(o: JSONObject) = Doc(
            id = o.optString("id"),
            title = o.optString("title"),
            intro = o.optString("intro", ""),
            full = o.optString("full", ""),
            image = o.optStringOrNull("img"),
            imageFull = o.optStringOrNull("imgFull"),
            wikiUrl = o.optStringOrNull("wpUrl"),
            fromOrthodoxWiki = o.optBoolean("ow", false),
            missing = o.optBoolean("missing", false),
            at = o.optLong("at", 0L)
        )
    }
}

object WikiRepo {

    private const val WP = "https://en.wikipedia.org/w/api.php"
    private const val OW = "https://orthodoxwiki.org/api.php"
    private const val UA = "SynaxisMobile/1.0 (Orthodox saints reader)"
    private const val TIMEOUT = 15000
    private const val STALE_AFTER = 90L * 24 * 60 * 60 * 1000  // ninety days

    private lateinit var dir: File
    private val lock = Mutex()
    private val memory = HashMap<String, Doc>()

    fun init(context: Context) {
        dir = File(context.applicationContext.filesDir, "docs")
        if (!dir.exists()) dir.mkdirs()
    }

    fun cached(id: String): Doc? = memory[id]

    /**
     * The life for a saint. Cache first, network only if needed, and a stale
     * cached copy is always preferred to nothing when the network is away.
     */
    suspend fun doc(saint: Saint, force: Boolean = false): Doc? {
        memory[saint.id]?.let { if (!force && !it.missing) return it }

        val onDisk = withContext(Dispatchers.IO) { readFile(saint.id) }
        if (onDisk != null && !force) {
            memory[saint.id] = onDisk
            val fresh = System.currentTimeMillis() - onDisk.at < STALE_AFTER
            if (fresh && !onDisk.missing) return onDisk
        }

        val fetched = withContext(Dispatchers.IO) { fetch(saint) }
        if (fetched != null) {
            memory[saint.id] = fetched
            withContext(Dispatchers.IO) { writeFile(fetched) }
            return fetched
        }

        return onDisk
    }

    // ---- network -------------------------------------------------------

    private fun fetch(saint: Saint): Doc? {
        // Wikipedia first: better images, more reliable uptime.
        if (saint.wikiTitle.isNotBlank()) {
            article(WP, saint.wikiTitle, saint.id, false)?.let { return it }
        }
        // Then OrthodoxWiki, which the browser build could never reach.
        if (saint.owTitle.isNotBlank()) {
            article(OW, saint.owTitle, saint.id, true)?.let { return it }
        }
        return null
    }

    private fun article(api: String, title: String, id: String, ow: Boolean): Doc? {
        val q = "?action=query&format=json&formatversion=2&redirects=1" +
            "&prop=extracts|pageimages|info&inprop=url&explaintext=1" +
            "&piprop=thumbnail|original&pithumbsize=640" +
            "&titles=" + URLEncoder.encode(title, "UTF-8")

        val body = get(api + q) ?: return null

        return try {
            val pages = JSONObject(body)
                .optJSONObject("query")
                ?.optJSONArray("pages") ?: return null
            val page = pages.optJSONObject(0) ?: return null
            if (page.optBoolean("missing", false)) return null

            val extract = page.optString("extract", "").trim()
            if (extract.isBlank()) return null

            val intro = extract.substringBefore("\n\n\n").take(1200).trim()
            val thumb = page.optJSONObject("thumbnail")?.optStringOrNull("source")
            val original = page.optJSONObject("original")?.optStringOrNull("source")

            Doc(
                id = id,
                title = page.optString("title", title),
                intro = if (intro.isBlank()) extract.take(600) else intro,
                full = extract,
                image = thumb,
                imageFull = original ?: thumb,
                wikiUrl = page.optStringOrNull("fullurl"),
                fromOrthodoxWiki = ow,
                missing = false,
                at = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun get(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Encoding", "gzip")
            }
            if (conn.responseCode !in 200..299) return null
            val raw = conn.inputStream
            val stream = if (conn.contentEncoding?.contains("gzip", true) == true) {
                java.util.zip.GZIPInputStream(raw)
            } else raw
            stream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    // ---- disk ----------------------------------------------------------

    private fun fileFor(id: String) = File(dir, id.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")

    private fun readFile(id: String): Doc? {
        val f = fileFor(id)
        if (!f.exists()) return null
        return try {
            Doc.from(JSONObject(f.readText()))
        } catch (e: Exception) {
            f.delete()
            null
        }
    }

    private fun writeFile(doc: Doc) {
        runCatching { fileFor(doc.id).writeText(doc.toJson().toString()) }
    }

    /** How many lives are already on the device. Shown in Settings. */
    suspend fun downloaded(): Int = lock.withLock {
        withContext(Dispatchers.IO) { dir.listFiles()?.size ?: 0 }
    }

    suspend fun cacheBytes(): Long = withContext(Dispatchers.IO) {
        dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        memory.clear()
        dir.listFiles()?.forEach { it.delete() }
    }

    /**
     * "Download everything" from Settings. Sequential on purpose - twenty
     * parallel requests to Wikipedia from a phone is how you get rate limited.
     * The pause is courtesy to a free service that is giving us the lives.
     */
    suspend fun syncAll(
        saints: List<Saint>,
        onProgress: (done: Int, total: Int) -> Unit
    ) {
        val pending = saints.filter { fileFor(it.id).let { f -> !f.exists() } }
        pending.forEachIndexed { i, saint ->
            doc(saint)
            onProgress(i + 1, pending.size)
            kotlinx.coroutines.delay(120)
        }
    }
}