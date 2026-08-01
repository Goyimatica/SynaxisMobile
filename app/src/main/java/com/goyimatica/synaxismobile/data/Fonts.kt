package com.goyimatica.synaxismobile.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

/*
 * Fonts the user brings with them.
 *
 * A family lives in filesDir/fonts/<Family Name>/ as w400.ttf, w500.ttf,
 * w600.ttf, w700.ttf and i400.ttf - whichever of them Google actually has.
 * The folder name is the family name, so there is no registry file to get
 * out of step with the disk.
 */
object Fonts {

    private const val TAG = "SynaxisFonts"
    private const val CSS = "https://fonts.googleapis.com/css2"

    /*
     * The important line in this file.
     *
     * Given a modern User-Agent, css2 answers with WOFF2, which Android's
     * font loader cannot read. Given an ancient one it answers with plain
     * TrueType, because that is all it believes the client can handle. We
     * want TrueType, so we ask as Internet Explorer 6.
     */
    private const val LEGACY_AGENT =
        "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)"

    private val WEIGHTS = listOf(400, 500, 600, 700)

    private var dir: File? = null

    /** Family names on this device. Snapshot state, so Settings redraws. */
    val installed = mutableStateListOf<String>()

    private val built = HashMap<String, FontFamily>()

    fun init(context: Context) {
        if (dir != null) return
        val d = File(context.filesDir, "fonts")
        if (!d.exists()) d.mkdirs()
        dir = d
        rescan()
    }

    private fun rescan() {
        val d = dir ?: return
        val found = (d.listFiles() ?: emptyArray())
            .filter { it.isDirectory && (it.listFiles()?.isNotEmpty() == true) }
            .map { it.name }
            .sorted()
        installed.clear()
        installed.addAll(found)
    }

    fun isInstalled(name: String): Boolean =
        name.isNotBlank() && installed.contains(name)

    /**
     * The family, built from whatever weights are on the disk.
     *
     * Built once and kept: a FontFamily made from files is cheap to hold and
     * expensive to rebuild on every recomposition of the whole theme.
     */
    fun family(name: String?): FontFamily? {
        if (name.isNullOrBlank()) return null
        built[name]?.let { return it }

        val folder = File(dir ?: return null, name)
        if (!folder.isDirectory) return null

        val fonts = ArrayList<androidx.compose.ui.text.font.Font>(5)
        WEIGHTS.forEach { w ->
            val f = File(folder, "w" + w + ".ttf")
            if (f.exists() && f.length() > 2000L) {
                fonts.add(Font(f, FontWeight(w), FontStyle.Normal))
            }
        }
        val italic = File(folder, "i400.ttf")
        if (italic.exists() && italic.length() > 2000L) {
            fonts.add(Font(italic, FontWeight.Normal, FontStyle.Italic))
        }

        if (fonts.isEmpty()) return null
        val family = FontFamily(fonts)
        built[name] = family
        return family
    }

    fun remove(name: String) {
        val folder = File(dir ?: return, name)
        folder.listFiles()?.forEach { it.delete() }
        folder.delete()
        built.remove(name)
        rescan()
    }

    /* ---- installing ------------------------------------------------------ */

    /**
     * Takes anything a person is likely to paste:
     *
     *   EB Garamond
     *   https://fonts.google.com/specimen/EB+Garamond
     *   a css2 link with a family= in it
     *   a direct link to a .ttf
     *
     * and returns the installed family name, or a plain-English failure.
     */
    suspend fun install(input: String): Result<String> = withContext(Dispatchers.IO) {
        val raw = input.trim()
        if (raw.isBlank()) return@withContext Result.failure(Exception("Nothing to install."))
        if (dir == null) return@withContext Result.failure(Exception("Fonts are not ready yet."))

        if (raw.endsWith(".ttf", ignoreCase = true) || raw.endsWith(".otf", ignoreCase = true)) {
            return@withContext directFile(raw)
        }

        val family = familyNameFrom(raw)
        if (family.isBlank()) {
            return@withContext Result.failure(Exception("Could not read a font name from that."))
        }

        val css = fetchCss(family)
            ?: return@withContext Result.failure(
                Exception("Google Fonts has no family called \"" + family + "\".")
            )

        val faces = parseFaces(css)
        if (faces.isEmpty()) {
            return@withContext Result.failure(Exception("No TrueType files were offered."))
        }

        val folder = File(dir, family)
        if (!folder.exists()) folder.mkdirs()

        var saved = 0
        faces.forEach { face ->
            val bytes = download(face.url)
            if (bytes != null && bytes.size > 2000) {
                val target = File(folder, face.fileName())
                runCatching { target.writeBytes(bytes) }.onSuccess { saved++ }
            }
        }

        if (saved == 0) {
            folder.delete()
            return@withContext Result.failure(Exception("The download did not finish."))
        }

        built.remove(family)
        rescan()
        Log.i(TAG, "installed " + family + " (" + saved + " files)")
        Result.success(family)
    }

    private fun directFile(url: String): Result<String> {
        val guessed = url.substringAfterLast('/')
            .substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
        val family = if (guessed.isBlank()) "Custom font" else guessed
        val bytes = download(url)
            ?: return Result.failure(Exception("That link could not be downloaded."))
        if (bytes.size < 2000) return Result.failure(Exception("That file is not a font."))

        val folder = File(dir, family)
        if (!folder.exists()) folder.mkdirs()
        runCatching { File(folder, "w400.ttf").writeBytes(bytes) }
            .onFailure { return Result.failure(Exception("Could not save the font.")) }

        built.remove(family)
        rescan()
        return Result.success(family)
    }

    /** "EB+Garamond" and "specimen/EB+Garamond" and "EB Garamond" all agree. */
    private fun familyNameFrom(raw: String): String {
        var s = raw
        if (s.contains("family=")) s = s.substringAfter("family=")
        if (s.contains("specimen/")) s = s.substringAfter("specimen/")
        s = s.substringBefore("&").substringBefore(":").substringBefore("?")
        s = s.replace('+', ' ').replace("%20", " ").trim()
        return s.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { w ->
                w.replaceFirstChar { ch -> ch.uppercaseChar() }
            }
    }

    private fun fetchCss(family: String): String? {
        val encoded = family.replace(" ", "+")
        val url = CSS + "?family=" + encoded +
            ":ital,wght@0,400;0,500;0,600;0,700;1,400&display=swap"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", LEGACY_AGENT)
            .build()
        return runCatching {
            Images.http.newCall(request).execute().use { r ->
                if (!r.isSuccessful) null else r.body?.string()
            }
        }.getOrNull()
    }

    private data class Face(val weight: Int, val italic: Boolean, val url: String) {
        fun fileName(): String = if (italic) "i400.ttf" else "w" + weight + ".ttf"
    }

    /*
     * The CSS comes back as a run of @font-face blocks. Each one carries a
     * font-style, a font-weight and one src url. Splitting on "@font-face"
     * keeps those three together, which a single global regex would not.
     */
    private fun parseFaces(css: String): List<Face> {
        val urlPattern = Regex("url\\((https?://[^)]+\\.ttf)\\)")
        val weightPattern = Regex("font-weight:\\s*(\\d{3})")

        val out = LinkedHashMap<String, Face>()
        css.split("@font-face").drop(1).forEach { block ->
            val url = urlPattern.find(block)?.groupValues?.get(1) ?: return@forEach
            val weight = weightPattern.find(block)?.groupValues?.get(1)?.toIntOrNull() ?: 400
            val italic = block.contains("font-style: italic") ||
                block.contains("font-style:italic")
            if (!italic && !WEIGHTS.contains(weight)) return@forEach
            if (italic && weight != 400) return@forEach
            val face = Face(weight, italic, url)
            /* the last block for a weight wins - it is the latin subset */
            out[face.fileName()] = face
        }
        return out.values.toList()
    }

    private fun download(url: String): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", LEGACY_AGENT)
            .build()
        return runCatching {
            Images.http.newCall(request).execute().use { r ->
                if (!r.isSuccessful) null else r.body?.bytes()
            }
        }.getOrNull()
    }
}