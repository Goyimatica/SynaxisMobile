package com.goyimatica.synaxismobile.data

import android.content.SharedPreferences

/**
 * Everything the Settings screen changes.
 *
 * V8 adds two font names. They are stored as plain strings rather than as an
 * index into a list, because the list of installed fonts changes whenever the
 * user adds one, and an index into a shifting list is how preferences end up
 * pointing at the wrong thing. Blank means the built-in typeface.
 */
data class Settings(
    val palette: Int = 0,          // 0 Night, 1 Midnight, 2 Sepia, 3 Parchment
    val face: Int = 1,             // 0 Cormorant, 1 Noto Serif, 2 Inter
    val sizeStep: Int = 3,         // 1..5
    val leadStep: Int = 2,         // 1..3
    val weight: Int = 600,         // 400 regular, 500 medium, 600 semibold
    val justify: Boolean = false,
    val dropCap: Boolean = true,
    val animations: Boolean = true,
    val calendarStyle: Int = 0,    // 0 Julian (old), 1 Revised (new)
    val keepScreenOn: Boolean = false,
    val syncOnWifiOnly: Boolean = false,
    val showPending: Boolean = true,
    val uiFont: String = "",       // a downloaded family, or blank for built-in
    val readerFont: String = "",   // ditto, for the reader only
    val prefsVersion: Int = 8,
) {
    fun write(p: SharedPreferences) {
        p.edit()
            .putInt("palette", palette)
            .putInt("face", face)
            .putInt("sizeStep", sizeStep)
            .putInt("leadStep", leadStep)
            .putInt("weight", weight)
            .putBoolean("justify", justify)
            .putBoolean("dropCap", dropCap)
            .putBoolean("animations", animations)
            .putInt("calendarStyle", calendarStyle)
            .putBoolean("keepScreenOn", keepScreenOn)
            .putBoolean("syncOnWifiOnly", syncOnWifiOnly)
            .putBoolean("showPending", showPending)
            .putString("uiFont", uiFont)
            .putString("readerFont", readerFont)
            .putInt("prefsVersion", 8)
            .apply()
    }

    companion object {
        fun read(p: SharedPreferences): Settings {
            val stored = Settings(
                palette = p.getInt("palette", 0),
                face = p.getInt("face", 1),
                sizeStep = p.getInt("sizeStep", 3).coerceIn(1, 5),
                leadStep = p.getInt("leadStep", 2).coerceIn(1, 3),
                weight = p.getInt("weight", 600),
                justify = p.getBoolean("justify", false),
                dropCap = p.getBoolean("dropCap", true),
                animations = p.getBoolean("animations", true),
                calendarStyle = p.getInt("calendarStyle", 0),
                keepScreenOn = p.getBoolean("keepScreenOn", false),
                syncOnWifiOnly = p.getBoolean("syncOnWifiOnly", false),
                showPending = p.getBoolean("showPending", true),
                uiFont = p.getString("uiFont", "").orEmpty(),
                readerFont = p.getString("readerFont", "").orEmpty(),
                prefsVersion = 8,
            )

            // Written by an older build, and never revisited by hand.
            if (p.getInt("prefsVersion", 0) >= 7) return stored
            return stored.copy(
                face = if (stored.face == 0) 1 else stored.face,
                weight = if (stored.weight < 600) 600 else stored.weight,
                sizeStep = if (stored.sizeStep < 3) 3 else stored.sizeStep,
            )
        }
    }
}