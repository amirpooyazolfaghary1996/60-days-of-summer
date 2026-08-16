package dev.boardwork.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import dev.boardwork.data.Exercise
import dev.boardwork.data.Repository
import dev.boardwork.pose.FormEngine
import dev.boardwork.pose.FormFeedback
import dev.boardwork.pose.FormProfile
import dev.boardwork.pose.PoseAnalyzer
import dev.boardwork.pose.Severity
import dev.boardwork.pose.framingHint
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * Live, on-device camera form-checking for the subset of movements with a
 * [FormProfile]. Everything runs on-device — no frame ever leaves the phone.
 * This is a heuristic, not a coach: it catches gross errors (shallow reps,
 * sagging hips, a knee angle outside a sane band) and says so when it can't
 * see clearly. Treat it as a second opinion alongside the cues, not a
 * replacement for them, especially on anything with real injury risk.
 */
@Composable
fun FormCheckScreen(repo: Repository) {
    val checkable = remember { repo.library.exercises.filter { it.formProfile != null } }
    var selected by remember { mutableStateOf<Exercise?>(null) }
    val current = selected

    if (current == null) {
        ExercisePicker(repo, checkable) { selected = it }
    } else {
        val profile = FormProfile.fromId(current.formProfile)
        if (profile == null) {
            selected = null
        } else {
            LiveCheck(current, profile, onBack = { selected = null })
        }
    }
}

@Composable
private fun ExercisePicker(repo: Repository, list: List<Exercise>, onPick: (Exercise) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Eyebrow("Camera form check")
            Spacer(Modifier.height(6.dp))
            DisplayText("Form", size = 26)
            Spacer(Modifier.height(10.dp))
            Text(
                "On-device pose tracking counts reps and flags depth, hip sag, and lean in real " +
                    "time. It's a heuristic on a single camera angle, not a coach — keep using the " +
                    "cues and, for anything demanding, a mirror or a second pair of eyes.",
                color = BW.Dim, fontSize = 13.5.sp
            )
        }
        item {
            Panel {
                list.forEachIndexed { idx, e ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(e) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Dot(repo.accent(e))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.name, fontSize = 15.sp, color = BW.Ink)
                        }
                        Pill(if (FormProfile.fromId(e.formProfile)?.countsReps == false) "Hold" else "Reps")
                    }
                    if (idx < list.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(color = BW.Edge)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCheck(exercise: Exercise, profile: FormProfile, onBack: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    val engine = remember(exercise.id) { FormEngine(profile) }
    var feedback by remember(exercise.id) { mutableStateOf<FormFeedback?>(null) }
    var useFrontCamera by remember { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .background(BW.Board)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "×", fontSize = 28.sp, color = BW.Dim,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 14.dp)
            )
            Column(Modifier.weight(1f)) {
                Eyebrow(exercise.group)
                DisplayText(exercise.name, size = 20, maxLines = 1)
            }
            Pill(if (useFrontCamera) "Front cam" else "Back cam") { useFrontCamera = !useFrontCamera }
        }

        if (!hasPermission) {
            PermissionPrompt(profile) { permissionLauncher.launch(Manifest.permission.CAMERA) }
        } else {
            Text(
                profile.framingHint(),
                color = BW.Faint, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                CameraPreview(useFrontCamera) { snapshot ->
                    feedback = engine.process(snapshot)
                }
            }
            FeedbackPanel(profile, engine.reps, feedback)
        }
    }
}

@Composable
private fun PermissionPrompt(profile: FormProfile, onRequest: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            "Form Check needs the camera to track your position. Nothing is recorded or leaves " +
                "the phone — frames are analyzed live and thrown away.",
            color = BW.Dim, fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(profile.framingHint(), color = BW.Faint, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        BigButton("Grant camera access") { onRequest() }
    }
}

@Composable
private fun FeedbackPanel(profile: FormProfile, reps: Int, feedback: FormFeedback?) {
    Column(Modifier.padding(16.dp)) {
        if (profile.countsReps) {
            StatRow {
                Column {
                    Eyebrow("Reps")
                    Spacer(Modifier.height(4.dp))
                    DisplayText("$reps", size = 32)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Eyebrow("Angle")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        feedback?.angleDeg?.let { "${it.roundToInt()}°" } ?: "—",
                        style = Data.copy(fontSize = 20.sp, color = BW.Ink)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            AngleBar(feedback?.angleDeg)
        } else {
            val good = feedback?.goodFormSeconds ?: 0.0
            val total = feedback?.totalSeconds ?: 0.0
            StatRow {
                Column {
                    Eyebrow("Good-form time")
                    Spacer(Modifier.height(4.dp))
                    DisplayText("${good.roundToInt()}s", size = 32)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Eyebrow("Total held")
                    Spacer(Modifier.height(4.dp))
                    Text("${total.roundToInt()}s", style = Data.copy(fontSize = 20.sp, color = BW.Ink))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        val (bg, fg) = when (feedback?.severity) {
            Severity.OK -> BW.Green to BW.Board
            Severity.WARN -> BW.Yellow to BW.Board
            Severity.LOW_CONFIDENCE, null -> BW.Panel2 to BW.Dim
        }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .padding(14.dp)
        ) {
            Text(
                feedback?.message ?: "Getting a read on your position…",
                color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
            )
        }
    }
}

/** A simple linear readout: full bar = arm/leg straight, empty = fully bent. No camera geometry involved. */
@Composable
private fun AngleBar(angleDeg: Double?) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val h = size.height
        drawRoundRect(BW.Panel2, size = size, cornerRadius = CornerRadius(h / 2, h / 2))
        val t = ((angleDeg ?: 0.0) / 180.0).coerceIn(0.0, 1.0).toFloat()
        if (t > 0f) {
            drawRoundRect(
                BW.Blue,
                size = Size(size.width * t, h),
                cornerRadius = CornerRadius(h / 2, h / 2)
            )
        }
    }
}

@Composable
private fun CameraPreview(
    useFrontCamera: Boolean,
    onSnapshot: (dev.boardwork.pose.PoseSnapshot) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val latestOnSnapshot = rememberUpdatedState(onSnapshot)
    val analyzer = remember { PoseAnalyzer(onResult = { latestOnSnapshot.value(it) }) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(useFrontCamera) {
        var boundProvider: ProcessCameraProvider? = null
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                val provider = providerFuture.get()
                boundProvider = provider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }
                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                provider.unbindAll()
                runCatching { provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis) }
            },
            ContextCompat.getMainExecutor(context)
        )
        onDispose { boundProvider?.unbindAll() }
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier.fillMaxSize())
}
