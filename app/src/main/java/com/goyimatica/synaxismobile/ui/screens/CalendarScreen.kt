package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.core.Cal
import com.goyimatica.synaxismobile.core.CalStyle
import com.goyimatica.synaxismobile.core.DayCell
import com.goyimatica.synaxismobile.core.FastLevel
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.ui.components.FastCard
import com.goyimatica.synaxismobile.ui.components.FastDot
import com.goyimatica.synaxismobile.ui.components.HairRule
import com.goyimatica.synaxismobile.ui.components.SaintCard
import com.goyimatica.synaxismobile.ui.components.ScreenHeader
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.components.SynChip
import com.goyimatica.synaxismobile.ui.components.fastColor
import com.goyimatica.synaxismobile.ui.components.fastWord
import com.goyimatica.synaxismobile.ui.theme.Syn
import com.goyimatica.synaxismobile.ui.toCalStyle
import com.goyimatica.synaxismobile.ui.toStored
import java.time.LocalDate

@Composable
fun CalendarScreen(onOpenSaint: (String) -> Unit) {
    val c = Syn.colors
    val settings by Store.settings.collectAsStateWithLifecycle()
    val style = settings.toCalStyle()

    val today = remember { LocalDate.now() }
    var year by remember { mutableStateOf(today.year) }
    var month by remember { mutableStateOf(today.monthValue) }
    var selected by remember { mutableStateOf(today) }

    val grid = remember(year, month, style) { Cal.month(year, month, style) }
    val weeks = remember(grid) {
        grid.cells.chunked(7).map { w -> if (w.size == 7) w else w + List(7 - w.size) { null } }
    }
    val info = remember(selected, style) { Cal.dayInfo(selected, style) }
    val onDay = remember(info.churchKey) { SaintsRepo.onFeast(info.churchKey) }
    val yearInfo = remember(year, style) { Cal.yearInfo(year, style) }

    fun step(by: Int) {
        var m = month + by
        var y = year
        if (m > 12) { m = 1; y += 1 }
        if (m < 1) { m = 12; y -= 1 }
        if (y in Cal.YEARS) { month = m; year = y }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("header") {
            ScreenHeader(
                kicker = "The church year",
                title = "Calendar",
                subtitle = "Both reckonings, the fasts in full, and every commemoration " +
                    "from 2025 to 2040.",
            )
            Spacer(Modifier.height(14.dp))
        }

        /* ---- which calendar ---- */
        item("style") {
            Column {
                SectionLabel("Reckoning")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalStyle.entries.forEach { s ->
                        SynChip(
                            text = if (s == CalStyle.JULIAN) "Old Calendar" else "New Calendar",
                            selected = style == s,
                            onClick = { Store.update { it.copy(calendarStyle = s.toStored()) } },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (style == CalStyle.JULIAN)
                        "Thirteen days behind the civil calendar. Kept by Jerusalem, Russia, " +
                            "Serbia, Georgia, Mount Athos and the Old Calendarists."
                    else
                        "The fixed feasts fall on the civil date. Kept by Constantinople, " +
                            "Greece, Romania, Antioch and most of the diaspora. " +
                            "Pascha is the same for all.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.faint,
                )
            }
        }

        /* ---- which year ---- */
        item("years") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Cal.YEARS, key = { it }) { y ->
                    SynChip(text = y.toString(), selected = y == year, onClick = { year = y })
                }
            }
        }

        /* ---- the grid ---- */
        item("grid") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.surface)
                    .padding(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.ChevronLeft, "Previous month", tint = c.dim,
                        modifier = Modifier.size(26.dp).clickable { step(-1) },
                    )
                    Text(
                        grid.name + " " + grid.year,
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.text,
                    )
                    Icon(
                        Icons.Outlined.ChevronRight, "Next month", tint = c.dim,
                        modifier = Modifier.size(26.dp).clickable { step(1) },
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    Cal.DOW_SHORT.forEach { d ->
                        Text(
                            d,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.faint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                weeks.forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { cell ->
                            DaySquare(
                                cell = cell,
                                isToday = cell?.date == today,
                                isSelected = cell?.date == selected,
                                modifier = Modifier.weight(1f),
                                onClick = { cell?.let { selected = it.date } },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HairRule()
                Spacer(Modifier.height(12.dp))
                Legend()
            }
        }

        /* ---- the day ---- */
        item("day-head") {
            Spacer(Modifier.height(6.dp))
            Column {
                Text(
                    Cal.fmt(selected),
                    style = MaterialTheme.typography.headlineMedium,
                    color = c.text,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    Cal.churchLine(selected, style),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.dim,
                )
            }
        }

        if (info.feasts.isNotEmpty()) {
            items(info.feasts, key = { "f-" + it.name }) { f ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (f.great) c.raised else c.surface)
                        .padding(15.dp),
                ) {
                    Text(
                        f.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (f.great) c.gold else c.text,
                    )
                    if (f.note.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(f.note, style = MaterialTheme.typography.bodySmall, color = c.dim)
                    }
                }
            }
        }

        item("day-fast") { FastCard(info.fast) }

        if (onDay.isNotEmpty()) {
            item("day-saints-label") {
                Spacer(Modifier.height(4.dp))
                SectionLabel(
                    if (onDay.size == 1) "One saint is commemorated"
                    else onDay.size.toString() + " saints are commemorated",
                )
            }
            items(onDay, key = { "s-" + it.id }) { s ->
                SaintCard(saint = s, onClick = { onOpenSaint(s.id) })
            }
        }

        /* ---- the year ---- */
        item("year-label") {
            Spacer(Modifier.height(12.dp))
            SectionLabel("The shape of " + year)
        }
        item("year") {
            val rows: List<Pair<String, LocalDate>> = listOf(
                "The Triodion opens" to yearInfo.triodion,
                "Meatfare" to yearInfo.meatfare,
                "Forgiveness Sunday" to yearInfo.cheesefare,
                "Great Lent begins" to yearInfo.lentBegins,
                "Lazarus Saturday" to yearInfo.lazarus,
                "Palm Sunday" to yearInfo.palm,
                "Great and Holy Thursday" to yearInfo.holyThursday,
                "Great and Holy Friday" to yearInfo.holyFriday,
                "PASCHA" to yearInfo.pascha,
                "Thomas Sunday" to yearInfo.thomas,
                "Radonitsa" to yearInfo.radonitsa,
                "The Ascension" to yearInfo.ascension,
                "Pentecost" to yearInfo.pentecost,
                "All Saints" to yearInfo.allSaints,
                "The Apostles' Fast begins" to yearInfo.apostlesFast,
                "Ss Peter and Paul" to yearInfo.apostlesEnd,
                "The Dormition Fast begins" to yearInfo.dormitionFast,
                "The Transfiguration" to yearInfo.transfiguration,
                "The Dormition" to yearInfo.dormition,
                "The Church New Year" to yearInfo.churchNewYear,
                "The Nativity of the Theotokos" to yearInfo.nativityTheotokos,
                "The Elevation of the Cross" to yearInfo.elevation,
                "The Protection" to yearInfo.protection,
                "The Nativity Fast begins" to yearInfo.nativityFast,
                "The Entrance of the Theotokos" to yearInfo.entrance,
                "THE NATIVITY OF CHRIST" to yearInfo.nativity,
                "Theophany" to yearInfo.theophany,
                "The Meeting of the Lord" to yearInfo.meeting,
                "The Annunciation" to yearInfo.annunciation,
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surface)
                    .padding(vertical = 6.dp),
            ) {
                rows.forEach { (name, date) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                year = date.year
                                month = date.monthValue
                                selected = date
                            }
                            .padding(horizontal = 15.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (name.first().isUpperCase() && name == name.uppercase())
                                c.gold else c.text,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            Cal.fmtShort(date),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.dim,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp)) {
                    Text(
                        "The Apostles' Fast runs " + yearInfo.apostlesDays +
                            " days this year, because one end of it moves with Pascha " +
                            "and the other does not.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.faint,
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySquare(
    cell: DayCell?,
    isToday: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = Syn.colors
    Box(
        modifier
            .aspectRatio(0.86f)
            .padding(2.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (isSelected) c.raised else androidx.compose.ui.graphics.Color.Transparent)
            .then(
                if (isToday) Modifier.border(1.dp, c.goldDim, RoundedCornerShape(9.dp))
                else Modifier,
            )
            .clickable(enabled = cell != null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (cell == null) return@Box
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                cell.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    cell.isPascha -> c.blood
                    cell.great -> c.gold
                    else -> c.text
                },
            )
            Spacer(Modifier.height(3.dp))
            /* the church day, small - this is the whole point of the Old Calendar */
            Text(
                cell.churchDay.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = c.faint,
            )
            Spacer(Modifier.height(3.dp))
            Box(
                Modifier
                    .size(if (cell.level == FastLevel.NONE) 4.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (cell.fastFree) c.rule else fastColor(cell.level),
                    ),
            )
        }
    }
}

@Composable
private fun Legend() {
    val c = Syn.colors
    Column {
        SectionLabel("What the dots mean")
        Spacer(Modifier.height(8.dp))
        listOf(
            FastLevel.NONE,
            FastLevel.DAIRY,
            FastLevel.FISH,
            FastLevel.OIL,
            FastLevel.XEROPHAGY,
            FastLevel.STRICT,
        ).chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 7.dp)) {
                row.forEach { level ->
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FastDot(level)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            fastWord(level),
                            style = MaterialTheme.typography.labelSmall,
                            color = c.dim,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "The small number under each day is the church date. A gold day is a " +
                "Great Feast; a red one is Pascha.",
            style = MaterialTheme.typography.bodySmall,
            color = c.faint,
        )
    }
}