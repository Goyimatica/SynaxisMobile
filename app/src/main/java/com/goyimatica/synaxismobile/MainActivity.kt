package com.goyimatica.synaxismobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goyimatica.synaxismobile.core.CalStyle
import com.goyimatica.synaxismobile.core.Cal
import com.goyimatica.synaxismobile.core.FastLevel
import com.goyimatica.synaxismobile.ui.theme.Palette
import com.goyimatica.synaxismobile.ui.theme.Syn
import com.goyimatica.synaxismobile.ui.theme.SynaxisTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SynaxisTheme(palette = Palette.NIGHT) {
                TodayPreviewScreen()
            }
        }
    }
}

@Composable
private fun TodayPreviewScreen() {
    val c = Syn.colors
    var style by remember { mutableStateOf(CalStyle.JULIAN) }
    val today = remember { LocalDate.now() }
    val info = Cal.dayInfo(today, style)

    Scaffold(containerColor = c.bg) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = "SYNAXIS",
                style = MaterialTheme.typography.labelSmall,
                color = c.gold,
            )
            Spacer(Modifier.height(14.dp))

            Text(
                text = Cal.fmt(today),
                style = MaterialTheme.typography.headlineLarge,
                color = c.text,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = Cal.churchLine(today, style),
                style = MaterialTheme.typography.bodyMedium,
                color = c.dim,
            )

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalStyle.entries.forEach { s ->
                    FilterChip(
                        selected = style == s,
                        onClick = { style = s },
                        label = { Text(if (s == CalStyle.JULIAN) "Old Calendar" else "New Calendar") },
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            if (info.feasts.isEmpty()) {
                Text(
                    "No feast is appointed for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.faint,
                )
            }
            info.feasts.forEach { f ->
                Text(
                    text = f.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (f.great) c.gold else c.text,
                )
                if (f.note.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(f.note, style = MaterialTheme.typography.bodyMedium, color = c.dim)
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.surface)
                    .padding(16.dp),
            ) {
                Text(
                    text = if (info.fast.level == FastLevel.NONE && info.fast.label.isEmpty())
                        "No fast today" else info.fast.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.gold,
                    fontWeight = FontWeight.Medium,
                )
                if (info.fast.detail.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(info.fast.detail, style = MaterialTheme.typography.bodyMedium, color = c.dim)
                }
                if (info.fast.eat.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text("EAT", style = MaterialTheme.typography.labelSmall, color = c.faint)
                    Spacer(Modifier.height(6.dp))
                    info.fast.eat.forEach { Text("·  $it", style = MaterialTheme.typography.bodyMedium, color = c.text) }
                }
                if (info.fast.avoid.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text("SET ASIDE", style = MaterialTheme.typography.labelSmall, color = c.faint)
                    Spacer(Modifier.height(6.dp))
                    info.fast.avoid.forEach { Text("·  $it", style = MaterialTheme.typography.bodyMedium, color = c.dim) }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "${Cal.count()} commemorations in the engine · Pascha " +
                    Cal.fmt(com.goyimatica.synaxismobile.core.Pascha.of(today.year)),
                style = MaterialTheme.typography.bodySmall,
                color = c.faint,
            )
        }
    }
}