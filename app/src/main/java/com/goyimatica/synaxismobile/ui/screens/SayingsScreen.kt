package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.goyimatica.synaxismobile.data.QuotesRepo
import com.goyimatica.synaxismobile.ui.components.EmptyNote
import com.goyimatica.synaxismobile.ui.components.Pressable
import com.goyimatica.synaxismobile.ui.components.ScreenHeader
import com.goyimatica.synaxismobile.ui.theme.Syn
import kotlinx.coroutines.delay

@Composable
fun SayingsScreen(onOpenSaint: (String) -> Unit, onOpenSettings: () -> Unit) {
    val c = Syn.colors
    val clipboard = LocalClipboardManager.current
    val all = remember { QuotesRepo.all() }
    var copiedAt by remember { mutableStateOf(-1) }

    LaunchedEffect(copiedAt) {
        if (copiedAt >= 0) {
            delay(1600)
            copiedAt = -1
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item("head") {
            ScreenHeader(
                overline = "Sayings",
                title = "From the fathers",
                subtitle = all.size.toString() + " sayings \u00B7 tap one to copy it",
                onSettings = onOpenSettings,
            )
            Spacer(Modifier.height(12.dp))
        }

        if (all.isEmpty()) {
            item("empty") { EmptyNote("No sayings are loaded.") }
        } else {
            items(all.size) { i ->
                val q = all[i]
                Pressable(
                    onClick = {
                        val id = q.saintId
                        if (!id.isNullOrBlank()) {
                            clipboard.setText(
                                AnnotatedString("\u201C" + q.text.trim() + "\u201D\n\u2014 " + q.by)
                            )
                        } else {
                            clipboard.setText(
                                AnnotatedString("\u201C" + q.text.trim() + "\u201D\n\u2014 " + q.by)
                            )
                        }
                        copiedAt = i
                    },
                    modifier = Modifier.fillMaxWidth(),
                    down = 0.985f,
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.surface)
                            .padding(17.dp),
                    ) {
                        Text(
                            "\u201C" + q.text.trim() + "\u201D",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                            ),
                            color = c.text,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (copiedAt == i) "Copied." else "\u2014 " + q.by,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (copiedAt == i) c.gold else c.goldDim,
                        )
                    }
                }
            }
        }
    }
}