package dev.boardwork.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.boardwork.data.ActivityLevel
import dev.boardwork.data.AdaptiveResult
import dev.boardwork.data.BodyGoal
import dev.boardwork.data.DietEngine
import dev.boardwork.data.GoalDirection
import dev.boardwork.data.Progress
import dev.boardwork.data.Repository
import dev.boardwork.data.Sex
import java.time.LocalDate

private val BENCHMARKS = listOf(
    "pushup_standard", "pike_pushup", "pushup_diamond",
    "bulgarian_split", "table_row", "plank", "hollow_hold"
)

@Composable
fun BodyScreen(
    repo: Repository,
    progress: Progress,
    onLogWeight: (Double) -> Unit,
    onSaveGoal: (BodyGoal) -> Unit,
    onTest: (String, String) -> Unit,
    onReset: () -> Unit
) {
    val latestWeight = repo.latestWeightKg(progress)
    val adaptive = repo.adaptive(progress)
    val diet = repo.dietTargets(progress)
    val goal = progress.goal
    var editingGoal by remember(goal) { mutableStateOf(goal == null) }

    val streak = remember(progress) {
        var s = 0
        for (i in progress.current - 1 downTo 1) {
            if (i in progress.done) s++ else break
        }
        s
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Eyebrow("Weekly weigh-in and goal")
            Spacer(Modifier.height(6.dp))
            DisplayText("Body", size = 26)
            Spacer(Modifier.height(6.dp))
        }

        item { WeightLogCard(latestWeight, onLogWeight) }

        item {
            Panel {
                Eyebrow("Trend")
                Spacer(Modifier.height(10.dp))
                WeightChart(progress.weightLog, goal)
            }
        }

        if (adaptive != null) item { AdaptiveCard(adaptive) }
        if (diet != null && goal != null) item { DietCard(diet, goal.direction) }

        item {
            Panel {
                StatRow {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("Goal")
                        Spacer(Modifier.height(6.dp))
                        if (goal == null) {
                            Text("No goal set yet", color = BW.Dim, fontSize = 14.sp)
                        } else {
                            Text(
                                "${directionLabel(goal.direction)} to %.1f kg by ${LocalDate.ofEpochDay(goal.targetDateEpochDay)}"
                                    .format(goal.goalWeightKg),
                                color = BW.Ink, fontSize = 14.sp
                            )
                        }
                    }
                    Pill(if (editingGoal) "Cancel" else if (goal == null) "Set goal" else "Edit") {
                        editingGoal = !editingGoal
                    }
                }
                if (editingGoal) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = BW.Edge)
                    Spacer(Modifier.height(16.dp))
                    GoalEditorFields(
                        repo = repo,
                        existing = goal,
                        latestWeight = latestWeight,
                        onSave = {
                            onSaveGoal(it)
                            editingGoal = false
                        }
                    )
                }
            }
        }

        item {
            Panel {
                StatRow {
                    BodyStat("Days logged", "${progress.done.size}", "/60")
                    BodyStat("Weigh-ins", "${progress.weightLog.size}", null)
                    BodyStat("Streak", "$streak", null)
                }
            }
        }

        item {
            Panel {
                Eyebrow("Benchmarks")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Run these on day 1 and again on day 60. One all-out set each.",
                    color = BW.Dim, fontSize = 13.5.sp
                )
                Spacer(Modifier.height(12.dp))
                BENCHMARKS.forEachIndexed { idx, id ->
                    val e = repo.exercise(id)
                    StatRow(Modifier.padding(vertical = 6.dp)) {
                        Text(e.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        BenchmarkField(progress.tests["$id.1"].orEmpty(), "d1") { onTest("$id.1", it) }
                        Spacer(Modifier.width(6.dp))
                        BenchmarkField(progress.tests["$id.60"].orEmpty(), "d60") { onTest("$id.60", it) }
                    }
                    if (idx < BENCHMARKS.lastIndex) HorizontalDivider(color = BW.Edge)
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            BigButton("Reset all progress", ghost = true) { onReset() }
        }
    }
}

/* ------------------------------------------------------------- weight log */

@Composable
private fun WeightLogCard(latestKg: Double?, onLog: (Double) -> Unit) {
    var text by remember { mutableStateOf("") }
    Panel {
        Eyebrow("This week")
        Spacer(Modifier.height(4.dp))
        Text(
            latestKg?.let { "%.1f kg".format(it) } ?: "No weigh-ins yet",
            style = Display.copy(fontSize = 28.sp, color = BW.Ink)
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            NumberField(
                "New weigh-in", text, suffix = "kg",
                modifier = Modifier.weight(1f)
            ) { text = it }
            Spacer(Modifier.width(10.dp))
            val value = text.toDoubleOrNull()
            Box(
                Modifier
                    .height(50.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (value != null) BW.Ink else BW.Panel2)
                    .border(1.dp, if (value != null) BW.Ink else BW.Edge, RoundedCornerShape(11.dp))
                    .clickable(enabled = value != null) {
                        value?.let { onLog(it); text = "" }
                    }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Log", color = if (value != null) BW.Board else BW.Faint, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* --------------------------------------------------------- adaptive & diet */

@Composable
private fun AdaptiveCard(result: AdaptiveResult) {
    Panel {
        Eyebrow("Adaptive coaching")
        Spacer(Modifier.height(10.dp))
        StatRow {
            Column {
                Eyebrow("Target")
                Spacer(Modifier.height(4.dp))
                Text(rateText(result.targetWeeklyRateKg), style = Display.copy(fontSize = 19.sp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Eyebrow("Actual")
                Spacer(Modifier.height(4.dp))
                Text(
                    result.actualWeeklyRateKg?.let { rateText(it) } ?: "—",
                    style = Display.copy(fontSize = 19.sp)
                )
            }
        }
        if (result.hasEnoughData && kotlin.math.abs(result.volumeMultiplier - 1.0) > 0.01) {
            Spacer(Modifier.height(10.dp))
            Pill("Sets ×%.2f this week".format(result.volumeMultiplier), BW.Yellow)
        }
        Spacer(Modifier.height(10.dp))
        Text(result.message, color = BW.Dim, fontSize = 13.5.sp)
    }
}

private fun rateText(kgPerWeek: Double): String {
    val sign = if (kgPerWeek > 0.001) "+" else ""
    return "$sign%.2f kg/wk".format(kgPerWeek)
}

@Composable
private fun DietCard(targets: DietEngine.Targets, direction: GoalDirection) {
    Panel {
        Eyebrow("Daily target")
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${targets.calories}", style = Display.copy(fontSize = 32.sp))
            Text(" kcal/day", style = Data, modifier = Modifier.padding(bottom = 6.dp, start = 5.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text("Maintenance ~${targets.maintenance} kcal · BMR ~${targets.bmr} kcal", style = Data)
        targets.clampNote?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = BW.Yellow, fontSize = 12.5.sp)
        }
        Spacer(Modifier.height(16.dp))
        StatRow {
            MacroStat("Protein", "${targets.proteinG.first}–${targets.proteinG.last} g")
            MacroStat("Fat", "${targets.fatG} g")
            MacroStat("Carbs", "${targets.carbG} g")
        }
        Spacer(Modifier.height(16.dp))
        Eyebrow("Where to spend it")
        Spacer(Modifier.height(8.dp))
        DietEngine.guidance(direction).forEach {
            Text("•  $it", color = BW.Dim, fontSize = 13.5.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(DietEngine.DISCLAIMER, color = BW.Faint, fontSize = 11.sp)
    }
}

@Composable
private fun MacroStat(label: String, value: String) {
    Column {
        Eyebrow(label)
        Spacer(Modifier.height(4.dp))
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = BW.Ink)
    }
}

/* ---------------------------------------------------------------- editors */

@Composable
private fun GoalEditorFields(
    repo: Repository,
    existing: BodyGoal?,
    latestWeight: Double?,
    onSave: (BodyGoal) -> Unit
) {
    val athlete = repo.plan.athlete
    var goalWeightText by remember { mutableStateOf(existing?.goalWeightKg?.let { "%.1f".format(it) } ?: "") }
    var heightText by remember { mutableStateOf((existing?.heightCm ?: athlete.heightCm).toString()) }
    var ageText by remember { mutableStateOf((existing?.age ?: athlete.age).toString()) }
    var months by remember {
        mutableStateOf(
            existing?.let {
                val days = (it.targetDateEpochDay - it.startDateEpochDay).coerceAtLeast(30)
                (days / 30).toInt().coerceIn(1, 12)
            } ?: 2
        )
    }
    var sex by remember { mutableStateOf(existing?.sex ?: Sex.UNSPECIFIED) }
    var activity by remember { mutableStateOf(existing?.activity ?: ActivityLevel.MODERATE) }

    val goalWeight = goalWeightText.toDoubleOrNull()
    val height = heightText.toIntOrNull()
    val age = ageText.toIntOrNull()
    val canSave = goalWeight != null && goalWeight > 0 && height != null && age != null

    Column {
        NumberField("Goal weight", goalWeightText, suffix = "kg") { goalWeightText = it }
        Spacer(Modifier.height(14.dp))

        Eyebrow("Timeframe")
        Spacer(Modifier.height(6.dp))
        MonthStepper(months) { months = it }
        Spacer(Modifier.height(4.dp))
        Text(
            "Target date: ${LocalDate.now().plusMonths(months.toLong())}",
            style = Data
        )
        Spacer(Modifier.height(14.dp))

        Row {
            NumberField("Height", heightText, suffix = "cm", modifier = Modifier.weight(1f)) { heightText = it }
            Spacer(Modifier.width(10.dp))
            NumberField("Age", ageText, suffix = "yrs", modifier = Modifier.weight(1f)) { ageText = it }
        }
        Spacer(Modifier.height(14.dp))

        Eyebrow("Sex (for the calorie formula)")
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            SelectPill("Male", sex == Sex.MALE) { sex = Sex.MALE }
            Spacer(Modifier.width(8.dp))
            SelectPill("Female", sex == Sex.FEMALE) { sex = Sex.FEMALE }
            Spacer(Modifier.width(8.dp))
            SelectPill("Prefer not to say", sex == Sex.UNSPECIFIED) { sex = Sex.UNSPECIFIED }
        }
        Spacer(Modifier.height(14.dp))

        Eyebrow("Activity outside training")
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            ActivityLevel.entries.forEachIndexed { i, level ->
                if (i > 0) Spacer(Modifier.width(8.dp))
                SelectPill(level.name.lowercase().replaceFirstChar { it.uppercase() }, activity == level) {
                    activity = level
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(activity.label, color = BW.Faint, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))

        BigButton("Save goal", enabled = canSave) {
            if (goalWeight != null && height != null && age != null) {
                onSave(
                    BodyGoal(
                        startWeightKg = latestWeight ?: goalWeight,
                        goalWeightKg = goalWeight,
                        startDateEpochDay = LocalDate.now().toEpochDay(),
                        targetDateEpochDay = LocalDate.now().plusMonths(months.toLong()).toEpochDay(),
                        heightCm = height,
                        age = age,
                        sex = sex,
                        activity = activity
                    )
                )
            }
        }
    }
}

@Composable
private fun MonthStepper(months: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepCircle("−") { if (months > 1) onChange(months - 1) }
        Text(
            "$months month${if (months == 1) "" else "s"}",
            modifier = Modifier.padding(horizontal = 18.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            color = BW.Ink
        )
        StepCircle("+") { if (months < 12) onChange(months + 1) }
    }
}

@Composable
private fun StepCircle(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .border(1.5.dp, BW.Edge, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, fontSize = 18.sp, color = BW.Ink)
    }
}

@Composable
private fun BodyStat(label: String, value: String, suffix: String?) {
    Column {
        Eyebrow(label)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontFamily = FontFamily.Monospace, fontSize = 26.sp, color = BW.Ink)
            if (suffix != null) {
                Text(suffix, style = Data.copy(color = BW.Faint), modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun BenchmarkField(value: String, hint: String, onChange: (String) -> Unit) {
    Box(
        Modifier
            .width(64.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(BW.Panel2)
            .border(1.dp, BW.Edge, RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = { onChange(it.filter { c -> c.isDigit() }.take(4)) },
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = BW.Ink
            ),
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(BW.Ink),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(hint, color = BW.Faint, fontSize = 12.sp)
                    }
                    inner()
                }
            }
        )
    }
}

private fun directionLabel(d: GoalDirection): String = when (d) {
    GoalDirection.GAIN -> "Gain"
    GoalDirection.LOSE -> "Lose"
    GoalDirection.MAINTAIN -> "Maintain"
}
