package com.goyimatica.synaxismobile.core

import java.time.DayOfWeek
import java.time.LocalDate

/* What may be eaten at each degree. */
private val FOOD: Map<FastLevel, Pair<List<String>, List<String>>> = mapOf(
    FastLevel.NONE to Pair(
        listOf("Meat, poultry, dairy and eggs", "Fish of every kind", "Wine and olive oil"),
        emptyList(),
    ),
    FastLevel.DAIRY to Pair(
        listOf("Cheese, milk, butter and eggs", "Fish, wine and olive oil"),
        listOf("Meat and poultry"),
    ),
    FastLevel.FISH to Pair(
        listOf("Fish", "Wine and olive oil", "Bread, grains, pulses, vegetables, fruit"),
        listOf("Meat and poultry", "Dairy and eggs"),
    ),
    FastLevel.OIL to Pair(
        listOf("Olive oil and wine", "Shellfish, octopus, squid", "Bread, grains, pulses, vegetables, fruit"),
        listOf("Meat and poultry", "Dairy and eggs", "Fish with a backbone"),
    ),
    FastLevel.XEROPHAGY to Pair(
        listOf(
            "Bread, grains and pulses",
            "Vegetables and fruit, raw or cooked without oil",
            "Nuts, olives, shellfish",
            "Water, tea, coffee",
        ),
        listOf(
            "Meat and poultry",
            "Dairy and eggs",
            "Fish with a backbone",
            "Olive oil and all oils",
            "Wine and every strong drink",
        ),
    ),
    FastLevel.STRICT to Pair(
        listOf("By the old custom, nothing until evening", "Bread, water and dried fruit for those who cannot keep it"),
        listOf("All food during the daylight hours", "Meat, dairy, fish, oil and wine when eating"),
    ),
)

private fun rule(level: FastLevel, label: String = "", detail: String = ""): FastRule {
    val (eat, avoid) = FOOD[level] ?: FOOD.getValue(FastLevel.NONE)
    return FastRule(level, label, detail, eat, avoid)
}

/* Is a church MM-DD inside a window? Wraps the new year, so ("12-25","01-04")
   means the Twelve Days rather than nothing at all. */
internal fun between(k: String, a: String, b: String): Boolean =
    if (a <= b) k >= a && k <= b else k >= a || k <= b

internal fun mmdd(d: LocalDate): String =
    "%02d-%02d".format(d.monthValue, d.dayOfMonth)

/*
 * The fast of a given civil day.
 *
 * Follows the older Russian typikon, which the app states on screen rather than
 * hiding — Greek practice is gentler on the ordinary Wednesday and Friday, and
 * the detail text says so where it matters.
 *
 * Order is load-bearing. Bright Week must be tested before Wednesday; the
 * Annunciation must be tested inside Great Lent, not after it.
 */
fun fastFor(date: LocalDate, style: CalStyle): FastRule {
    val po = Pascha.offset(date)
    val church = date.minusDays(style.shift)
    val k = mmdd(church)
    val dow = date.dayOfWeek
    val sat = dow == DayOfWeek.SATURDAY
    val sun = dow == DayOfWeek.SUNDAY
    val wed = dow == DayOfWeek.WEDNESDAY
    val fri = dow == DayOfWeek.FRIDAY

    /* ─ fast-free seasons ─ */
    if (po == 0L || po in 1L..6L) return rule(
        FastLevel.NONE, "Bright Week — fast-free",
        "From Pascha to Bright Saturday nothing at all is fasted, not even the Wednesday and the Friday. " +
            "The fast resumes on the Wednesday of St Thomas week.",
    )
    if (po in 50L..55L) return rule(
        FastLevel.NONE, "The week after Pentecost — fast-free",
        "Seven days without fasting, ending on the Saturday before All Saints.",
    )
    if (po in -69L..-64L) return rule(
        FastLevel.NONE, "The week of the Publican — fast-free",
        "The first week of the Triodion is kept without fasting, Wednesday and Friday included, " +
            "against the pride of the Pharisee who boasted of fasting twice in the week.",
    )
    if (between(k, "12-25", "01-04")) return rule(
        FastLevel.NONE, "The Twelve Days — fast-free",
        "From the Nativity to the eve of Theophany. Even Wednesday and Friday are free.",
    )

    /* ─ Cheesefare week ─ */
    if (po in -56L..-50L) return rule(
        FastLevel.DAIRY, "Cheesefare — no meat, dairy allowed",
        "Meat is set aside from Meatfare Sunday. Cheese, milk, butter and eggs are eaten all week, " +
            "on the Wednesday and Friday too, as the Church weans us gently.",
    )

    /* ─ Great Lent and Holy Week ─ */
    if (po in -48L..-1L) {
        if (po == -48L) return rule(
            FastLevel.STRICT, "Clean Monday — the strictest beginning",
            "The first day of Great Lent. Those who are able keep it without food until evening; " +
                "the rest eat plainly, without oil or wine.",
        )
        if (po == -2L) return rule(
            FastLevel.STRICT, "Great and Holy Friday — total fast",
            "The one day of the year on which the Church asks for no food at all until the burial " +
                "shroud has been carried out in the evening.",
        )
        if (po == -1L) return rule(
            FastLevel.XEROPHAGY, "Great and Holy Saturday — no oil",
            "The single Saturday of the year on which oil is not allowed. A little wine is permitted after the Liturgy.",
        )
        if (po == -7L) return rule(
            FastLevel.FISH, "Palm Sunday — fish allowed",
            "With the Annunciation, one of only two days in Great Lent on which fish is eaten.",
        )
        if (k == "03-25") {
            if (po in -6L..-3L) return rule(
                FastLevel.OIL, "The Annunciation in Holy Week — wine and oil",
                "Falling on the first four days of Holy Week the feast keeps wine and oil, but not fish.",
            )
            if (po == -2L || po == -1L) return rule(
                FastLevel.XEROPHAGY, "The Annunciation on the Great Days — wine only",
                "On Great Friday or Holy Saturday even the Annunciation yields: wine is allowed, fish and oil are not.",
            )
            return rule(
                FastLevel.FISH, "THE ANNUNCIATION — fish allowed",
                "A Great Feast of the Lord's incarnation. Fish, wine and oil are eaten on whatever day of Lent it falls.",
            )
        }
        if (sat || sun) return rule(
            FastLevel.OIL, "Great Lent — wine and oil",
            "On the Saturdays and Sundays of Lent two full meals are taken with wine and olive oil. " +
                "Fish, meat and dairy are still set aside.",
        )
        if (po == -17L || po == -16L) return rule(
            FastLevel.OIL, "Great Lent — wine and oil for the vigil",
            "Wine and oil are allowed for the Great Canon on the Thursday and the Akathist on the Friday of the fifth week.",
        )
        if (po == -43L) return rule(
            FastLevel.OIL, "St Theodore — wine and oil",
            "The first Saturday of Lent, kept with the blessing of kolyva.",
        )
        return rule(
            FastLevel.XEROPHAGY, "Great Lent — xerophagy",
            "Dry eating: no oil, no wine, no fish, nothing from an animal. This is the weekday rule of Lent " +
                "from Clean Monday to Lazarus Saturday.",
        )
    }

    /* ─ the Apostles' Fast ─
       It opens on the Monday after All Saints, which is Pascha + 57, and closes
       on the eve of Ss Peter and Paul, the fixed church date 28 June. One end is
       movable and the other is not, so the fast runs anywhere from eight to
       forty-two days. The church-date window is a sanity bound, not the rule. */
    if (po >= 57L && k >= "05-01" && k <= "06-28") {
        if (sat || sun) return rule(
            FastLevel.FISH, "The Apostles' Fast — fish on Saturday and Sunday",
            "A gentle fast in the summer. Fish, wine and oil on the weekend.",
        )
        if (wed || fri) return rule(
            FastLevel.XEROPHAGY, "The Apostles' Fast — xerophagy",
            "Wednesday and Friday are kept without oil or wine.",
        )
        if (dow == DayOfWeek.MONDAY) return rule(
            FastLevel.OIL, "The Apostles' Fast — wine and oil",
            "Monday is kept without fish, but wine and oil are allowed by most uses.",
        )
        return rule(
            FastLevel.FISH, "The Apostles' Fast — fish allowed",
            "Fish is eaten on Tuesday and Thursday through this fast.",
        )
    }

    /* ─ the Dormition Fast, church 1–14 August ─ */
    if (k >= "08-01" && k <= "08-14") {
        if (k == "08-06") return rule(
            FastLevel.FISH, "THE TRANSFIGURATION — fish allowed",
            "The one day of the Dormition Fast on which fish is eaten. Grapes and the first fruits are blessed.",
        )
        if (sat || sun) return rule(
            FastLevel.OIL, "The Dormition Fast — wine and oil",
            "Two weeks kept almost as strictly as Lent; the weekend brings wine and oil, but not fish.",
        )
        if (wed || fri) return rule(
            FastLevel.XEROPHAGY, "The Dormition Fast — xerophagy",
            "Dry eating on Wednesday and Friday.",
        )
        return rule(
            FastLevel.XEROPHAGY, "The Dormition Fast — xerophagy",
            "On weekdays the Dormition Fast is kept without oil, like Great Lent. " +
                "Some parishes allow oil on Tuesday and Thursday.",
        )
    }

    /* ─ the Nativity Fast, church 15 November – 24 December ─ */
    if (between(k, "11-15", "12-24")) {
        if (k == "12-24") return rule(
            FastLevel.STRICT, "The Eve of the Nativity — total fast",
            "By long custom nothing is eaten until the first star is seen, and then only kolyva or " +
                "sochivo — boiled wheat with honey.",
        )
        if (k == "11-21") return rule(
            FastLevel.FISH, "THE ENTRANCE OF THE THEOTOKOS — fish allowed",
            "A Great Feast in the middle of the fast; fish, wine and oil.",
        )
        val late = k >= "12-20" && k <= "12-24"
        if (late) {
            if (sat || sun) return rule(
                FastLevel.OIL, "The Nativity Fast, the last days — wine and oil",
                "From 20 December the fast tightens: no fish even at the weekend.",
            )
            return rule(
                FastLevel.XEROPHAGY, "The Nativity Fast, the last days — xerophagy",
                "The five days before the feast are kept as strictly as Great Lent.",
            )
        }
        if (wed || fri) return rule(
            FastLevel.XEROPHAGY, "The Nativity Fast — xerophagy",
            "Wednesday and Friday without oil or wine until 19 December.",
        )
        if (sat || sun || dow == DayOfWeek.TUESDAY || dow == DayOfWeek.THURSDAY) return rule(
            FastLevel.FISH, "The Nativity Fast — fish allowed",
            "Until 19 December fish is eaten on Tuesday, Thursday, Saturday and Sunday.",
        )
        return rule(
            FastLevel.OIL, "The Nativity Fast — wine and oil",
            "Monday is kept without fish; wine and oil are allowed.",
        )
    }

    /* ─ fixed days of fasting, whatever the weekday ─ */
    if (k == "09-14") return rule(
        FastLevel.OIL, "THE ELEVATION OF THE CROSS — a fast day",
        "A Great Feast kept with fasting for the sake of the Cross: no fish, no dairy, but wine and oil are allowed.",
    )
    if (k == "08-29") return rule(
        FastLevel.OIL, "The Beheading of the Forerunner — a fast day",
        "Kept as a fast on whatever day it falls, in memory of Herod's banquet. Wine and oil only.",
    )
    if (k == "01-05") return rule(
        FastLevel.XEROPHAGY, "The Eve of Theophany — strict fast",
        "Dry eating until the Great Blessing of Water.",
    )

    /* ─ the Wednesday and the Friday of every other week ─ */
    if (wed || fri) {
        val f = FIXED[k]
        if (f != null && f.great) return rule(
            FastLevel.FISH, "A Great Feast on a fast day — fish allowed",
            "When one of the Twelve Feasts falls on a Wednesday or a Friday outside the fasts, " +
                "fish, wine and oil are allowed.",
        )
        return rule(
            FastLevel.XEROPHAGY,
            (if (wed) "Wednesday" else "Friday") + " — a fast day",
            (if (wed) "For the betrayal of the Lord." else "For His crucifixion.") +
                " The Russian typikon asks for dry eating; much of Greek practice allows oil and wine " +
                "when there is no greater fast. Keep what your own parish keeps.",
        )
    }

    return rule(FastLevel.NONE)
}