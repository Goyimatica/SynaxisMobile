package com.goyimatica.synaxismobile.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class Doc(
    val id: String,
    val title: String,
    val intro: String,
    val full: String,
    val image: String,
    val imageFull: String,
    val wikiUrl: String,
    val fromOrthodoxWiki: Boolean,
    val missing: Boolean,
    val at: Long,
)

object WikiRepo {

    const val OW_API = "https://orthodoxwiki.org/api.php"
    const val WP_API = "https://en.wikipedia.org/w/api.php"
    const val COMMONS_API = "https://commons.wikimedia.org/w/api.php"
    const val AGENT = Images.AGENT

    private const val TAG = "SynaxisImages"

    /* V7.1: 1600px. Wikimedia renders thumbnails up to about 2000px happily;
       1600 is sharper than any phone panel and still a fraction of the
       original file, which for a scanned icon can be twenty megabytes. */
    private const val THUMB = 1600

    private const val PARALLEL = 12
    private const val WEEK = 7L * 24L * 60L * 60L * 1000L

    private var dir: File? = null

    private val mem = ConcurrentHashMap<String, Doc>(64)
    private val diskLock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** saint id -> picture URL, readable from any composable for free. */
    val thumbs = mutableStateMapOf<String, String>()

    fun init(context: Context) {
        if (dir != null) return
        dir = File(context.cacheDir, "docs").apply { mkdirs() }
        scope.launch { indexThumbs() }
    }

    private fun indexThumbs() {
        val files = dir?.listFiles { f -> f.name.endsWith(".json") } ?: return
        files.forEach { f ->
            runCatching {
                val o = JSONObject(f.readText())
                val img = o.optString("image")
                if (img.isNotBlank()) {
                    thumbs[o.optString("id", f.nameWithoutExtension)] = img
                }
            }
        }
    }

    /* ---- cache ---------------------------------------------------------- */

    private fun fileFor(id: String): File? = dir?.let { File(it, id + ".json") }

    fun cached(id: String): Doc? {
        mem[id]?.let { return it }
        val f = fileFor(id) ?: return null
        if (!f.exists()) return null
        return runCatching {
            val o = JSONObject(f.readText())
            Doc(
                id = o.optString("id", id),
                title = o.optString("title"),
                intro = o.optString("intro"),
                full = o.optString("full"),
                image = o.optString("image"),
                imageFull = o.optString("imageFull"),
                wikiUrl = o.optString("wikiUrl"),
                fromOrthodoxWiki = o.optBoolean("fromOrthodoxWiki"),
                missing = o.optBoolean("missing"),
                at = o.optLong("at"),
            )
        }.getOrNull()?.also {
            mem[id] = it
            if (it.image.isNotBlank()) thumbs[id] = it.image
        }
    }

    /**
     * Which lives the startup sync actually has to fetch.
     *
     * A saint with no picture anywhere on either wiki would otherwise be
     * hunted again on every single launch, so a fruitless search is only
     * repeated once a week.
     */
    suspend fun pending(saints: List<Saint>): List<Saint> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        saints.filter { s ->
            val d = cached(s.id)
            when {
                d == null -> true
                d.missing -> now - d.at > WEEK
                d.full.length < 600 -> true
                d.image.isBlank() -> now - d.at > WEEK
                else -> false
            }
        }
    }

    private suspend fun save(doc: Doc) = diskLock.withLock {
        mem[doc.id] = doc
        if (doc.image.isNotBlank()) thumbs[doc.id] = doc.image
        val f = fileFor(doc.id) ?: return@withLock
        runCatching {
            val o = JSONObject()
                .put("id", doc.id)
                .put("title", doc.title)
                .put("intro", doc.intro)
                .put("full", doc.full)
                .put("image", doc.image)
                .put("imageFull", doc.imageFull)
                .put("wikiUrl", doc.wikiUrl)
                .put("fromOrthodoxWiki", doc.fromOrthodoxWiki)
                .put("missing", doc.missing)
                .put("at", doc.at)
            f.writeText(o.toString())
        }
    }

    suspend fun downloaded(): Int = withContext(Dispatchers.IO) {
        dir?.listFiles { f -> f.name.endsWith(".json") }?.size ?: 0
    }

    suspend fun cacheBytes(): Long = withContext(Dispatchers.IO) {
        (dir?.listFiles() ?: emptyArray()).sumOf { it.length() }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            diskLock.withLock {
                dir?.listFiles()?.forEach { it.delete() }
                mem.clear()
                thumbs.clear()
            }
        }
    }

    /* ---- http ------------------------------------------------------------ */

    private fun get(url: String): String? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        Images.http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.i(TAG, "http " + response.code + " for " + url)
                null
            } else {
                response.body?.string()
            }
        }
    }.getOrNull()

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun api(base: String, query: String): JSONObject? {
        val body = get(base + "?format=json&formatversion=2&" + query) ?: return null
        return runCatching { JSONObject(body) }.getOrNull()
    }

    private fun firstPage(o: JSONObject?): JSONObject? {
        val pages = o?.optJSONObject("query")?.optJSONArray("pages") ?: return null
        if (pages.length() == 0) return null
        return pages.optJSONObject(0)
    }

    /* ---- text ------------------------------------------------------------ */

    private val APPARATUS = listOf(
        "references", "external links", "see also", "notes", "sources",
        "further reading", "bibliography", "succession box", "navigation",
    )

    private fun tidy(raw: String): String {
        val lines = raw.replace("\r\n", "\n").split("\n").toMutableList()
        var cut = lines.size
        var i = lines.size - 1
        while (i >= 0) {
            val t = lines[i].trim()
            val head = t.startsWith("==") && t.endsWith("==")
            if (head) {
                val name = t.trim('=', ' ').lowercase()
                if (APPARATUS.any { name == it || name.startsWith(it) }) {
                    cut = i
                    i--
                    continue
                }
                break
            }
            i--
        }
        return lines.subList(0, cut).joinToString("\n").trim()
    }

    private fun extractOf(base: String, title: String): String? {
        val o = api(
            base,
            "action=query&prop=extracts&explaintext=1&redirects=1&titles=" + enc(title),
        )
        val text = firstPage(o)?.optString("extract").orEmpty()
        return text.ifBlank { null }
    }

    private fun wikitextOf(base: String, title: String): String? {
        val o = api(
            base,
            "action=query&prop=revisions&rvprop=content&rvslots=main&redirects=1&titles=" + enc(title),
        )
        val rev = firstPage(o)?.optJSONArray("revisions")?.optJSONObject(0)
        val raw = rev?.optJSONObject("slots")?.optJSONObject("main")?.optString("content").orEmpty()
        if (raw.isBlank()) return null
        val cleaned = raw
            .replace(Regex("(?s)\\{\\{.*?\\}\\}"), "")
            .replace(Regex("(?s)<ref[^>]*>.*?</ref>"), "")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\[\\[(?:[^\\]|]*\\|)?([^\\]]+)\\]\\]"), "$1")
            .replace(Regex("'''?"), "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        return cleaned.ifBlank { null }
    }

    /* ---- pictures --------------------------------------------------------- */

    private val JUNK = listOf(
        "logo", "icon.svg", "edit", "button", "flag", "map", "seal", "coat",
        "disambig", "commons", "wiki", "stub", "padlock", "question", "emblem",
        "ambox", "crystal", "nuvola", "symbol",
    )

    private fun usableFile(name: String): Boolean {
        val n = name.lowercase()
        if (!(n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") ||
                n.endsWith(".webp") || n.endsWith(".gif"))
        ) return false
        return JUNK.none { n.contains(it) }
    }

    private fun pageImage(base: String, title: String): Pair<String, String>? {
        val o = api(
            base,
            "action=query&prop=pageimages&piprop=original|thumbnail&pithumbsize=" + THUMB +
                "&redirects=1&titles=" + enc(title),
        )
        val page = firstPage(o) ?: return null
        val original = page.optJSONObject("original")?.optString("source").orEmpty()
        val thumb = page.optJSONObject("thumbnail")?.optString("source").orEmpty()
        val full = original.ifBlank { thumb }
        val small = thumb.ifBlank { original }
        return if (full.isBlank()) null else Pair(small, full)
    }

    private fun embeddedImage(base: String, title: String): Pair<String, String>? {
        val o = api(base, "action=query&prop=images&imlimit=40&redirects=1&titles=" + enc(title))
        val arr = firstPage(o)?.optJSONArray("images") ?: return null
        for (i in 0 until arr.length()) {
            val name = arr.optJSONObject(i)?.optString("title").orEmpty()
            if (name.isBlank() || !usableFile(name)) continue
            val info = fileUrls(base, name)
            if (info != null) return info
        }
        return null
    }

    private fun fileUrls(base: String, fileTitle: String): Pair<String, String>? {
        val o = api(
            base,
            "action=query&prop=imageinfo&iiprop=url|size&iiurlwidth=" + THUMB +
                "&titles=" + enc(fileTitle),
        )
        val info = firstPage(o)?.optJSONArray("imageinfo")?.optJSONObject(0) ?: return null
        if (info.optInt("size", 0) in 1 until 8000) return null
        val full = info.optString("url").orEmpty()
        val thumb = info.optString("thumburl").ifBlank { full }
        return if (full.isBlank()) null else Pair(thumb, full)
    }

    private fun commonsImage(name: String): Pair<String, String>? {
        val o = api(
            COMMONS_API,
            "action=query&list=search&srnamespace=6&srlimit=8&srsearch=" + enc(name),
        )
        val arr = o?.optJSONObject("query")?.optJSONArray("search") ?: return null
        for (i in 0 until arr.length()) {
            val t = arr.optJSONObject(i)?.optString("title").orEmpty()
            if (t.isBlank() || !usableFile(t)) continue
            val info = fileUrls(COMMONS_API, t)
            if (info != null) return info
        }
        return null
    }

    private fun findPicture(saint: Saint): Pair<String, String>? {
        val ow = saint.owTitle
        val wp = saint.wikiTitle
        if (ow.isNotBlank()) {
            pageImage(OW_API, ow)?.let { Log.i(TAG, saint.id + ": orthodoxwiki pageimage"); return it }
            embeddedImage(OW_API, ow)?.let { Log.i(TAG, saint.id + ": orthodoxwiki file"); return it }
        }
        if (wp.isNotBlank()) {
            pageImage(WP_API, wp)?.let { Log.i(TAG, saint.id + ": wikipedia pageimage"); return it }
            embeddedImage(WP_API, wp)?.let { Log.i(TAG, saint.id + ": wikipedia file"); return it }
        }
        commonsImage(saint.name)?.let { Log.i(TAG, saint.id + ": commons search"); return it }
        Log.i(TAG, saint.id + ": no picture found (ow=" + ow + " wp=" + wp + ")")
        return null
    }

    /* ---- the life --------------------------------------------------------- */

    private fun bodyFor(saint: Saint): Triple<String, Boolean, String> {
        val ow = saint.owTitle
        val wp = saint.wikiTitle

        var fromOw = ""
        if (ow.isNotBlank()) {
            fromOw = (extractOf(OW_API, ow) ?: "").let {
                if (it.length < 120) wikitextOf(OW_API, ow) ?: it else it
            }
        }
        var fromWp = ""
        if (wp.isNotBlank()) {
            fromWp = (extractOf(WP_API, wp) ?: "").let {
                if (it.length < 120) wikitextOf(WP_API, wp) ?: it else it
            }
        }

        val useWp = fromOw.isBlank() || fromWp.length > (fromOw.length * 3) / 2
        val chosen = if (useWp) fromWp else fromOw
        val host = if (useWp) "https://en.wikipedia.org/wiki/" else "https://orthodoxwiki.org/"
        val link = host + enc(if (useWp) wp else ow)
        return Triple(tidy(chosen), !useWp, if (chosen.isBlank()) "" else link)
    }

    suspend fun doc(saint: Saint, force: Boolean = false): Doc? = withContext(Dispatchers.IO) {
        val id = saint.id
        val old = cached(id)

        val goodText = (old?.full?.length ?: 0) >= 600
        val goodPicture = !old?.image.isNullOrBlank()
        if (!force && old != null && goodText && goodPicture) return@withContext old

        val body: String
        val fromOw: Boolean
        val url: String
        if (force || !goodText || old == null) {
            val t = bodyFor(saint)
            body = t.first
            fromOw = t.second
            url = t.third
        } else {
            body = old.full
            fromOw = old.fromOrthodoxWiki
            url = old.wikiUrl
        }

        val picture: Pair<String, String>? =
            if (force || !goodPicture || old == null) findPicture(saint)
            else Pair(old.image, old.imageFull)

        val text = if (body.isNotBlank()) body else old?.full.orEmpty()
        val intro = text.split("\n").firstOrNull { it.trim().length > 40 }?.trim().orEmpty()

        val fresh = Doc(
            id = id,
            title = saint.display,
            intro = intro,
            full = text,
            image = picture?.first.orEmpty(),
            imageFull = picture?.second.orEmpty(),
            wikiUrl = url.ifBlank { old?.wikiUrl.orEmpty() },
            fromOrthodoxWiki = fromOw,
            missing = text.isBlank(),
            at = System.currentTimeMillis(),
        )
        save(fresh)
        fresh
    }

    /* Twelve in flight, always, with no batch barrier. */
    suspend fun syncAll(saints: List<Saint>, onProgress: (Int, Int) -> Unit) {
        val total = saints.size
        if (total == 0) {
            onProgress(0, 0)
            return
        }
        val done = AtomicInteger(0)
        val gate = Semaphore(PARALLEL)
        onProgress(0, total)
        withContext(Dispatchers.IO) {
            coroutineScope {
                saints.map { s ->
                    async {
                        gate.withPermit { runCatching { doc(s) } }
                        onProgress(done.incrementAndGet(), total)
                    }
                }.awaitAll()
            }
        }
    }
}