package com.goyimatica.synaxismobile.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.time.LocalDate
import kotlin.random.Random

data class Quote(
    val text: String,
    val by: String,
    val saintId: String?
)

/**
 * The daily saying.
 *
 * It has to feel unpredictable but stay the same all day - open the app at
 * dawn and at midnight and it is the same words. So the date seeds a hash
 * rather than a random number generator. Knuth's multiplicative constant
 * scatters consecutive days across the whole list, which a plain modulo of
 * the day number does not: that would walk the quotes in order, one per day,
 * and you would notice within a week.
 */
object QuotesRepo {

    private val mutex = Mutex()

    @Volatile
    private var quotes: List<Quote> = emptyList()

    val isLoaded: Boolean get() = quotes.isNotEmpty()
    val count: Int get() = quotes.size

    suspend fun load(context: Context) {
        if (isLoaded) return
        mutex.withLock {
            if (isLoaded) return
            quotes = withContext(Dispatchers.IO) {
                val json = context.assets.open("quotes.json")
                    .bufferedReader()
                    .use { it.readText() }
                val arr = JSONArray(json)
                val out = ArrayList<Quote>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val text = o.optString("q", "")
                    val by = o.optString("by", "")
                    if (text.isNotBlank() && by.isNotBlank()) {
                        out.add(Quote(text, by, o.optStringOrNull("id")))
                    }
                }
                out
            }
        }
    }

    fun all(): List<Quote> = quotes

    fun forDay(date: LocalDate): Quote? {
        if (quotes.isEmpty()) return null
        val ordinal = date.year * 10000L + date.monthValue * 100L + date.dayOfMonth
        val scattered = (ordinal * 2654435761L) xor (ordinal shr 7)
        val index = ((scattered % quotes.size).toInt() + quotes.size) % quotes.size
        return quotes[index]
    }

    fun today(): Quote? = forDay(LocalDate.now())

    /** For the refresh gesture on the Today card: never the same one twice. */
    fun another(current: Quote?): Quote? {
        if (quotes.size <= 1) return quotes.firstOrNull()
        var pick = quotes[Random.nextInt(quotes.size)]
        var guard = 0
        while (pick.text == current?.text && guard < 8) {
            pick = quotes[Random.nextInt(quotes.size)]
            guard++
        }
        return pick
    }

    fun by(saintId: String): List<Quote> = quotes.filter { it.saintId == saintId }
}