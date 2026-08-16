package dev.boardwork.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.boardwork.data.Exercise
import dev.boardwork.data.Frame
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot

/*
 * The rig is a 2D skeleton in a 100x100 box with the floor at y = 92.
 * Poses are keyframed as joint positions and interpolated with a smoothstep
 * curve, so the same JSON drives both this app and the web version.
 */

private val BONES = listOf(
    "shoulder" to "hip",
    "shoulder" to "elbow",
    "elbow" to "hand",
    "hip" to "knee",
    "knee" to "foot"
)

private val FAR_BONES = listOf(
    "shoulder" to "elbow2",
    "elbow2" to "hand2",
    "hip" to "knee2",
    "knee2" to "foot2"
)

private fun smoothstep(u: Float) = u * u * (3f - 2f * u)

/** Interpolate the pose at normalised time [t] in 0..1. */
fun samplePose(frames: List<Frame>, t: Float): Map<String, List<Float>> {
    if (frames.isEmpty()) return emptyMap()
    val time = t.coerceIn(0f, 0.999999f)
    for (i in 0 until frames.size - 1) {
        val a = frames[i]
        val b = frames[i + 1]
        if (time in a.t..b.t) {
            val span = b.t - a.t
            val u = if (span <= 0f) 0f else smoothstep((time - a.t) / span)
            val keys = a.p.keys + b.p.keys
            return keys.associateWith { k ->
                val pa = a.p[k] ?: b.p[k]!!
                val pb = b.p[k] ?: a.p[k]!!
                listOf(
                    pa[0] + (pb[0] - pa[0]) * u,
                    pa[1] + (pb[1] - pa[1]) * u
                )
            }
        }
    }
    return frames.last().p
}

@Composable
fun RigView(
    exercise: Exercise,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val cycleMs = (exercise.tempo * 1000f).toInt().coerceAtLeast(400)
    val transition = rememberInfiniteTransition(label = "rig")

    // One continuous ramp over two cycles lets us derive both the pose phase
    // and, for mirrored movements, which side is currently working.
    val ramp by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleMs * 2, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val phase = ramp - floor(ramp)
    val flip = exercise.mirror && ramp >= 1f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .background(
                Brush.verticalGradient(listOf(Color(0xFF22262E), Color(0xFF14171B)))
            )
    ) {
        drawRig(exercise, phase, flip, accent)
    }
}

private fun DrawScope.drawRig(
    exercise: Exercise,
    phase: Float,
    flip: Boolean,
    accent: Color
) {
    val pad = size.minDimension * 0.06f
    val s = minOf((size.width - pad * 2) / 100f, (size.height - pad * 2) / 100f)
    val ox = (size.width - 100f * s) / 2f
    val oy = (size.height - 100f * s) / 2f

    fun px(x: Float) = ox + (if (flip) 100f - x else x) * s
    fun py(y: Float) = oy + y * s

    // floor
    drawLine(
        BW.Edge,
        Offset(ox, py(92f)),
        Offset(ox + 100f * s, py(92f)),
        strokeWidth = s * 0.5f
    )

    val pose = samplePose(exercise.frames, phase)

    // equipment
    val propColor = Color(0xFF49525F)
    exercise.props.forEach { p ->
        when (p.kind) {
            "box" -> {
                val left = px(if (flip) p.x + p.w else p.x)
                val top = py(p.y)
                drawRect(
                    propColor.copy(alpha = 0.16f),
                    topLeft = Offset(left, top),
                    size = Size(p.w * s, p.h * s)
                )
                drawRect(
                    propColor,
                    topLeft = Offset(left, top),
                    size = Size(p.w * s, p.h * s),
                    style = Stroke(width = s * 0.7f)
                )
            }
            "wall" -> drawLine(
                propColor, Offset(px(p.x), py(2f)), Offset(px(p.x), py(92f)),
                strokeWidth = s * 0.8f
            )
            "bar" -> drawLine(
                propColor, Offset(px(p.x1), py(p.y1)), Offset(px(p.x2), py(p.y2)),
                strokeWidth = s * 0.8f
            )
            "strap" -> pose["hand"]?.let { h ->
                drawLine(
                    propColor, Offset(px(h[0]), py(h[1])), Offset(px(p.x), py(p.y)),
                    strokeWidth = s * 0.6f
                )
            }
        }
    }

    fun bone(a: String, b: String, color: Color, width: Float) {
        val pa = pose[a] ?: return
        val pb = pose[b] ?: return
        drawLine(
            color,
            Offset(px(pa[0]), py(pa[1])),
            Offset(px(pb[0]), py(pb[1])),
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }

    FAR_BONES.forEach { (a, b) -> bone(a, b, Color(0xFF5E6775), s * 1.4f) }
    BONES.forEach { (a, b) -> bone(a, b, BW.Ink, s * 1.9f) }

    // neck stops at the skull edge so the line never runs through the head
    val head = pose["head"]
    val shoulder = pose["shoulder"]
    if (head != null && shoulder != null) {
        val hx = px(head[0]); val hy = py(head[1])
        val sx = px(shoulder[0]); val sy = py(shoulder[1])
        val r = s * 7.2f
        val dx = hx - sx; val dy = hy - sy
        val len = hypot(dx, dy).takeIf { it > 0.01f } ?: 1f
        drawLine(
            BW.Ink,
            Offset(sx, sy),
            Offset(hx - dx / len * r, hy - dy / len * r),
            strokeWidth = s * 1.9f,
            cap = StrokeCap.Round
        )
        drawCircle(BW.Ink, radius = r, center = Offset(hx, hy), style = Stroke(width = s * 1.9f))
    }

    // working joints picked out in the channel colour
    listOf("hand", "elbow", "knee", "foot").forEach { k ->
        pose[k]?.let { drawCircle(accent, radius = s * 1.4f, center = Offset(px(it[0]), py(it[1]))) }
    }
}

/** Unused helper kept for symmetry with the web renderer. */
internal fun distance(a: List<Float>, b: List<Float>) = abs(a[0] - b[0]) + abs(a[1] - b[1])
