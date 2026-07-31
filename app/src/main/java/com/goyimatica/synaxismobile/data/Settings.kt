package com.goyimatica.synaxismobile.data

import android.content.SharedPreferences

/**
 * Everything the Settings screen changes.
 *
 * Stored as primitives rather than enum names so a renamed enum constant in a
 * later version cannot orphan somebody's preferences.
 *
 * V7 changes three defaults: the reading face is Noto Serif rather than
 * Cormorant, the weight is semibold, and the size scale itself got bigger in
 * Theme.kt. prefsVersion lifts those three for anyone who installed earlier
 * and never changed them.
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
    val prefsVersion: Int = 7,
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
            .putInt("prefsVersion", 7)
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
                prefsVersion = 7,
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