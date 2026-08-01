package com.goyimatica.synaxismobile.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * One entry in the index.
 *
 *   n   -> name          "Seraphim of Sarov"
 *   e   -> epithet       "Wonderworker of All Russia"
 *   f   -> feast         "01-02" on the CHURCH calendar, or null
 *   fl  -> feastLabel    words for a movable commemoration
 *   era -> era           "Patristic", "Modern", "The fasts", ...
 *   j   -> jurisdiction  "Russian", "Greek", "ROCOR", ...
 *   w   -> wikiTitle     exact Wikipedia article title
 *   o   -> owTitle       exact OrthodoxWiki article title
 *   b   -> tags
 *   c   -> century
 *   k   -> kind          "saint", "feast", "fast" or "topic"
 *
 * V8: `kind`. A feast, a fast and a subject are fetched, cached, read,
 * highlighted and bookmarked by exactly the same machinery as a life - the
 * only thing the field changes is how the app talks about the entry and
 * where it is allowed to appear.
 */
data class Saint(
    val id: String,
    val name: String,
    val epithet: String,
    val feast: String?,
    val feastLabel: String?,
    val era: String,
    val jurisdiction: String,
    val wikiTitle: String,
    val owTitle: String,
    val tags: List<String>,
    val century: String?,
    val note: String?,
    val pending: Boolean,
    val kind: String = KIND_SAINT,
) {

    /** "Seraphim of Sarov, Wonderworker of All Russia" */
    val display: String
        get() = if (epithet.isBlank()) name else "$name, $epithet"

    val hasFeast: Boolean get() = !feast.isNullOrBlank()

    val isSaint: Boolean get() = kind == KIND_SAINT

    /** "Life", "Feast", "Fast", "Subject" - what to call this thing. */
    val kindWord: String
        get() = when (kind) {
            KIND_FEAST -> "Feast"
            KIND_FAST -> "Fast"
            KIND_TOPIC -> "Subject"
            else -> "Life"
        }

    fun feastText(): String = when {
        !feastLabel.isNullOrBlank() -> feastLabel
        !feast.isNullOrBlank() -> monthDay(feast)
        else -> ""
    }

    val initial: Char
        get() = name.firstOrNull { it.isLetter() }?.uppercaseChar() ?: '\u00b7'

    internal val haystack: String =
        (name + ' ' + epithet + ' ' + era + ' ' + jurisdiction + ' ' +
            tags.joinToString(" ") + ' ' + (century ?: "")).lowercase()

    companion object {

        const val KIND_SAINT = "saint"
        const val KIND_FEAST = "feast"
        const val KIND_FAST = "fast"
        const val KIND_TOPIC = "topic"

        private val MONTHS = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        /** "01-02" -> "2 January" */
        fun monthDay(key: String): String {
            val parts = key.split("-")
            if (parts.size != 2) return key
            val m = parts[0].toIntOrNull() ?: return key
            val d = parts[1].toIntOrNull() ?: return key
            if (m < 1 || m > 12) return key
            return "$d ${MONTHS[m - 1]}"
        }

        fun from(o: JSONObject): Saint = Saint(
            id = o.optString("id"),
            name = o.optString("n"),
            epithet = o.optString("e", ""),
            feast = o.optStringOrNull("f"),
            feastLabel = o.optStringOrNull("fl"),
            era = o.optString("era", ""),
            jurisdiction = o.optString("j", ""),
            wikiTitle = o.optString("w", ""),
            owTitle = o.optString("o", ""),
            tags = o.optJSONArray("b").toStringList(),
            century = o.optStringOrNull("c"),
            note = o.optStringOrNull("note"),
            pending = o.optBoolean("pending", false),
            kind = o.optStringOrNull("k") ?: KIND_SAINT,
        )

        fun listFrom(json: String): List<Saint> {
            val arr = JSONArray(json)
            val out = ArrayList<Saint>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val s = from(o)
                if (s.id.isNotBlank() && s.name.isNotBlank()) out.add(s)
            }
            return out
        }
    }
}

/** org.json turns a JSON null into the string "null"; this does not. */
internal fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val v = optString(key, "")
    return if (v.isBlank()) null else v
}

internal fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    val out = ArrayList<String>(length())
    for (i in 0 until length()) {
        val v = optString(i, "")
        if (v.isNotBlank()) out.add(v)
    }
    return out
}