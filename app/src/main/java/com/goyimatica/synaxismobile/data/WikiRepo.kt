package com.goyimatica.synaxismobile.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CancellationException
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

/*
 * A life as kept on this phone.
 *
 * V10: two sources. `full` is whichever article read better and is what
 * earlier versions stored; `fullOw` and `fullWp` are the OrthodoxWiki and
 * Wikipedia texts themselves, so the reader can offer either one offline.
 * `both` says the two-source pass has run for this entry - a blank source
 * then means that wiki genuinely has nothing on the subject, not that the
 * entry was fetched by an older build.
 */
data class Doc(
    val id: String,
    val title: String,
    val intro: String,
    val full: String,
    val fullOw: String,
    val fullWp: String,
    val image: String,
    val imageFull: String,
    val wikiUrl: String,
    val fromOrthodoxWiki: Boolean,
    val missing: Boolean,
    val at: Long,
    val both: Boolean = true,
)

object WikiRepo {

    const val OW_API = "https://orthodoxwiki.org/api.php"
    const val WP_API = "https://en.wikipedia.org/w/api.php"
    const val COMMONS_API = "https://commons.wikimedia.org/w/api.php"
    const val AGENT = Images.AGENT

    private const val THUMB = 1600
    /* Twelve kept Wikimedia happy and the phone cool; the client below can
       hold far more, so a whole library sync goes faster without touching
       the two host limits per domain. */
    private const val PARALLEL = 32
    private const val WEEK = 7L * 24L * 60L * 60L * 1000L

    /* V10: fetched from filesDir, not cacheDir. The OS may clear cacheDir
       whenever it pleases; these lives are meant to stay on the phone. */
    private var dir: File? = null

    private val mem = ConcurrentHashMap<String, Doc>(64)
    private val diskLock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /* V9.1: entry id -> the Wikipedia title we found for it, or "" if there
       is none. Held for the session so a subject is never searched twice. */
    private val resolvedWp = ConcurrentHashMap<String, String>()

    /** saint id -> picture URL, readable from any composable for free. */
    val thumbs = mutableStateMapOf<String, String>()

    fun init(context: Context) {
        if (dir != null) return
        dir = File(context.filesDir, "docs").apply { mkdirs() }
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
                fullOw = o.optString("fullOw"),
                fullWp = o.optString("fullWp"),
                image = o.optString("image"),
                imageFull = o.optString("imageFull"),
                wikiUrl = o.optString("wikiUrl"),
                fromOrthodoxWiki = o.optBoolean("fromOrthodoxWiki"),
                missing = o.optBoolean("missing"),
                at = o.optLong("at"),
                /* Old cache files have no `both` key; they must be fetched
                   once more to fill in the second source, so the default is
                   false, not true. */
                both = o.optBoolean("both", false),
            )
        }.getOrNull()?.also {
            mem[id] = it
            if (it.image.isNotBlank()) thumbs[id] = it.image
        }
    }

    /**
     * Which entries the startup sync actually has to fetch.
     *
     * An entry with no picture anywhere would otherwise be hunted again on
     * every single launch, so a fruitless search is repeated once a week.
     * A cache written by a pre-V10 build has no `both` flag, so it is fetched
     * once more to fill in the second source.
     */
    suspend fun pending(saints: List<Saint>): List<Saint> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        saints.filter { s ->
            val d = cached(s.id)
            when {
                d == null -> true
                !d.both -> true
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
                .put("fullOw", doc.fullOw)
                .put("fullWp", doc.fullWp)
                .put("image", doc.image)
                .put("imageFull", doc.imageFull)
                .put("wikiUrl", doc.wikiUrl)
                .put("fromOrthodoxWiki", doc.fromOrthodoxWiki)
                .put("missing", doc.missing)
                .put("at", doc.at)
                .put("both", doc.both)
            f.writeText(o.toString())
        }
    }

    suspend fun downloaded(): Int = withContext(Dispatchers.IO) {
        dir?.listFiles { f -> f.name.endsWith(".json") }?.size ?: 0
    }

    suspend fun cacheBytes(): Long = withContext(Dispatchers.IO) {
        (dir?.listFiles() ?: emptyArray()).sumOf { it.length() }
    }

    /**
     * V11: roughly what the missing lives will cost, so the choice dialog can
     * say how much data is coming before a single byte is fetched. Measured
     * from the lives already on the phone; a generous default when none are.
     */
    suspend fun estimateBytes(missing: List<Saint>): Long = withContext(Dispatchers.IO) {
        val files = dir?.listFiles { f -> f.name.endsWith(".json") } ?: return@withContext 0L
        val avg = if (files.isEmpty()) 0L else files.sumOf { it.length() } / files.size
        val per = if (avg > 0L) avg else 700_000L
        per * missing.size
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            diskLock.withLock {
                dir?.listFiles()?.forEach { it.delete() }
                mem.clear()
                thumbs.clear()
                resolvedWp.clear()
            }
        }
    }

    /* ---- http ------------------------------------------------------------ */

    /*
     * One polite, resilient caller.
     *
     * Wikimedia throttles with 429 and breaks with 5xx; both are worth one
     * or two retries with a short pause, because a single refused request
     * used to cost a saint its picture for the whole session. Nothing else
     * is retried. The URL is never logged - it is a record of what you read.
     */
    private fun get(url: String): String? {
        var attempt = 0
        while (attempt < 3) {
            attempt++
            var retryable = false
            val result = try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build()
                Images.http.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()
                    } else {
                        retryable = response.code == 429 || response.code in 500..599
                        null
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                retryable = true
                null
            }
            if (result != null) return result
            if (!retryable || attempt >= 3) return null
            Thread.sleep(400L * attempt)
        }
        return null
    }

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

    /* ---- is this the right article --------------------------------------- */

    /*
     * The rule that keeps a 1940 film poster off the All-Night Vigil.
     *
     * Any title a search hands back has to carry at least one substantial
     * word of the name we searched for. Short words are ignored, because
     * "of" and "the" would match everything; if a name has no long word at
     * all the check passes, because there is nothing left to test it with.
     */
    private fun bigWords(s: String): List<String> =
        s.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 3 }

    private fun related(title: String, name: String): Boolean {
        val words = bigWords(name)
        if (words.isEmpty()) return true
        val t = title.lowercase()
        return words.any { t.contains(it) }
    }

    private fun searchTitle(base: String, query: String): String? {
        val o = api(
            base,
            "action=query&list=search&srnamespace=0&srlimit=1&srsearch=" + enc(query),
        )
        val arr = o?.optJSONObject("query")?.optJSONArray("search") ?: return null
        if (arr.length() == 0) return null
        val t = arr.optJSONObject(0)?.optString("title").orEmpty()
        return t.ifBlank { null }
    }

    /**
     * The Wikipedia article for an entry that was never given one.
     *
     * Every harvested saint and every subject has an empty `w`, which under
     * V8 meant there was no second place to look when OrthodoxWiki came up
     * short - and that is exactly why "Desert Fathers" opened with nothing
     * in it. One search, checked against the name, remembered for the run.
     */
    private fun wpTitleFor(saint: Saint): String {
        if (saint.wikiTitle.isNotBlank()) return saint.wikiTitle
        resolvedWp[saint.id]?.let { return it }

        val first = if (saint.isSaint) saint.display else saint.name + " Eastern Orthodox"
        val found = searchTitle(WP_API, first) ?: searchTitle(WP_API, saint.name)
        val out = if (found != null && related(found, saint.name)) found else ""
        resolvedWp[saint.id] = out
        return out
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

    /** The article as prose: the plain extract, or the wikitext if it is thin. */
    private fun pageText(base: String, title: String): String {
        if (title.isBlank()) return ""
        val extract = extractOf(base, title).orEmpty()
        if (extract.length >= 120) return extract
        return wikitextOf(base, title) ?: extract
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

    /* V11: a person is not a place. For a saint, refuse pictures whose file
       names say church, monastery, tomb or landscape - those are the random
       buildings that used to stand in for a life. */
    private val PLACES = listOf(
        "church", "cathedral", "monastery", "abbey", "chapel", "basilica",
        "tomb", "grave", "monument", "memorial", "plaque", "window", "stained",
        "facade", "interior", "exterior", "aerial", "panorama", "landscape",
        "building", "house", "village", "city", "street", "square", "tower",
        "bell", "gate", "wall", "cemetery", "reliquary", "sarcophagus", "crypt",
        "nave", "apse", "iconostasis", "mural", "ceiling", "dome", "floor",
    )

    private fun usableForPerson(name: String): Boolean {
        val n = name.lowercase()
        if (!usableFile(name)) return false
        if (PLACES.any { n.contains(it) }) return false
        return true
    }

    private fun pageImage(base: String, title: String, person: Boolean = false): Pair<String, String>? {
        if (title.isBlank()) return null
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
        if (full.isBlank()) return null
        if (person && !usableForPerson(full.substringAfterLast('/'))) return null
        return Pair(small, full)
    }

    private fun embeddedImage(base: String, title: String, person: Boolean = false): Pair<String, String>? {
        if (title.isBlank()) return null
        val o = api(base, "action=query&prop=images&imlimit=40&redirects=1&titles=" + enc(title))
        val arr = firstPage(o)?.optJSONArray("images") ?: return null
        var best: Pair<String, String>? = null
        for (i in 0 until arr.length()) {
            val name = arr.optJSONObject(i)?.optString("title").orEmpty()
            if (name.isBlank() || !usableFile(name)) continue
            if (person && !usableForPerson(name)) continue
            val info = fileUrls(base, name) ?: continue
            /* for a person, an icon-named file wins over a plain one */
            if (person && name.lowercase().contains("icon")) return info
            if (best == null) best = info
        }
        return best
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

    /*
     * V9.1: Commons, but only a file that admits whose it is.
     *
     * This is the search that produced a film poster for the All-Night Vigil.
     * It now demands that the file name itself carry a word of the name we
     * are looking for, and the caller only reaches it for a person.
     */
    /*
     * V11: an actual icon of a person, searched for by name. Commons is full
     * of "Icon of St X" files; when one exists it is the right picture, and
     * it beats any lead image, which for a saint is all too often a photo of
     * their church or their tomb.
     */
    private fun iconImage(name: String): Pair<String, String>? {
        if (name.isBlank()) return null
        val o = api(
            COMMONS_API,
            "action=query&list=search&srnamespace=6&srlimit=8&srsearch=" + enc("icon of " + name),
        )
        val arr = o?.optJSONObject("query")?.optJSONArray("search") ?: return null
        for (i in 0 until arr.length()) {
            val t = arr.optJSONObject(i)?.optString("title").orEmpty()
            if (t.isBlank() || !usableForPerson(t)) continue
            if (!t.lowercase().contains("icon")) continue
            if (!related(t, name)) continue
            val info = fileUrls(COMMONS_API, t)
            if (info != null) return info
        }
        return null
    }

    private fun commonsImage(name: String): Pair<String, String>? {
        val o = api(
            COMMONS_API,
            "action=query&list=search&srnamespace=6&srlimit=8&srsearch=" + enc(name),
        )
        val arr = o?.optJSONObject("query")?.optJSONArray("search") ?: return null
        var best: Pair<String, String>? = null
        for (i in 0 until arr.length()) {
            val t = arr.optJSONObject(i)?.optString("title").orEmpty()
            if (t.isBlank() || !usableForPerson(t)) continue
            if (!related(t, name)) continue
            val info = fileUrls(COMMONS_API, t) ?: continue
            if (t.lowercase().contains("icon")) return info
            if (best == null) best = info
        }
        return best
    }

    private fun findPicture(saint: Saint): Pair<String, String>? {
        val person = saint.isSaint
        val ow = saint.owTitle.ifBlank { saint.name }

        if (person) {
            iconImage(saint.name)?.let { return it }
        }

        pageImage(OW_API, ow, person)?.let { return it }
        embeddedImage(OW_API, ow, person)?.let { return it }

        val wp = wpTitleFor(saint)
        if (wp.isNotBlank()) {
            pageImage(WP_API, wp, person)?.let { return it }
            embeddedImage(WP_API, wp, person)?.let { return it }
        }

        /* A blind search of Commons is a last resort for a person and for
           nobody else. A subject either has the picture on its own article
           or it goes without one, which is far better than a wrong one. */
        if (person) {
            commonsImage(saint.name)?.let { return it }
        }

        return null
    }

    /* ---- the article ------------------------------------------------------ */

    /**
     * Both articles, cleaned, as a pair of (orthodoxWiki, wikipedia) texts.
     * A blank side is honest: that wiki has nothing on this subject.
     */
    private fun bodyFor(saint: Saint): Pair<String, String> {
        val ow = saint.owTitle.ifBlank { saint.name }
        val fromOw = tidy(pageText(OW_API, ow))

        var wp = saint.wikiTitle
        var fromWp = ""
        if (wp.isBlank()) wp = wpTitleFor(saint)
        fromWp = tidy(pageText(WP_API, wp))

        return Pair(fromOw, fromWp)
    }

    /**
     * The fuller of the two, as before V10. Wikipedia wins only when it is
     * clearly richer, because for a life the OrthodoxWiki telling is usually
     * the better one; the reader can still switch to the other at will.
     */
    private fun prefer(ow: String, wp: String): Boolean {
        if (ow.isBlank()) return true
        if (wp.isBlank()) return false
        return wp.length > (ow.length * 3) / 2
    }

    suspend fun doc(saint: Saint, force: Boolean = false): Doc? = withContext(Dispatchers.IO) {
        val id = saint.id
        val old = cached(id)

        val goodText = (old?.full?.length ?: 0) >= 600
        val goodPicture = !old?.image.isNullOrBlank()
        if (!force && old != null && goodText && goodPicture && old.both) return@withContext old

        val ow: String
        val wp: String
        val url: String
        if (force || old == null || !goodText || !old.both) {
            val sources = bodyFor(saint)
            ow = sources.first
            wp = sources.second
            val useWp = prefer(ow, wp)
            val host = if (useWp) "https://en.wikipedia.org/wiki/" else "https://orthodoxwiki.org/"
            val title = if (useWp) wpTitleFor(saint) else saint.owTitle.ifBlank { saint.name }
            url = if ((if (useWp) wp else ow).isBlank()) "" else host + enc(title)
        } else {
            /* Good text, missing picture: keep the words, hunt only the icon. */
            ow = old.fullOw
            wp = old.fullWp
            url = old.wikiUrl
        }

        val picture: Pair<String, String>? =
            if (force || !goodPicture || old == null) findPicture(saint)
            else Pair(old.image, old.imageFull)

        val useWp = prefer(ow, wp)
        val text = (if (useWp) wp else ow).ifBlank { old?.full.orEmpty() }
        val intro = text.split("\n").firstOrNull { it.trim().length > 40 }?.trim().orEmpty()

        val fresh = Doc(
            id = id,
            title = saint.display,
            intro = intro,
            full = text,
            fullOw = ow,
            fullWp = wp,
            image = picture?.first.orEmpty(),
            imageFull = picture?.second.orEmpty(),
            wikiUrl = url.ifBlank { old?.wikiUrl.orEmpty() },
            fromOrthodoxWiki = !useWp,
            missing = text.isBlank(),
            at = System.currentTimeMillis(),
            both = true,
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
