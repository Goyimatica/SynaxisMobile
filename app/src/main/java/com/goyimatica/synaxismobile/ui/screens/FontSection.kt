package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.data.Fonts
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.ui.components.HairRule
import com.goyimatica.synaxismobile.ui.components.Pressable
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.components.SynChip
import com.goyimatica.synaxismobile.ui.theme.Syn
import kotlinx.coroutines.launch

private const val BUILT_IN = "Built-in"

/**
 * Typefaces, in Settings.
 *
 * Two choices and one text field. The interface is set in one family from
 * top to bottom; the reader may keep its own, because a face that is right
 * for a tab bar is rarely right for four thousand words of a saint's life.
 *
 * Drop `FontSection()` anywhere in SettingsScreen's column.
 */
@Composable
fun FontSection() {
    val c = Syn.colors
    val scope = rememberCoroutineScope()
    val settings by Store.settings.collectAsStateWithLifecycle()

    var typed by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    /* Reading Fonts.installed inside composition is what subscribes this
       screen to it - it is snapshot state, so a finished download redraws
       the chips with nothing else asked of us. */
    val families = listOf(BUILT_IN) + Fonts.installed.toList()

    Column(Modifier.fillMaxWidth()) {

        SectionLabel("Typeface")
        Spacer(Modifier.height(14.dp))

        Text(
            "The interface is set in one family throughout. The reader may keep " +
                "its own \u2014 a face made for headings is seldom a face made for " +
                "four thousand words.",
            style = MaterialTheme.typography.bodySmall,
            color = c.faint,
        )

        Spacer(Modifier.height(18.dp))

        Text("Throughout the app", style = MaterialTheme.typography.titleMedium, color = c.text)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(families, key = { "ui-" + it }) { name ->
                val chosen = (name == BUILT_IN && settings.uiFont.isBlank()) ||
                    name == settings.uiFont
                SynChip(
                    text = name,
                    selected = chosen,
                    onClick = {
                        Store.update {
                            it.copy(uiFont = if (name == BUILT_IN) "" else name)
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text("In the reader", style = MaterialTheme.typography.titleMedium, color = c.text)
        Spacer(Modifier.height(4.dp))
        Text(
            "Built-in keeps whichever of Cormorant, Noto Serif or Inter you chose above.",
            style = MaterialTheme.typography.bodySmall,
            color = c.faint,
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(families, key = { "rd-" + it }) { name ->
                val chosen = (name == BUILT_IN && settings.readerFont.isBlank()) ||
                    name == settings.readerFont
                SynChip(
                    text = name,
                    selected = chosen,
                    onClick = {
                        Store.update {
                            it.copy(readerFont = if (name == BUILT_IN) "" else name)
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        HairRule()
        Spacer(Modifier.height(22.dp))

        Text("Add one from Google Fonts", style = MaterialTheme.typography.titleMedium, color = c.text)
        Spacer(Modifier.height(4.dp))
        Text(
            "A family name, a fonts.google.com link, or a direct link to a .ttf file. " +
                "Four weights and an italic are downloaded and kept on this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = c.faint,
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = typed,
            onValueChange = {
                typed = it
                message = ""
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !busy,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("EB Garamond", color = c.faint) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.goldDim,
                unfocusedBorderColor = c.rule,
                focusedTextColor = c.text,
                unfocusedTextColor = c.text,
                cursorColor = c.gold,
            ),
        )

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Pressable(
                onClick = {
                    if (busy || typed.isBlank()) return@Pressable
                    busy = true
                    message = "Fetching\u2026"
                    val wanted = typed
                    scope.launch {
                        val result = Fonts.install(wanted)
                        busy = false
                        result
                            .onSuccess { name ->
                                typed = ""
                                message = name + " is installed. Choose it above."
                            }
                            .onFailure { e ->
                                message = e.message ?: "That did not work."
                            }
                    }
                },
                down = 0.94f,
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (busy) c.surface else c.gold)
                        .border(1.dp, c.goldDim, RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                ) {
                    Text(
                        if (busy) "Downloading\u2026" else "Download",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (busy) c.dim else c.bg,
                    )
                }
            }

            if (settings.uiFont.isNotBlank() || settings.readerFont.isNotBlank()) {
                Spacer(Modifier.width(12.dp))
                Pressable(
                    onClick = { Store.update { it.copy(uiFont = "", readerFont = "") } },
                    down = 0.94f,
                ) {
                    Text(
                        "Back to built-in",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.dim,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
                    )
                }
            }
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = c.goldDim,
            )
        }

        if (Fonts.installed.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SectionLabel("Downloaded")
            Spacer(Modifier.height(10.dp))
            Fonts.installed.toList().forEach { name ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(name, style = MaterialTheme.typography.bodyMedium, color = c.text)
                    Pressable(
                        onClick = {
                            Fonts.remove(name)
                            Store.update { s ->
                                s.copy(
                                    uiFont = if (s.uiFont == name) "" else s.uiFont,
                                    readerFont = if (s.readerFont == name) "" else s.readerFont,
                                )
                            }
                        },
                        down = 0.9f,
                    ) {
                        Text(
                            "Remove",
                            style = MaterialTheme.typography.labelLarge,
                            color = c.blood,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}