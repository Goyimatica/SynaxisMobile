package com.goyimatica.synaxismobile.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** A highlight or a note. Two integers and a colour, exactly as on the web.
 *  `source` is the text the offsets belong to: "ow", "wp", or "" for a
 *  legacy pre-1.0.1 mark made on the preferred telling. */
data class Mark(
    val key: String,
    val start: Int,
    val end: Int,
    val color: String,
    val at: Long,
    val text: String,
    val note: String? = null,
    val source: String = ""
) {
    fun toJson(): JSONObject = JSONObject()
        .put("k", key)
        .put("a", start)
        .put("b", end)
        .put("c", color)
        .put("t", at)
        .put("x", text)
        .put("note", note ?: JSONObject.NULL)
        .put("s", source)

    companion object {
        fun from(o: JSONObject) = Mark(
            key = o.optString("k"),
            start = o.optInt("a", 0),
            end = o.optInt("b", 0),
            color = o.optString("c", "yellow"),
            at = o.optLong("t", 0L),
            text = o.optString("x", ""),
            note = o.optStringOrNull("note"),
            source = o.optString("s", "")
        )

        /* V10: UUID instead of Math.random. Keys are only ever local
           identifiers, but a predictable key is still a needless foot-gun. */
        fun newKey(): String =
            "m" + java.lang.Long.toString(System.currentTimeMillis(), 36) +
                UUID.randomUUID().toString().replace("-", "").take(6)
    }
}

/** A saint you have opened, and how far down you got. */
data class Recent(val id: String, val at: Long, val progress: Float)

data class Library(
    val bookmarks: List<String> = emptyList(),
    val recents: List<Recent> = emptyList(),
    val marks: Map<String, List<Mark>> = emptyMap()
) {
    fun isBookmarked(id: String) = bookmarks.contains(id)
    fun marksFor(id: String): List<Mark> = marks[id] ?: emptyList()
    val markCount: Int get() = marks.values.sumOf { it.size }
    val noteCount: Int get() = marks.values.sumOf { list -> list.count { !it.note.isNullOrBlank() } }
}

object Store {

    private const val PREFS = "synaxis_prefs"
    private const val FILE = "synaxis.json"
    private const val RECENTS_MAX = 40

    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()

    private lateinit var prefs: SharedPreferences
    private lateinit var file: File

    private val _library = MutableStateFlow(Library())
    val library: StateFlow<Library> = _library.asStateFlow()

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    @Volatile
    private var ready = false

    /** Call once, from MainActivity, before anything reads state. */
    suspend fun init(context: Context) {
        if (ready) return
        val app = context.applicationContext
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        file = File(app.filesDir, FILE)
        _settings.value = Settings.read(prefs)
        _library.value = withContext(Dispatchers.IO) { readLibrary() }
        ready = true
    }

    // ---- reading -------------------------------------------------------

    private fun readLibrary(): Library {
        if (!file.exists()) return Library()
        return try {
            val root = JSONObject(file.readText())

            val bookmarks = ArrayList<String>()
            val ba = root.optJSONArray("bookmarks")
            if (ba != null) for (i in 0 until ba.length()) {
                val v = ba.optString(i, "")
                if (v.isNotBlank()) bookmarks.add(v)
            }

            val recents = ArrayList<Recent>()
            val ra = root.optJSONArray("recents")
            if (ra != null) for (i in 0 until ra.length()) {
                val o = ra.optJSONObject(i) ?: continue
                val id = o.optString("id", "")
                if (id.isNotBlank()) {
                    recents.add(
                        Recent(
                            id = id,
                            at = o.optLong("at", 0L),
                            progress = o.optDouble("p", 0.0).toFloat()
                        )
                    )
                }
            }

            val marks = HashMap<String, List<Mark>>()
            val mo = root.optJSONObject("marks")
            if (mo != null) for (id in mo.keys()) {
                val arr = mo.optJSONArray(id) ?: continue
                val list = ArrayList<Mark>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val m = Mark.from(o)
                    if (m.key.isNotBlank() && m.end > m.start) list.add(m)
                }
                if (list.isNotEmpty()) marks[id] = list.sortedBy { it.start }
            }

            Library(bookmarks, recents.sortedByDescending { it.at }, marks)
        } catch (e: Exception) {
            // A corrupt file must never stop the app opening. Start clean and
            // keep the old one beside it, in case the highlights matter.
            runCatching { file.copyTo(File(file.path + ".broken"), overwrite = true) }
            Library()
        }
    }

    // ---- writing -------------------------------------------------------

    private fun persist(state: Library) {
        io.launch {
            writeLock.withLock {
                val root = JSONObject()

                root.put("bookmarks", JSONArray(state.bookmarks))

                val ra = JSONArray()
                state.recents.forEach { r ->
                    ra.put(
                        JSONObject()
                            .put("id", r.id)
                            .put("at", r.at)
                            .put("p", r.progress.toDouble())
                    )
                }
                root.put("recents", ra)

                val mo = JSONObject()
                state.marks.forEach { (id, list) ->
                    val arr = JSONArray()
                    list.forEach { arr.put(it.toJson()) }
                    mo.put(id, arr)
                }
                root.put("marks", mo)
                root.put("v", 1)

                // Write beside, then move. A process death mid-write can lose
                // the newest highlight; it must not lose the whole library.
                val tmp = File(file.path + ".tmp")
                tmp.writeText(root.toString())
                if (!tmp.renameTo(file)) {
                    file.writeText(root.toString())
                    tmp.delete()
                }
            }
        }
    }

    private fun mutate(block: (Library) -> Library) {
        val next = block(_library.value)
        _library.value = next
        persist(next)
    }

    // ---- bookmarks -----------------------------------------------------

    fun toggleBookmark(id: String): Boolean {
        var nowOn = false
        mutate { s ->
            val list = s.bookmarks.toMutableList()
            if (list.remove(id)) {
                nowOn = false
            } else {
                list.add(0, id)
                nowOn = true
            }
            s.copy(bookmarks = list)
        }
        return nowOn
    }

    // ---- recents -------------------------------------------------------

    /** Opened a life. Moves it to the top and keeps the list bounded. */
    fun touch(id: String) = mutate { s ->
        val previous = s.recents.firstOrNull { it.id == id }?.progress ?: 0f
        val list = s.recents.filter { it.id != id }.toMutableList()
        list.add(0, Recent(id, System.currentTimeMillis(), previous))
        s.copy(recents = list.take(RECENTS_MAX))
    }

    fun setProgress(id: String, progress: Float) = mutate { s ->
        val clamped = progress.coerceIn(0f, 1f)
        val list = s.recents.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        if (i >= 0) {
            // Progress only ever moves forward, so closing the reader near the
            // top does not throw away how far you actually read.
            val old = list[i]
            if (clamped > old.progress) list[i] = old.copy(progress = clamped, at = System.currentTimeMillis())
        } else {
            list.add(0, Recent(id, System.currentTimeMillis(), clamped))
        }
        s.copy(recents = list)
    }

    fun clearRecents() = mutate { it.copy(recents = emptyList()) }

    // ---- marks ---------------------------------------------------------

    fun addMark(saintId: String, mark: Mark) = mutate { s ->
        val list = (s.marks[saintId] ?: emptyList())
            /* A new highlight swallows anything it fully covers on the SAME
               source, so dragging over three short highlights leaves one,
               not four overlapping. Marks on the other telling must never
               be eaten: the same offsets mean different words there. */
            .filterNot {
                it.source == mark.source &&
                    it.start >= mark.start && it.end <= mark.end
            }
            .plus(mark)
            .sortedBy { it.start }
        s.copy(marks = s.marks + (saintId to list))
    }

    fun dropMark(saintId: String, key: String) = mutate { s ->
        val list = (s.marks[saintId] ?: emptyList()).filterNot { it.key == key }
        s.copy(
            marks = if (list.isEmpty()) s.marks - saintId else s.marks + (saintId to list)
        )
    }

    fun editMark(saintId: String, key: String, color: String? = null, note: String? = null) =
        mutate { s ->
            val list = (s.marks[saintId] ?: emptyList()).map { m ->
                if (m.key != key) m
                else m.copy(
                    color = color ?: m.color,
                    note = if (note == null) m.note else note.ifBlank { null }
                )
            }
            s.copy(marks = s.marks + (saintId to list))
        }

    fun clearMarks(saintId: String) = mutate { it.copy(marks = it.marks - saintId) }

    /** Everything highlighted, newest first, for the Library screen. */
    fun allMarks(): List<Pair<String, Mark>> =
        _library.value.marks
            .flatMap { (id, list) -> list.map { id to it } }
            .sortedByDescending { it.second.at }

    // ---- settings ------------------------------------------------------

    fun update(block: (Settings) -> Settings) {
        val next = block(_settings.value)
        _settings.value = next
        next.write(prefs)
    }

    fun eraseEverything() {
        _library.value = Library()
        io.launch { writeLock.withLock { file.delete() } }
    }
}