package dev.boardwork.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.boardwork.data.Day
import dev.boardwork.data.Exercise
import dev.boardwork.data.Item
import dev.boardwork.data.Progress
import dev.boardwork.data.Repository

internal fun Repository.accent(e: Exercise) =
    port(e.board)?.let { BW.parse(it.color) } ?: BW.Faint

internal fun targetText(repo: Repository, item: Item): String {
    val e = repo.exercise(item.exercise)
    return when (item.unit) {
        "max" -> "Max effort"
        "sec" -> "${item.sets} × ${item.target}s" + if (e.unilateral) " / side" else ""
        else -> "${item.sets} × ${item.target}" + if (e.unilateral) " / side" else ""
    }
}

/* ------------------------------------------------------------------- TODAY */

@Composable
fun TodayScreen(
    repo: Repository,
    progress: Progress,
    onStart: (Int) -> Unit,
    onLog: (Int) -> Unit,
    onPickDay: () -> Unit
) {
    val day = repo.day(progress.current)
    val phase = repo.phaseOf(day)
    val done = day.day in progress.done

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 14.dp, 18.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            StatRow {
                Column(Modifier.weight(1f)) {
                    Eyebrow("Phase ${phase.id} · ${phase.name} · Week ${day.week}")
                    Spacer(Modifier.height(6.dp))
                    DisplayText("Day ${day.day} — ${day.title}", size = 30)
                }
                Pill("Change day", modifier = Modifier.clickable { onPickDay() })
            }
            TraceDivider(Modifier.padding(vertical = 6.dp))
        }

        if (day.isRest) {
            item {
                Panel {
                    ChannelLabel("Rest", BW.Faint)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Nothing scheduled. Walk, sleep well, eat enough. " +
                            "Muscle is built on the days you do not train.",
                        color = BW.Ink, fontSize = 15.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                BigButton("Mark day complete", ghost = true) { onLog(day.day) }
            }
            return@LazyColumn
        }

        item {
            Panel {
                StatRow {
                    Column(Modifier.weight(1f)) {
                        ChannelLabel(day.subtitle)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "~${day.estimatedMinutes} min · ${day.restSeconds}s rest" +
                                if (day.deload) " · DELOAD" else "",
                            style = Data
                        )
                    }
                    if (done) Pill("Done", BW.Green)
                }
                Spacer(Modifier.height(12.dp))
                Text(day.intensityNote, color = BW.Dim, fontSize = 14.sp)
            }
        }

        day.blocks.filter { it.items.isNotEmpty() }.forEach { block ->
            item {
                Panel {
                    Eyebrow(block.name)
                    Spacer(Modifier.height(8.dp))
                    block.items.forEachIndexed { i, it ->
                        val e = repo.exercise(it.exercise)
                        StatRow(Modifier.padding(vertical = 9.dp)) {
                            Row(
                                Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Dot(repo.accent(e))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    e.name, fontSize = 15.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(targetText(repo, it), style = Data)
                        }
                        if (i < block.items.lastIndex) HorizontalDivider(color = BW.Edge)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            BigButton(if (done) "Repeat this session" else "Start session") { onStart(day.day) }
            if (!done) {
                Spacer(Modifier.height(9.dp))
                BigButton("Log without the timer", ghost = true) { onLog(day.day) }
            }
        }
    }
}

/* -------------------------------------------------------------------- PLAN */

@Composable
fun PlanScreen(repo: Repository, progress: Progress, onPick: (Int) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 14.dp, 18.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Eyebrow("60-day sequence")
            Spacer(Modifier.height(6.dp))
            DisplayText("The plan", size = 26)
            Spacer(Modifier.height(6.dp))
        }
        items(repo.plan.phases) { p ->
            Panel {
                StatRow {
                    ChannelLabel("Phase ${p.id} · ${p.name}")
                    Text("Days ${p.startDay}–${p.endDay}", style = Data)
                }
                Spacer(Modifier.height(7.dp))
                Text(p.focus, color = BW.Dim, fontSize = 14.sp)
            }
        }
        item {
            Spacer(Modifier.height(6.dp))
            DayGrid(repo.plan.days, progress, onPick)
        }
    }
}

@Composable
private fun DayGrid(days: List<Day>, progress: Progress, onPick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(((days.size + 6) / 7 * 52).dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        userScrollEnabled = false
    ) {
        items(days) { d ->
            val done = d.day in progress.done
            val isToday = d.day == progress.current
            val c = BW.dayColor(d.key)
            Box(
                Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (d.isRest) BW.Panel else c.copy(alpha = if (done) 0.5f else 0.22f))
                    .border(
                        if (isToday) 2.dp else 1.dp,
                        when {
                            isToday -> BW.Ink
                            done -> BW.Green
                            else -> BW.Edge
                        },
                        RoundedCornerShape(7.dp)
                    )
                    .clickable { onPick(d.day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${d.day}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (isToday || done) BW.Ink else BW.Faint
                )
            }
        }
    }
}

/* ----------------------------------------------------------------- LIBRARY */

private val GROUP_ORDER = listOf("chest", "shoulders", "triceps", "back", "legs", "core", "mobility")

@Composable
fun LibraryScreen(repo: Repository, onOpen: (Exercise) -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 14.dp, 18.dp, 120.dp)
    ) {
        item {
            Eyebrow("Every movement in the plan")
            Spacer(Modifier.height(6.dp))
            DisplayText("Moves", size = 26)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search movements or muscles", color = BW.Faint) },
                singleLine = true,
                shape = RoundedCornerShape(11.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        GROUP_ORDER.forEach { group ->
            val list = repo.library.exercises.filter {
                it.group == group && (q.isEmpty() ||
                    it.name.lowercase().contains(q) ||
                    it.muscles.joinToString(" ").lowercase().contains(q))
            }
            if (list.isEmpty()) return@forEach
            item {
                Spacer(Modifier.height(18.dp))
                Eyebrow(group)
                Spacer(Modifier.height(8.dp))
            }
            item {
                Panel {
                    list.forEachIndexed { i, e ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(e) }
                                .padding(vertical = 11.dp)
                        ) {
                            StatRow {
                                Row(
                                    Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Dot(repo.accent(e))
                                    Spacer(Modifier.width(10.dp))
                                    Text(e.name, fontSize = 15.sp)
                                }
                                Text(if (e.kind == "time") "hold" else "reps", style = Data)
                            }
                            Row(Modifier.padding(start = 18.dp, top = 3.dp)) {
                                Text(
                                    e.muscles.take(3).joinToString(" · "),
                                    color = BW.Faint, fontSize = 12.5.sp,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (e.formProfile != null) {
                                    Spacer(Modifier.width(8.dp))
                                    Pill("Form check", BW.Green)
                                }
                            }
                        }
                        if (i < list.lastIndex) HorizontalDivider(color = BW.Edge)
                    }
                }
            }
        }
    }
}
