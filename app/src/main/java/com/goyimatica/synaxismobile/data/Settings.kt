package com.goyimatica.synaxismobile.data

/**
 * Everything the reader can choose. Defaults are what a first-time reader
 * gets: night palette, Cormorant at medium, the old reckoning.
 */
data class Settings(
    val palette: Int = 0,
    val face: Int = 0,
    val sizeStep: Int = 3,
    val leadStep: Int = 2,
    val weight: Int = 500,
    val justify: Boolean = false,
    val dropCap: Boolean = true,
    val animations: Boolean = true,
    val calendarStyle: Int = 0,
    val keepScreenOn: Boolean = false,
    val syncOnWifiOnly: Boolean = false,
    val showPending: Boolean = true,
)