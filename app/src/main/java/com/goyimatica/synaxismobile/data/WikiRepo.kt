package com.goyimatica.synaxismobile.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class Doc(
    val id: String,
    val title: String = "",
    val intro: String = "",
    val full: String = "",
    val image: String = "",
    val imageFull: String = "",
    val wikiUrl: String = "",
    val fromOrthodoxWiki: Boolean = false,
    val missing: Boolean = false,
    val at: Long = 0L,
)

/**
 * Lives and pictures, fetched once and kept on the disk as plain JSON.
 *
 * On the pictures: OrthodoxWiki does not run the PageImages extension, so
 * prop=pageimages returns nothing there no matter how the query is phrased.
 * The only way to get an icon off that wiki is prop=images, which lists the
 * files a page uses, and then imageinfo on the file title. Wikipedia does run
 * PageImages, so it gets the short query. Both need a real User-Agent.
 */
object WikiRepo {

    private const val OW_API = "https://orthodoxwiki.org/api.php"
    private const val WP_API = "https://en.wikipedia.org/w/api.php"
    private const val OW_PAGE = "https://orthodoxwiki.org/index.php?title="
    private const val WP_PAGE = "https://en.wikipedia.org/wiki/"
    private const val AGENT = "Synaxis/1.0 (Android; an Orthodox reader; contact via GitHub)"

    private var dir: File? = null
    private val mem = ConcurrentHashMap<String, Doc>()

    fun init(context: Context) {
        val d = File(context.filesDir, "docs")
        if (!d.exists()) d.mkdirs()
        dir = d
    }

    private fun now() = System.currentTimeMillis()

    private fun file(id: String): File? = dir?.let { File(it, id + ".json") }

    /** Whatever is already on this device. Never touches the network. */
    fun cached(id: String): Doc? {
        mem[id]?.let { return it }
        val f = file(id) ?: return null
        if (!f.exists()) return null
        val read = runCatching { decode(JSONObject(f.readText())) }.getOrNull() ?: return null
        mem[id] = read
        return read
    }

    /**
     * The life, from the disk if we have it. If we have the text but no
     * picture - which is every life downloaded by an earlier build - only the
     * picture is fetched, and the cache entry is repaired in place.
     */
    suspend fun doc(saint: Saint, force: Boolean = false): Doc? = withContext(Dispatchers.IO) {
        val old = if (force) null else cached(saint.id)

        if (old != null && !old.missing && old.full.isNotBlank()) {
            if (old.image.isNotBlank()) return@withContext old
            val found = picture(saint, old.fromOrthodoxWiki)
                ?: return@withContext old
            val repaired = old.copy(image = found.first, imageFull = found.second, at = now())
            save(repaired)
            return@withContext repaired
        }

        val fresh = fetch(saint)
        if (fresh != null) save(fresh)
        fresh
    }

    // ---------------------------------------------------------------- fetch

    private fun fetch(saint: Saint): Doc {
        val ow = saint.owTitle.ifBlank { saint.name }
        val wp = saint.wikiTitle.ifBlank { saint.name }

        var body = if (ow.isNotBlank()) extract(OW_API, ow) else null
        var fromOw = body != null
        if (body == null && wp.isNotBlank()) body = extract(WP_API, wp)

        if (body.isNullOrBlank()) {
            return Doc(id = saint.id, title = saint.name, missing = true, at = now())
        }

        val text = tidy(body)
        val pic = picture(saint, fromOw)

        return Doc(
            id = saint.id,
            title = saint.name,
            intro = opening(text),
            full = text,
            image = pic?.first.orEmpty(),
            imageFull = pic?.second.orEmpty(),
            wikiUrl = if (fromOw) OW_PAGE + enc(ow) else WP_PAGE + enc(wp),
            fromOrthodoxWiki = fromOw,
            missing = false,
            at = now(),
        )
    }

    private fun extract(api: String, title: String): String? {
        val j = http(
            api + "?action=query&format=json&formatversion=2&redirects=1" +
                "&prop=extracts&explaintext=1&exsectionformat=plain&titles=" + enc(title)
        ) ?: return null
        val page = firstPage(j) ?: return null
        if (page.optBoolean("missing", false)) return null
        val e = page.optString("extract", "")
        return if (e.trim().length < 120) null else e
    }

    // -------------------------------------------------------------- picture

    /** Everything a wiki puts on a page that is not an icon of the saint. */
    private val JUNK = listOf(
        "logo", "icon", "wiki", "edit", "stub", "padlock", "ambox", "disambig",
        "question", "commons", "flag", "coat of arms", "map", "placeholder",
        "symbol", "button", "banner", "seal of",
    )

    private fun picture(saint: Saint, preferOw: Boolean): Pair<String, String>? {
        val ow = saint.owTitle.ifBlank { saint.name }
        val wp = saint.wikiTitle.ifBlank { saint.name }

        if (preferOw && ow.isNotBlank()) owPicture(ow)?.let { return it }
        if (wp.isNotBlank()) wpPicture(wp)?.let { return it }
        if (!preferOw && ow.isNotBlank()) owPicture(ow)?.let { return it }
        return null
    }

    private fun owPicture(title: String): Pair<String, String>? {
        val j = http(
            OW_API + "?action=query&format=json&formatversion=2&redirects=1" +
                "&prop=images&imlimit=25&titles=" + enc(title)
        ) ?: return null
        val page = firstPage(j) ?: return null
        val arr = page.optJSONArray("images") ?: return null

        for (i in 0 until arr.length()) {
            val name = arr.optJSONObject(i)?.optString("title", "").orEmpty()
            if (name.isBlank()) continue
            val lower = name.lowercase()
            if (lower.endsWith(".svg")) continue
            if (JUNK.any { lower.contains(it) }) continue
            fileUrls(OW_API, name)?.let { return it }
        }
        return null
    }

    private fun fileUrls(api: String, fileTitle: String): Pair<String, String>? {
        val j = http(
            api + "?action=query&format=json&formatversion=2" +
                "&prop=imageinfo&iiprop=url&iiurlwidth=900&titles=" + enc(fileTitle)
        ) ?: return null
        val info = firstPage(j)?.optJSONArray("imageinfo")?.optJSONObject(0) ?: return null
        val orig = info.optString("url", "")
        if (orig.isBlank()) return null
        val thumb = info.optString("thumburl", "")
        return Pair(thumb.ifBlank { orig }, orig)
    }

    private fun wpPicture(title: String): Pair<String, String>? {
        val j = http(
            WP_API + "?action=query&format=json&formatversion=2&redirects=1" +
                "&prop=pageimages&piprop=thumbnail%7Coriginal&pithumbsize=900&titles=" + enc(title)
        ) ?: return null
        val page = firstPage(j) ?: return null
        val thumb = page.optJSONObject("thumbnail")?.optString("source", "").orEmpty()
        val orig = page.optJSONObject("original")?.optString("source", "").orEmpty()
        if (thumb.isBlank() && orig.isBlank()) return null
        return Pair(thumb.ifBlank { orig }, orig.ifBlank { thumb })
    }

    // ----------------------------------------------------------------- text

    /** Cut the apparatus off the end and thin out the blank lines. */
    private fun tidy(raw: String): String {
        val stop = listOf(
            "References", "External links", "See also", "Sources", "Notes",
            "Further reading", "Bibliography", "Succession box",
        )
        val kept = ArrayList<String>()
        for (line in raw.lines()) {
            val t = line.trim()
            if (t.startsWith("==")) {
                val heading = t.trim('=', ' ')
                if (stop.any { heading.equals(it, ignoreCase = true) }) break
                kept.add(heading)
                continue
            }
            kept.add(t)
        }
        return kept.joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun opening(text: String): String {
        val paras = text.split("\n\n").map { it.trim() }.filter { it.length > 40 }
        if (paras.isEmpty()) return text.take(420)
        val first = paras[0]
        if (first.length >= 240 || paras.size == 1) return first
        return (first + " " + paras[1]).take(560)
    }

    // ----------------------------------------------------------------- disk

    private fun save(doc: Doc) {
        mem[doc.id] = doc
        val f = file(doc.id) ?: return
        runCatching { f.writeText(encode(doc).toString()) }
    }

    private fun encode(d: Doc) = JSONObject()
        .put("id", d.id)
        .put("title", d.title)
        .put("intro", d.intro)
        .put("full", d.full)
        .put("image", d.image)
        .put("imageFull", d.imageFull)
        .put("wikiUrl", d.wikiUrl)
        .put("ow", d.fromOrthodoxWiki)
        .put("missing", d.missing)
        .put("at", d.at)

    private fun decode(j: JSONObject) = Doc(
        id = j.optString("id", ""),
        title = j.optString("title", ""),
        intro = j.optString("intro", ""),
        full = j.optString("full", ""),
        image = j.optString("image", ""),
        imageFull = j.optString("imageFull", ""),
        wikiUrl = j.optString("wikiUrl", ""),
        fromOrthodoxWiki = j.optBoolean("ow", false),
        missing = j.optBoolean("missing", false),
        at = j.optLong("at", 0L),
    )

    suspend fun downloaded(): Int = withContext(Dispatchers.IO) {
        dir?.listFiles()?.count { it.name.endsWith(".json") } ?: 0
    }

    suspend fun cacheBytes(): Long = withContext(Dispatchers.IO) {
        dir?.listFiles()?.sumOf { it.length() } ?: 0L
    }

    suspend fun clear(): Unit? = withContext(Dispatchers.IO) {
        mem.clear()
        dir?.listFiles()?.forEach { it.delete() }
        null
    }

    /**
     * Every life, six at a time. Six is polite to the wiki and still gets
     * through the whole index in a couple of minutes on a decent connection.
     */
    suspend fun syncAll(saints: List<Saint>, onProgress: (Int, Int) -> Unit) {
        val total = saints.size
        val counted = AtomicInteger(0)
        onProgress(0, total)

        withContext(Dispatchers.IO) {
            saints.chunked(6).forEach { batch ->
                coroutineScope {
                    batch.map { s ->
                        async {
                            runCatching { doc(s) }
                            onProgress(counted.incrementAndGet(), total)
                        }
                    }.awaitAll()
                }
            }
        }
    }

    // ----------------------------------------------------------------- http

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun firstPage(j: JSONObject): JSONObject? {
        val pages = j.optJSONObject("query")?.optJSONArray("pages") ?: return null
        return if (pages.length() == 0) null else pages.optJSONObject(0)
    }

    private fun http(url: String): JSONObject? = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 18000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", AGENT)
            setRequestProperty("Accept", "application/json")
        }
        conn.inputStream.bufferedReader().use { JSONObject(it.readText()) }
    }.getOrNull()
}