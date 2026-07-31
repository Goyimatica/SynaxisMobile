package com.goyimatica.synaxismobile.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The whole synaxarion, held in memory. Loaded once from assets on first use;
 * every read after that is a map lookup.
 *
 * In the browser this work was scattered through app.js - filtering the array
 * again on every keystroke, rebuilding the feast index on every calendar day
 * change. Here the indexes are built once, at load.
 */
object SaintsRepo {

    private val mutex = Mutex()

    @Volatile
    private var saints: List<Saint> = emptyList()

    private var byId: Map<String, Saint> = emptyMap()
    private var byFeast: Map<String, List<Saint>> = emptyMap()

    val isLoaded: Boolean get() = saints.isNotEmpty()
    val count: Int get() = saints.size

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
            saints = parsed
            byId = parsed.associateBy { it.id }
            byFeast = parsed
                .filter { it.hasFeast }
                .groupBy { it.feast!! }
        }
    }

    fun all(): List<Saint> = saints

    fun byId(id: String?): Saint? = if (id == null) null else byId[id]

    /** Everyone commemorated on a church-calendar "MM-DD". */
    fun onFeast(churchKey: String): List<Saint> = byFeast[churchKey] ?: emptyList()

    fun eras(): List<String> =
        saints.map { it.era }.filter { it.isNotBlank() }.distinct().sorted()

    fun jurisdictions(): List<String> =
        saints.map { it.jurisdiction }.filter { it.isNotBlank() }.distinct().sorted()

    fun tags(): List<String> =
        saints.flatMap { it.tags }.distinct().sorted()

    /**
     * One pass, all four filters. An empty or null argument means "no opinion",
     * so the Lives screen can pass whatever the user has actually chosen.
     */
    fun filter(
        query: String? = null,
        era: String? = null,
        jurisdiction: String? = null,
        tag: String? = null
    ): List<Saint> {
        val q = query?.trim()?.lowercase().orEmpty()
        if (q.isEmpty() && era.isNullOrBlank() &&
            jurisdiction.isNullOrBlank() && tag.isNullOrBlank()
        ) return saints

        return saints.filter { s ->
            (era.isNullOrBlank() || s.era == era) &&
                (jurisdiction.isNullOrBlank() || s.jurisdiction == jurisdiction) &&
                (tag.isNullOrBlank() || s.tags.contains(tag)) &&
                (q.isEmpty() || s.haystack.contains(q))
        }
    }

    /**
     * Search, ranked: a name that starts with the term first, then a name that
     * contains it, then anything else that matches. Same order of preference
     * the web search had, done in one sort rather than three passes.
     */
    fun search(query: String, limit: Int = 60): List<Saint> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return saints
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

    /** Alphabetical, grouped under initials, for the Lives index. */
    fun grouped(list: List<Saint> = saints): List<Pair<Char, List<Saint>>> =
        list.sortedBy { it.name }
            .groupBy { it.initial }
            .toSortedMap()
            .map { (k, v) -> k to v }
}