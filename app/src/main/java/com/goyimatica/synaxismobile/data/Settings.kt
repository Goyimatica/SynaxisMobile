package com.goyimatica.synaxismobile.data

import android.content.SharedPreferences

/**
 * Everything the Settings screen changes.
 *
 * Stored as primitives rather than enum names so a renamed enum constant in a
 * later version cannot orphan somebody's preferences.
 */
data class Settings(
    val palette: Int = 0,          // 0 Night, 1 Midnight, 2 Sepia, 3 Parchment
    val face: Int = 0,             // 0 Cormorant, 1 Noto Serif, 2 Inter
    val sizeStep: Int = 3,         // 1..5
    val leadStep: Int = 2,         // 1..3
    val weight: Int = 500,         // 400 regular, 500 medium, 600 semibold
    val justify: Boolean = false,
    val dropCap: Boolean = true,
    val animations: Boolean = true,
    val calendarStyle: Int = 0,    // 0 Julian (old), 1 Revised (new)
    val keepScreenOn: Boolean = false,
    val syncOnWifiOnly: Boolean = false,
    val showPending: Boolean = true
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
            .apply()
    }

    companion object {
        fun read(p: SharedPreferences) = Settings(
            palette = p.getInt("palette", 0),
            face = p.getInt("face", 0),
            sizeStep = p.getInt("sizeStep", 3).coerceIn(1, 5),
            leadStep = p.getInt("leadStep", 2).coerceIn(1, 3),
            weight = p.getInt("weight", 500),
            justify = p.getBoolean("justify", false),
            dropCap = p.getBoolean("dropCap", true),
            animations = p.getBoolean("animations", true),
            calendarStyle = p.getInt("calendarStyle", 0),
            keepScreenOn = p.getBoolean("keepScreenOn", false),
            syncOnWifiOnly = p.getBoolean("syncOnWifiOnly", false),
            showPending = p.getBoolean("showPending", true)
        )
    }
}