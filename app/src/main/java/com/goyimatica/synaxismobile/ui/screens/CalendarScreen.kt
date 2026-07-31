package com.goyimatica.synaxismobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goyimatica.synaxismobile.data.SaintsRepo
import com.goyimatica.synaxismobile.data.Store
import com.goyimatica.synaxismobile.ui.Motion
import com.goyimatica.synaxismobile.ui.animColor
import com.goyimatica.synaxismobile.ui.animDp
import com.goyimatica.synaxismobile.ui.animFloat
import com.goyimatica.synaxismobile.ui.components.FastCard
import com.goyimatica.synaxismobile.ui.components.FeastCard
import com.goyimatica.synaxismobile.ui.components.SaintCard
import com.goyimatica.synaxismobile.ui.components.ScreenHeader
import com.goyimatica.synaxismobile.ui.components.SectionLabel
import com.goyimatica.synaxismobile.ui.components.SynChip
import com.goyimatica.synaxismobile.ui.components.fastColor
import com.goyimatica.synaxismobile.ui.pressScale
import com.goyimatica.synaxismobile.ui.rememberInteraction
import com.goyimatica.synaxismobile.ui.theme.Syn
import com.goyimatica.synaxismobile.ui.toCalStyle
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(onOpenSaint: (String) -> Unit, onOpenSettings: () -> Unit) {
    val c = Syn.colors
    val settings by Store.settings.collectAsStateWithLifecycle()
    val style = settings.toCalStyle()

    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.of(today.year, today.monthValue)) }
    var chosen by remember { mutableStateOf(today) }

    /*  Forty-two days of paschalion is cheap, but not cheap enough to redo on
        every recomposition - so it is keyed to the month and the reckoning and
        nothing else.  */
    val cells = remember(month, settings.calendarStyle) {
        val first = month.atDay(1)
        val lead = first.dayOfWeek.value - 1
        val start = first.minusDays(lead.toLong())
        (0 until 42).map { i ->
            val d = start.plusDays(i.toLong())
            factsFor(d, style)
        }
    }

    val facts = remember(chosen, settings.calendarStyle) { factsFor(chosen, style) }
    val commemorated = remember(facts.churchKey, settings.showPending) {
        SaintsRepo.onFeast(facts.churchKey).filter { settings.showPending || !it.pending }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("head") {
            ScreenHeader(
                overline = "Calendar",
                title = monthName(month.monthValue),
                subtitle = month.year.toString(),
                onSettings = onOpenSettings,
            )
            Spacer(Modifier.height(14.dp))
        }

        item("reckoning") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SynChip(
                    text = "Julian",
                    selected = settings.calendarStyle == 0,
                    onClick = { Store.update { it.copy(calendarStyle = 0) } },
                )
                SynChip(
                    text = "Revised",
                    selected = settings.calendarStyle == 1,
                    onClick = { Store.update { it.copy(calendarStyle = 1) } },
                )
            }
        }

        item("controls") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MorphButton(
                    onClick = { month = month.minusMonths(1) },
                    content = {
                        Icon(Icons.Outlined.ChevronLeft, "Previous month", tint = c.text)
                    },
                )
                MorphPill(
                    label = "Today",
                    active = month == YearMonth.of(today.year, today.monthValue) && chosen == today,
                    onClick = {
                        month = YearMonth.of(today.year, today.monthValue)
                        chosen = today
                    },
                )
                MorphButton(
                    onClick = { month = month.plusMonths(1) },
                    content = {
                        Icon(Icons.Outlined.ChevronRight, "Next month", tint = c.text)
                    },
                )
            }
        }

        item("years") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((2025..2040).toList(), key = { it }) { y ->
                    SynChip(
                        text = y.toString(),
                        selected = month.year == y,
                        onClick = { month = YearMonth.of(y, month.monthValue) },
                    )
                }
            }
        }

        item("dow") {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                DAY_INITIALS.forEachIndexed { i, d ->
                    Text(
                        d,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (i >= 5) c.goldDim else c.faint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item("grid") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { day ->
                            DayCellView(
                                facts = day,
                                inMonth = day.date.monthValue == month.monthValue,
                                isToday = day.date == today,
                                isChosen = day.date == chosen,
                                onClick = { chosen = day.date },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        item("chosen-fast") {
            FastCard(
                civilLine = facts.civilLine,
                churchLine = facts.churchLine,
                season = facts.season,
                rule = facts.rule,
                onClick = { chosen = chosen },
            )
        }

        item("feasts") {
            AnimatedVisibility(
                visible = facts.feasts.isNotEmpty(),
                enter = fadeIn(Motion.fade()) + expandVertically(Motion.size()),
                exit = fadeOut(Motion.fade()) + shrinkVertically(Motion.size()),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Spacer(Modifier.height(2.dp))
                    SectionLabel(if (facts.feasts.size == 1) "The feast" else "The feasts")
                    facts.feasts.forEach { FeastCard(it) }
                }
            }
        }

        if (commemorated.isNotEmpty()) {
            item("c-label") {
                Spacer(Modifier.height(4.dp))
                SectionLabel("Commemorated on " + shortDate(facts.churchDate))
            }
            items(commemorated, key = { "cal-" + it.id }) { s ->
                SaintCard(saint = s, onClick = { onOpenSaint(s.id) })
            }
        }

        item("legend") {
            Spacer(Modifier.height(10.dp))
            SectionLabel("The dots")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                LegendRow("No dot", "Fast-free, or an ordinary day outside a fast")
                LegendRow("A coloured dot", "The severity of the fast, palest to darkest")
                LegendRow("A gold pip", "A feast is kept on that day")
            }
        }
    }
}

/**
 * One day. The chosen day's shape travels from a circle to a rounded square,
 * which is the whole of the new Material button language and reads as motion
 * rather than as a colour swap.
 */
@Composable
private fun DayCellView(
    facts: DayFacts,
    inMonth: Boolean,
    isToday: Boolean,
    isChosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Syn.colors
    val press = rememberInteraction()

    val radius by animDp(if (isChosen) 12.dp else 22.dp, Motion.spatial())
    val fill by animColor(
        when {
            isChosen -> c.gold
            isToday -> c.raised
            else -> androidx.compose.ui.graphics.Color.Transparent
        }
    )
    val ink by animColor(
        when {
            isChosen -> c.bg
            !inMonth -> c.faint.copy(alpha = 0.45f)
            else -> c.text
        }
    )
    val shape = RoundedCornerShape(radius)

    Box(
        modifier
            .aspectRatio(1f)
            .pressScale(press, down = 0.9f)
            .clip(shape)
            .background(fill)
            .then(
                if (isToday && !isChosen) Modifier.border(1.dp, c.goldDim, shape) else Modifier
            )
            .clickable(interactionSource = press, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                facts.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isToday || isChosen) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = ink,
            )
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                val level = facts.rule.level
                if (level != com.goyimatica.synaxismobile.core.FastLevel.NONE) {
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isChosen) c.bg.copy(alpha = 0.7f) else fastColor(level)
                            ),
                    )
                }
                if (facts.feasts.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isChosen) c.bg.copy(alpha = 0.7f) else c.gold),
                    )
                }
            }
        }
    }
}

@Composable
private fun MorphButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = Syn.colors
    val press = rememberInteraction()
    val pressedScale by animFloat(1f, Motion.quick())
    val shape = RoundedCornerShape(16.dp)

    Box(
        Modifier
            .size(46.dp)
            .pressScale(press, down = 0.88f)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.rule, shape)
            .clickable(interactionSource = press, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size((22f * pressedScale).dp), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun MorphPill(label: String, active: Boolean, onClick: () -> Unit) {
    val c = Syn.colors
    val press = rememberInteraction()
    val radius by animDp(if (active) 14.dp else 23.dp, Motion.spatial())
    val fill by animColor(if (active) c.raised else c.surface)
    val ink by animColor(if (active) c.gold else c.dim)
    val shape = RoundedCornerShape(radius)

    Box(
        Modifier
            .height(46.dp)
            .width(120.dp)
            .pressScale(press, down = 0.94f)
            .clip(shape)
            .background(fill)
            .border(1.dp, if (active) c.goldDim else c.rule, shape)
            .clickable(interactionSource = press, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}

@Composable
private fun LegendRow(head: String, body: String) {
    val c = Syn.colors
    Row(verticalAlignment = Alignment.Top) {
        Text(
            head,
            style = MaterialTheme.typography.labelSmall,
            color = c.goldDim,
            modifier = Modifier.width(104.dp),
        )
        Text(body, style = MaterialTheme.typography.bodySmall, color = c.faint)
    }
}