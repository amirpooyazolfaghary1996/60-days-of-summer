package dev.boardwork.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.view.WindowManager
import android.app.Activity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.boardwork.data.Repository
import dev.boardwork.data.Step
import kotlinx.coroutines.delay

@Composable
fun SessionScreen(
    repo: Repository,
    dayNumber: Int,
    onClose: () -> Unit,
    onFinish: (Int) -> Unit
) {
    val day = remember(dayNumber) { repo.day(dayNumber) }
    val steps: List<Step> = remember(dayNumber) { day.steps() }
    if (steps.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    var index by remember { mutableIntStateOf(0) }
    val setsDone = remember { mutableStateListOf<Int>().apply { repeat(steps.size) { add(0) } } }
    var restLeft by remember { mutableIntStateOf(0) }
    var restLabel by remember { mutableStateOf("") }

    val step = steps[index]
    val exercise = repo.exercise(step.item.exercise)
    val port = repo.port(exercise.board)
    val accent = port?.let { BW.parse(it.color) } ?: BW.Faint
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // rest countdown
    LaunchedEffect(restLeft) {
        if (restLeft > 0) {
            delay(1000)
            restLeft -= 1
            if (restLeft == 0) chime(context)
        }
    }

    fun advance() {
        if (index < steps.lastIndex) {
            index += 1
            restLeft = 0
        } else {
            onFinish(dayNumber)
        }
    }

    fun tapSet(n: Int) {
        val current = setsDone[index]
        setsDone[index] = if (current == n) n - 1 else n
        val total = step.item.sets
        when {
            setsDone[index] >= total && index < steps.lastIndex -> {
                index += 1
                restLabel = "Next: ${repo.exercise(steps[index].item.exercise).name}"
                restLeft = day.restSeconds
            }
            setsDone[index] >= total -> onFinish(dayNumber)
            setsDone[index] == n -> {
                restLabel = "Set ${n + 1} of $total"
                restLeft = day.restSeconds
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BW.Board)
    ) {
        Column(Modifier.fillMaxSize()) {
            // header
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "×",
                    fontSize = 30.sp,
                    color = BW.Dim,
                    modifier = Modifier
                        .clickable { onClose() }
                        .padding(end = 14.dp)
                )
                Column(Modifier.weight(1f)) {
                    Eyebrow(step.block)
                    Spacer(Modifier.height(3.dp))
                    DisplayText(exercise.name, size = 24, maxLines = 1)
                }
                Text("${index + 1}/${steps.size}", style = Data)
            }

            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 0.dp, 16.dp, 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                                style = Label.copy(fontSize = 9.sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                item {
                    StatRow {
                        DisplayText(
                            when (step.item.unit) {
                                "max" -> "Max effort"
                                "sec" -> "${step.item.sets} × ${step.item.target}s"
                                else -> "${step.item.sets} × ${step.item.target}"
                            },
                            size = 26
                        )
                        Pill(if (exercise.unilateral) "Each side" else "${tempoLabel(exercise.tempo)} per rep")
                    }
                    step.item.note?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = BW.Dim, fontSize = 13.5.sp)
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        for (k in 1..step.item.sets) {
                            SetChip(k, k <= setsDone[index], Modifier.weight(1f)) { tapSet(k) }
                        }
                    }
                }

                item {
                    Column {
                        exercise.cues.forEachIndexed { i, cue ->
                            Row(Modifier.padding(bottom = 9.dp)) {
                                Text(
                                    "${i + 1}",
                                    style = Label.copy(fontSize = 10.sp),
                                    modifier = Modifier
                                        .width(22.dp)
                                        .padding(top = 2.dp)
                                )
                                Text(cue, color = BW.Dim, fontSize = 14.5.sp)
                            }
                        }
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
                val regressionText = exercise.regression
                if (regressionText != null) {
                    item {
                        Panel {
                            Eyebrow("Too hard")
                            Spacer(Modifier.height(6.dp))
                            Text(regressionText, fontSize = 14.sp)
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
                val substituteText = exercise.substitute
                if (substituteText != null) {
                    item {
                        Panel {
                            Eyebrow("No safe anchor")
                            Spacer(Modifier.height(6.dp))
                            Text(substituteText, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // footer
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(16.dp)
        ) {
            BigButton(if (index == steps.lastIndex) "Finish session" else "Next movement") { advance() }
        }

        // rest overlay
        if (restLeft > 0) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(BW.Board.copy(alpha = 0.97f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Eyebrow("Rest")
                Spacer(Modifier.height(16.dp))
                Text(
                    "$restLeft",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light,
                    fontSize = 92.sp,
                    color = BW.Ink
                )
                Spacer(Modifier.height(12.dp))
                Text(restLabel, style = Data)
                Spacer(Modifier.height(24.dp))
                Box(Modifier.width(200.dp)) {
                    BigButton("Skip rest", ghost = true) { restLeft = 0 }
                }
            }
        }
    }
}

private fun tempoLabel(tempo: Float): String {
    val whole = tempo.toInt()
    return if (tempo == whole.toFloat()) "${whole}s" else String.format("%.1fs", tempo)
}

/** Two short tones plus a buzz when the rest interval ends. */
private fun chime(context: Context) {
    runCatching {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        android.os.Handler(android.os.Looper.getMainLooper())
            .postDelayed({ tone.release() }, 600)
    }
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 60, 120), -1))
    }
}
