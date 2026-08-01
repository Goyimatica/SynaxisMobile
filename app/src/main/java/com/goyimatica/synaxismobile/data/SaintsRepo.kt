package com.goyimatica.synaxismobile.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The whole index, held in memory and indexed once at load.
 *
 * V9.1 - two lists, and the safe one is the default.
 *
 *   all()        the people. Lives, the calendar, the library, the filters.
 *   everything() the people and the subjects together. Two callers only:
 *                Search, which should find a feast if you type its name,
 *                and the startup sync, which must download the subjects
 *                or they would open blank.
 *
 * Written this way round on purpose. A screen added next month that calls
 * all() without thinking gets the correct answer; under V8 it would have
 * silently listed the Akathist among the martyrs.
 */
object SaintsRepo {

    private val mutex = Mutex()

    @Volatile
    private var entries: List<Saint> = emptyList()

    @Volatile
    private var people: List<Saint> = emptyList()

    private var byId: Map<String, Saint> = emptyMap()
    private var byFeast: Map<String, List<Saint>> = emptyMap()
    private var byTitle: Map<String, Saint> = emptyMap()

    val isLoaded: Boolean get() = entries.isNotEmpty()

    /** Everything in the file, subjects included. */
    val count: Int get() = entries.size

    /** Just the people. What the app means when it says "lives". */
    val saintCount: Int get() = people.size

    val subjectCount: Int get() = entries.size - people.size

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
            people = parsed.filter { it.isSaint }
            byId = parsed.associateBy { it.id }
            byFeast = people
                .filter { it.hasFeast }
                .groupBy { it.feast!! }

            /* Every way a subject might be named, pointing at its entry: the
               display name and the wiki title it was harvested under. This is
               what lets a feast card on Today find something to open. */
            val titles = HashMap<String, Saint>(parsed.size)
            parsed.forEach { s ->
                if (!s.isSaint) {
                    titles[s.name.lowercase()] = s
                    if (s.owTitle.isNotBlank()) titles[s.owTitle.lowercase()] = s
                }
            }
            byTitle = titles
        }
    }

    /** The people. The default answer, because it is the safe one. */
    fun all(): List<Saint> = people

    /** The people and the subjects. Search and the startup sync only. */
    fun everything(): List<Saint> = entries

    fun saints(): List<Saint> = people

    fun subjects(): List<Saint> = entries.filter { !it.isSaint }

    fun byId(id: String?): Saint? = if (id == null) null else byId[id]

    /** Everyone commemorated on a church-calendar "MM-DD". People only. */
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

    /* The filter lists are drawn from the people, so Lives never offers you
       an era called "The faith". */

    fun eras(): List<String> =
        people.map { it.era }.filter { it.isNotBlank() }.distinct().sorted()

    fun jurisdictions(): List<String> =
        people.map { it.jurisdiction }.filter { it.isNotBlank() }.distinct().sorted()

    fun tags(): List<String> =
        people.flatMap { it.tags }.distinct().sorted()

    private fun List<Saint>.sift(
        query: String?,
        era: String?,
        jurisdiction: String?,
        tag: String?
    ): List<Saint> {
        val q = query?.trim()?.lowercase().orEmpty()
        if (q.isEmpty() && era.isNullOrBlank() &&
            jurisdiction.isNullOrBlank() && tag.isNullOrBlank()
        ) return this

        return this.filter { s ->
            (era.isNullOrBlank() || s.era == era) &&
                (jurisdiction.isNullOrBlank() || s.jurisdiction == jurisdiction) &&
                (tag.isNullOrBlank() || s.tags.contains(tag)) &&
                (q.isEmpty() || s.haystack.contains(q))
        }
    }

    /** Lives. People only. */
    fun filter(
        query: String? = null,
        era: String? = null,
        jurisdiction: String? = null,
        tag: String? = null
    ): List<Saint> = people.sift(query, era, jurisdiction, tag)

    /** Search. Subjects included. */
    fun filterAll(
        query: String? = null,
        era: String? = null,
        jurisdiction: String? = null,
        tag: String? = null
    ): List<Saint> = entries.sift(query, era, jurisdiction, tag)

    /**
     * Search, over everything.
     *
     * Type "dormition" and you should be offered the feast; type "seraphim"
     * and a person should come first. So the sort is: how well the name
     * matched, then people before subjects, then alphabetical.
     */
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
            .sortedWith(
                compareBy(
                    { it.first },
                    { if (it.second.isSaint) 0 else 1 },
                    { it.second.name },
                )
            )
            .take(limit)
            .map { it.second }
    }

    fun grouped(list: List<Saint> = people): List<Pair<Char, List<Saint>>> =
        list.sortedBy { it.name }
            .groupBy { it.initial }
            .toSortedMap()
            .map { (k, v) -> k to v }
}