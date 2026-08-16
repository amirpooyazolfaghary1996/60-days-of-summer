package dev.boardwork

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.boardwork.data.Exercise
import dev.boardwork.data.Progress
import dev.boardwork.data.Repository
import dev.boardwork.ui.BW
import dev.boardwork.ui.BigButton
import dev.boardwork.ui.BodyScreen
import dev.boardwork.ui.BoardWorkTheme
import dev.boardwork.ui.ChannelLabel
import dev.boardwork.ui.DisplayText
import dev.boardwork.ui.Eyebrow
import dev.boardwork.ui.FormCheckScreen
import dev.boardwork.ui.Label
import dev.boardwork.ui.LibraryScreen
import dev.boardwork.ui.Panel
import dev.boardwork.ui.Pill
import dev.boardwork.ui.PlanScreen
import dev.boardwork.ui.PortMap
import dev.boardwork.ui.RigView
import dev.boardwork.ui.SessionScreen
import dev.boardwork.ui.TodayScreen

private enum class Tab(val label: String) {
    TODAY("Today"), PLAN("Plan"), MOVES("Moves"), FORM("Form"), BODY("Body")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = Repository(applicationContext)
        setContent {
            BoardWorkTheme { App(repo) }
        }
    }
}

@Composable
private fun App(repo: Repository) {
    var progress by remember { mutableStateOf(repo.loadProgress()) }
    var tab by remember { mutableStateOf(Tab.TODAY) }
    var sessionDay by remember { mutableStateOf<Int?>(null) }
    var openMove by remember { mutableStateOf<Exercise?>(null) }
    var picking by remember { mutableStateOf(false) }

    fun update(p: Progress) {
        progress = p
        repo.saveProgress(p)
    }

    fun complete(day: Int) {
        val next = if (day < repo.plan.days.size) day + 1 else day
        update(
            progress.copy(
                done = progress.done + day,
                current = if (progress.current == day) next else progress.current
            )
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BW.Board)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    Tab.TODAY -> TodayScreen(
                        repo, progress,
                        onStart = { sessionDay = it },
                        onLog = { complete(it) },
                        onPickDay = { picking = true }
                    )
                    Tab.PLAN -> PlanScreen(repo, progress) {
                        update(progress.copy(current = it))
                        tab = Tab.TODAY
                    }
                    Tab.MOVES -> LibraryScreen(repo) { openMove = it }
                    Tab.FORM -> FormCheckScreen(repo)
                    Tab.BODY -> BodyScreen(
                        repo, progress,
                        onLogWeight = { kg ->
                            val entry = dev.boardwork.data.WeightEntry(java.time.LocalDate.now().toEpochDay(), kg)
                            val withoutToday = progress.weightLog.filterNot { it.dateEpochDay == entry.dateEpochDay }
                            update(progress.copy(weightLog = withoutToday + entry))
                        },
                        onSaveGoal = { goal -> update(progress.copy(goal = goal)) },
                        onTest = { k, v -> update(progress.copy(tests = progress.tests + (k to v))) },
                        onReset = { update(Progress()) }
                    )
                }
            }
            BottomBar(tab) { tab = it }
        }
    }

    sessionDay?.let { day ->
        SessionScreen(
            repo, day,
            onClose = { sessionDay = null },
            onFinish = {
                complete(it)
                sessionDay = null
                tab = Tab.TODAY
            }
        )
        BackHandler { sessionDay = null }
    }

    openMove?.let { exercise ->
        MoveSheet(repo, exercise) { openMove = null }
        BackHandler { openMove = null }
    }

    if (picking) {
        DayPicker(
            current = progress.current,
            total = repo.plan.days.size,
            onPick = { update(progress.copy(current = it)); picking = false },
            onDismiss = { picking = false }
        )
        BackHandler { picking = false }
    }
}

@Composable
private fun BottomBar(active: Tab, onSelect: (Tab) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BW.Edge)
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(BW.Board)
            .padding(vertical = 9.dp)
    ) {
        Tab.entries.forEach { t ->
            val on = t == active
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onSelect(t) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (on) BW.Ink else Color.Transparent)
                        .border(1.5.dp, if (on) BW.Ink else BW.Faint, RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    t.label.uppercase(),
                    style = Label.copy(color = if (on) BW.Ink else BW.Faint, fontSize = 9.5.sp)
                )
            }
        }
    }
}

/* ------------------------------------------------------------- move detail */

@Composable
private fun MoveSheet(repo: Repository, exercise: Exercise, onClose: () -> Unit) {
    val port = repo.port(exercise.board)
    val accent = port?.let { BW.parse(it.color) } ?: BW.Faint

    Box(
        Modifier
            .fillMaxSize()
            .background(BW.Board)
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 14.dp, 18.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "×",
                    fontSize = 30.sp,
                    color = BW.Dim,
                    modifier = Modifier.clickable { onClose() }
                )
                Spacer(Modifier.height(6.dp))
                ChannelLabel(exercise.group)
                Spacer(Modifier.height(8.dp))
                DisplayText(exercise.name, size = 26)
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BW.Edge, RoundedCornerShape(16.dp))
                ) {
                    RigView(exercise, accent, Modifier.weight(1f))
                    Column(
                        Modifier
                            .width(112.dp)
                            .background(BW.Panel)
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        PortMap(exercise.board)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            (port?.label ?: "Mat only").uppercase(),
                            style = Label.copy(fontSize = 9.sp)
                        )
                    }
                }
            }
            item {
                Column {
                    exercise.cues.forEachIndexed { i, cue ->
                        Row(Modifier.padding(bottom = 9.dp)) {
                            Text(
                                "${i + 1}",
                                style = Label,
                                modifier = Modifier
                                    .width(22.dp)
                                    .padding(top = 2.dp)
                            )
                            Text(cue, color = BW.Dim, fontSize = 14.5.sp)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    exercise.muscles.take(3).forEach { Pill(it) }
                }
            }
            if (port != null) {
                item {
                    Panel {
                        Eyebrow("Handle placement")
                        Spacer(Modifier.height(6.dp))
                        Text(port.note, fontSize = 14.sp)
                    }
                }
            }
            val regression = exercise.regression
            if (regression != null) {
                item {
                    Panel {
                        Eyebrow("Too hard")
                        Spacer(Modifier.height(6.dp))
                        Text(regression, fontSize = 14.sp)
                    }
                }
            }
            val progressionText = exercise.progression
            if (progressionText != null) {
                item {
                    Panel {
                        Eyebrow("Too easy")
                        Spacer(Modifier.height(6.dp))
                        Text(progressionText, fontSize = 14.sp)
                    }
                }
            }
            val substitute = exercise.substitute
            if (substitute != null) {
                item {
                    Panel {
                        Eyebrow("No safe anchor")
                        Spacer(Modifier.height(6.dp))
                        Text(substitute, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/* --------------------------------------------------------------- day picker */

@Composable
private fun DayPicker(current: Int, total: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableIntStateOf(current) }

    Box(
        Modifier
            .fillMaxSize()
            .background(BW.Board.copy(alpha = 0.96f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Eyebrow("Jump to day")
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton("−") { if (value > 1) value-- }
                Text(
                    "$value",
                    style = Label.copy(fontSize = 56.sp, color = BW.Ink, letterSpacing = 0.sp),
                    modifier = Modifier.padding(horizontal = 26.dp)
                )
                StepButton("+") { if (value < total) value++ }
            }
            Spacer(Modifier.height(24.dp))
            Box(Modifier.width(240.dp)) {
                BigButton("Go to day $value") { onPick(value) }
            }
        }
    }
}

@Composable
private fun StepButton(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(54.dp)
            .clip(CircleShape)
            .border(1.5.dp, BW.Edge, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, fontSize = 26.sp, color = BW.Ink)
    }
}
