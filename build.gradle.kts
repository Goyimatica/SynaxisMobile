// Top-level build file. Nothing is applied here — only declared.
// The Kotlin Android plugin is deliberately absent: AGP 9 provides Kotlin itself.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}