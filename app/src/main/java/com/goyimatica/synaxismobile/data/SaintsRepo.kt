package com.goyimatica.synaxismobile.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The whole index, held in memory and indexed once at load.
 *
 * V8: the index is no longer only saints. `all()` is everything, because the
 * Lives screen, the search and the startup download all want everything;
 * `onFeast` is only saints, because a feast article is not a person and has
 * no business in "commemorated today".
 */
object SaintsRepo {

    private val mutex = Mutex()

    @Volatile
    private var entries: List<Saint> = emptyList()

    private var byId: Map<String, Saint> = emptyMap()
    private var byFeast: Map<String, List<Saint>> = emptyMap()
    private var byTitle: Map<String, Saint> = emptyMap()

    val isLoaded: Boolean get() = entries.isNotEmpty()
    val count: Int get() = entries.size

    /** Just the people. What the app means when it says "lives". */
    val saintCount: Int get() = entries.count { it.isSaint }

    suspend fun load(context: Context) {
        if (isLoaded) return
        mutex.withLock {
            if (isLoaded) return
            val parsed = withContext(Dispatchers.IO) {
                val json = context.assets.open("saints.json")
                    .bufferedReader()
                    .use { it.readText() }
                Saint.listFrom(json)
            }
            entries = parsed
            byId = parsed.associateBy { it.id }
            byFeast = parsed
                .filter { it.hasFeast && it.isSaint }
                .groupBy { it.feast!! }

            /* Every way an article might be named, pointing at its entry:
               the display name, and the wiki title it was harvested under.
               This is what lets a feast card find something to open. */
            val titles = HashMap<String, Saint>(parsed.size * 2)
            parsed.forEach { s ->
                if (!s.isSaint) {
                    titles[s.name.lowercase()] = s
                    if (s.owTitle.isNotBlank()) titles[s.owTitle.lowercase()] = s
                }
            }
            byTitle = titles
        }
    }

    fun all(): List<Saint> = entries

    fun saints(): List<Saint> = entries.filter { it.isSaint }

    fun subjects(): List<Saint> = entries.filter { !it.isSaint }

    fun byId(id: String?): Saint? = if (id == null) null else byId[id]

    /** Everyone commemorated on a church-calendar "MM-DD". Saints only. */
    fun onFeast(churchKey: String): List<Saint> = byFeast[churchKey] ?: emptyList()

    /**
     * The article behind a feast name, a fast name or a season.
     *
     * Exact match first, then the longest subject whose name is contained in
     * what we were given - so "The Dormition Fast - wine and oil" finds
     * "Dormition Fast", and "Week 9 after Pentecost" finds "Pentecost".
     */
    fun topicFor(text: String?): Saint? {
        if (text.isNullOrBlank()) return null
        val t = text.lowercase()
        byTitle[t]?.let { return it }
        return byTitle.entries
            .filter { it.key.length >= 5 && t.contains(it.key) }
            .maxByOrNull { it.key.length }
            ?.value
    }

    fun eras(): List<String> =
        entries.map { it.era }.filter { it.isNotBlank() }.distinct().sorted()

    fun jurisdictions(): List<String> =
        entries.map { it.jurisdiction }.filter { it.isNotBlank() }.distinct().sorted()

    fun tags(): List<String> =
        entries.flatMap { it.tags }.distinct().sorted()

    fun filter(
        query: String? = null,
        era: String? = null,
        jurisdiction: String? = null,
        tag: String? = null
    ): List<Saint> {
        val q = query?.trim()?.lowercase().orEmpty()
        if (q.isEmpty() && era.isNullOrBlank() &&
            jurisdiction.isNullOrBlank() && tag.isNullOrBlank()
        ) return entries

        return entries.filter { s ->
            (era.isNullOrBlank() || s.era == era) &&
                (jurisdiction.isNullOrBlank() || s.jurisdiction == jurisdiction) &&
                (tag.isNullOrBlank() || s.tags.contains(tag)) &&
                (q.isEmpty() || s.haystack.contains(q))
        }
    }

    fun search(query: String, limit: Int = 60): List<Saint> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return entries
            .mapNotNull { s ->
                val n = s.name.lowercase()
                val rank = when {
                    n.startsWith(q) -> 0
                    n.contains(q) -> 1
                    s.haystack.contains(q) -> 2
                    else -> return@mapNotNull null
                }
                rank to s
            }
            .sortedWith(compareBy({ it.first }, { it.second.name }))
            .take(limit)
            .map { it.second }
    }

    fun grouped(list: List<Saint> = entries): List<Pair<Char, List<Saint>>> =
        list.sortedBy { it.name }
            .groupBy { it.initial }
            .toSortedMap()
            .map { (k, v) -> k to v }
}