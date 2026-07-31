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
 * Two things this repository has learned the hard way. OrthodoxWiki does not
 * run the PageImages extension, so a picture can only be had from it by
 * listing the page's files and then asking for one file's URL. And its
 * articles put `== Sources ==` in the middle rather than at the end, so
 * cutting the text at the first apparatus heading throws the writings away.
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
     * The life, from the disk if we have it.
     *
     * Two repairs happen without a full refetch: a life with text but no
     * picture gets the picture, and a life that was cut short by the old
     * `tidy` - anything under six hundred characters that came from a wiki
     * that is usually longer - is fetched again once.
     */
    suspend fun doc(saint: Saint, force: Boolean = false): Doc? = withContext(Dispatchers.IO) {
        val old = if (force) null else cached(saint.id)

        if (old != null && !old.missing && old.full.length >= 600) {
            if (old.image.isNotBlank()) return@withContext old
            val found = picture(saint, old.fromOrthodoxWiki) ?: return@withContext old
            val repaired = old.copy(image = found.first, imageFull = found.second, at = now())
            save(repaired)
            return@withContext repaired
        }

        val fresh = fetch(saint)
        if (fresh != null && (fresh.full.length >= (old?.full?.length ?: 0))) {
            save(fresh)
            return@withContext fresh
        }
        old ?: fresh
    }

    // ---------------------------------------------------------------- fetch

    /**
     * Both wikis are asked. OrthodoxWiki is the life; Wikipedia is the
     * biography. The Orthodox text is preferred unless the other is more than
     * half as long again, in which case the longer one is the one with the
     * works in it.
     */
    private fun fetch(saint: Saint): Doc {
        val ow = saint.owTitle.ifBlank { saint.name }
        val wp = saint.wikiTitle.ifBlank { saint.name }

        val fromOw = if (ow.isBlank()) null else body(OW_API, ow)
        val fromWp = if (wp.isBlank()) null else body(WP_API, wp)

        val useOw = when {
            fromOw == null -> false
            fromWp == null -> true
            else -> fromWp.length <= (fromOw.length * 3) / 2
        }

        val chosen = (if (useOw) fromOw else fromWp) ?: fromOw ?: fromWp

        if (chosen.isNullOrBlank()) {
            return Doc(id = saint.id, title = saint.name, missing = true, at = now())
        }

        val text = tidy(chosen)
        val pic = picture(saint, useOw)

        return Doc(
            id = saint.id,
            title = saint.name,
            intro = opening(text),
            full = text,
            image = pic?.first.orEmpty(),
            imageFull = pic?.second.orEmpty(),
            wikiUrl = if (useOw) OW_PAGE + enc(ow) else WP_PAGE + enc(wp),
            fromOrthodoxWiki = useOw,
            missing = false,
            at = now(),
        )
    }

    /** TextExtracts first; the raw wikitext if that comes back thin. */
    private fun body(api: String, title: String): String? {
        extract(api, title)?.let { if (it.length >= 400) return it }
        val raw = wikitext(api, title)
        val plain = extract(api, title)
        return when {
            raw != null && plain != null -> if (raw.length > plain.length) raw else plain
            raw != null -> raw
            else -> plain
        }
    }

    private fun extract(api: String, title: String): String? {
        val j = http(
            api + "?action=query&format=json&formatversion=2&redirects=1" +
                "&prop=extracts&explaintext=1&titles=" + enc(title)
        ) ?: return null
        val page = firstPage(j) ?: return null
        if (page.optBoolean("missing", false)) return null
        val e = page.optString("extract", "")
        return if (e.trim().length < 120) null else e
    }

    /**
     * The page as it was typed. Templates, files and tables are thrown away,
     * links are unwrapped, and the heading markers are kept - the reader sets
     * them as headings.
     */
    private fun wikitext(api: String, title: String): String? {
        val j = http(
            api + "?action=query&format=json&formatversion=2&redirects=1" +
                "&prop=revisions&rvprop=content&rvslots=main&titles=" + enc(title)
        ) ?: return null
        val page = firstPage(j) ?: return null
        if (page.optBoolean("missing", false)) return null
        val raw = page.optJSONArray("revisions")
            ?.optJSONObject(0)
            ?.optJSONObject("slots")
            ?.optJSONObject("main")
            ?.optString("content", "")
            .orEmpty()
        if (raw.isBlank()) return null

        var t = raw
        // Templates and tables, innermost first.
        repeat(6) { t = t.replace(Regex("\\{\\{[^{}]*\\}\\}"), " ") }
        t = t.replace(Regex("(?s)\\{\\|.*?\\|\\}"), " ")
        t = t.replace(Regex("(?s)<ref[^>]*>.*?</ref>"), "")
        t = t.replace(Regex("<ref[^>]*/>"), "")
        t = t.replace(Regex("(?s)<!--.*?-->"), "")
        t = t.replace(Regex("(?i)\\[\\[(File|Image|Category):[^\\]]*\\]\\]"), "")
        // [[target|shown]] and [[target]]
        t = t.replace(Regex("\\[\\[[^\\]|]*\\|([^\\]]*)\\]\\]"), "$1")
        t = t.replace(Regex("\\[\\[([^\\]]*)\\]\\]"), "$1")
        t = t.replace(Regex("\\[https?://\\S+ ([^\\]]*)\\]"), "$1")
        t = t.replace(Regex("\\[https?://\\S+\\]"), "")
        t = t.replace(Regex("<[^>]+>"), "")
        t = t.replace("'''", "").replace("''", "")
        t = t.replace(Regex("(?m)^[*#:;]+\\s*"), "")
        t = t.replace("&nbsp;", " ").replace("&amp;", "&").replace("&quot;", "\"")

        val cleaned = t.lines().joinToString("\n") { it.trim() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        return if (cleaned.length < 200) null else cleaned
    }

    // -------------------------------------------------------------- picture

    /** Everything a wiki puts on a page that is not an icon of the saint. */
    private val JUNK = listOf(
        "logo", "icon.png", "wiki", "edit", "stub", "padlock", "ambox", "disambig",
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

    /**
     * Take the apparatus off the end and nothing off the middle.
     *
     * The old version stopped at the first heading it recognised, which on
     * OrthodoxWiki is often `Sources` sitting halfway down with the writings
     * below it. This one finds the last apparatus heading that has nothing but
     * apparatus after it, and cuts there.
     */
    private fun tidy(raw: String): String {
        val apparatus = listOf(
            "references", "external links", "see also", "notes", "sources",
            "further reading", "bibliography", "succession box", "navigation",
        )

        val lines = raw.lines().map { it.trim() }

        // Walk backwards; keep dropping trailing apparatus sections.
        var cut = lines.size
        var i = lines.size - 1
        while (i >= 0) {
            val l = lines[i]
            if (l.startsWith("==") && l.endsWith("=")) {
                val head = l.trim('=', ' ').lowercase()
                if (apparatus.any { head == it }) {
                    cut = i
                    i--
                    continue
                }
                break
            }
            i--
        }

        val kept = lines.subList(0, cut.coerceAtLeast(1))
        return kept.joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    /** The opening, for the In brief card. Headings are never part of it. */
    private fun opening(text: String): String {
        val paras = text.split("\n\n")
            .map { it.trim() }
            .filter { it.length > 40 && !it.startsWith("==") }
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

    /** Every life, six at a time. */
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