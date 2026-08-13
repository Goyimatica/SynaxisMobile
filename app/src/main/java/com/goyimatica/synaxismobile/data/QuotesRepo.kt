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
 * V14: the quote follows the reckoning. Today is keyed to the *church* date
 * - the day the user's chosen calendar (Julian or Revised Julian) calls it -
 * so the same civil day shows the quote of its church day, and the quote
 * never changes mid-day no matter how often the app is opened.
 *
 * There is one quote per slot of the year, and every (month, day) pair maps
 * to a fixed slot (1..366, February 29 included), so every day of the
 * church year has its own saying and the same date always shows the same
 * words.
 */
object QuotesRepo {

    private val mutex = Mutex()

    @Volatile
    private var quotes: List<Quote> = emptyList()

    val isLoaded: Boolean get() = quotes.isNotEmpty()
    val count: Int get() = quotes.size

    /* Days before the first of each month, counting February as always
       29 days, so every real date - February 29 included - owns a slot. */
    private val MONTH_PREFIX = intArrayOf(0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335)

    /** 1..366 for any (month, day), stable across leap and common years. */
    private fun slotOf(church: LocalDate): Int =
        MONTH_PREFIX[church.monthValue - 1] + church.dayOfMonth

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

    /** The quote for a church-calendar date (the user's reckoning). */
    fun forDay(church: LocalDate): Quote? {
        if (quotes.isEmpty()) return null
        val index = (slotOf(church) - 1) % quotes.size
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