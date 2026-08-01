import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/* ---- signing ------------------------------------------------------------
 * keystore.properties (never committed) or four environment variables.
 * Either way the key itself stays out of git.
 */
val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun secret(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

val releaseStorePath = secret("storeFile", "SYNAXIS_STORE_FILE")
val releaseStorePass = secret("storePassword", "SYNAXIS_STORE_PASSWORD")
val releaseKeyAlias = secret("keyAlias", "SYNAXIS_KEY_ALIAS")
val releaseKeyPass = secret("keyPassword", "SYNAXIS_KEY_PASSWORD")
val canSignRelease =
    releaseStorePath != null && rootProject.file(releaseStorePath).exists() &&
        releaseStorePass != null && releaseKeyAlias != null && releaseKeyPass != null

android {
    namespace = "com.goyimatica.synaxismobile"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.goyimatica.synaxismobile"
        minSdk = 24
        targetSdk = 37

        // CI passes VERSION_CODE and VERSION_NAME from the tag; locally these win.
        versionCode = (System.getenv("VERSION_CODE") ?: "7").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = rootProject.file(releaseStorePath!!)
                storePassword = releaseStorePass
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPass
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isDebuggable = false
            /* R8 is on. Coil 3 finds its network fetcher through a service
               loader entry; proguard-rules.pro keeps that registration alive,
               so a shrunk build still loads pictures. */
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            /* Only the real key, or none at all - never the debug key. The
               task guard below refuses to package a release without one. */
            signingConfig = signingConfigs.findByName("release")
        }
    }

    /* ---- lint ------------------------------------------------------------
     * lintVitalRelease runs automatically before a release APK is packaged
     * and, by default, any fatal-severity finding aborts the build. Findings
     * are still reported on every build, but they no longer stand between
     * you and an APK.
     */
    lint {
        abortOnError = false
        checkReleaseBuilds = true
        warningsAsErrors = false
        disable += setOf("FullBackupContent")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    androidResources {
        noCompress += listOf("ttf")
    }
}

/* ---- release without a key fails loudly --------------------------------
 * The debug keystore is public knowledge - it ships with every Android
 * Studio. Signing a release with it (the old fallback) would let anyone
 * with the SDK publish an "update" over this app. So a release is now
 * refused until a real key exists; only the debug variant may use it.
 */
val releaseCanSign = canSignRelease // local copy: task actions cannot capture the script

tasks.configureEach {
    if (name.startsWith("assembleRelease") ||
        name.startsWith("bundleRelease") ||
        name.startsWith("packageRelease")
    ) {
        doFirst {
            check(releaseCanSign) {
                "Release builds need a signing key. Create keystore.properties " +
                    "or set the SYNAXIS_STORE_FILE / SYNAXIS_STORE_PASSWORD / " +
                    "SYNAXIS_KEY_ALIAS / SYNAXIS_KEY_PASSWORD environment variables."
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.profileinstaller)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}