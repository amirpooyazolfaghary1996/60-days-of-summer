package dev.boardwork.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * A schematic of the push-up board with the sockets you need for the current
 * movement lit in their channel colour. This is the one thing the app has that
 * a generic workout app cannot: it is tied to this specific piece of kit.
 *
 * The layout is stylised, not a photograph. Match on colour and on how wide
 * apart the lit pair sits; the written note on each screen is the exact
 * instruction.
 */

private data class Socket(val id: String, val x: Float, val y: Float, val channel: String)

private val SOCKETS = listOf(
    Socket("S1", 16f, 24f, "blue"), Socket("S2", 84f, 24f, "blue"),
    Socket("S3", 30f, 36f, "blue"), Socket("S4", 70f, 36f, "blue"),
    Socket("S5", 20f, 48f, "yellow"), Socket("S6", 80f, 48f, "yellow"),
    Socket("S7", 26f, 60f, "red"), Socket("S8", 74f, 60f, "red"),
    Socket("S9", 38f, 52f, "green"), Socket("S10", 62f, 52f, "green"),
    Socket("S11", 40f, 70f, "green"), Socket("S12", 60f, 70f, "green"),
    Socket("S13", 30f, 82f, "red"), Socket("S14", 70f, 82f, "red"),
    Socket("S15", 50f, 20f, "yellow"), Socket("S16", 50f, 66f, "red")
)

private val PAIRS = mapOf(
    "blue-wide" to listOf("S1", "S2"),
    "blue-outer" to listOf("S3", "S4"),
    "yellow" to listOf("S5", "S6"),
    "red-outer" to listOf("S7", "S8"),
    "green-inner" to listOf("S11", "S12"),
    "red-center" to listOf("S13", "S14")
)

private val TRACES = listOf(
    "blue" to listOf(16f to 24f, 50f to 20f, 84f to 24f),
    "blue" to listOf(30f to 36f, 70f to 36f),
    "yellow" to listOf(20f to 48f, 50f to 20f, 80f to 48f),
    "green" to listOf(38f to 52f, 50f to 60f, 62f to 52f),
    "green" to listOf(40f to 70f, 50f to 60f, 60f to 70f),
    "red" to listOf(26f to 60f, 50f to 66f, 74f to 60f),
    "red" to listOf(30f to 82f, 50f to 66f, 70f to 82f)
)

@Composable
fun PortMap(boardKey: String?, modifier: Modifier = Modifier) {
    val active = PAIRS[boardKey].orEmpty()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.14f)
    ) {
        val s = minOf(size.width / 100f, size.height / 100f)
        val ox = (size.width - 100f * s) / 2f
        val oy = (size.height - 100f * s) / 2f
        fun px(x: Float) = ox + x * s
        fun py(y: Float) = oy + y * s

        // board body
        drawRoundRect(
            Color(0xFF15181D),
            topLeft = Offset(px(2f), py(10f)),
            size = Size(96f * s, 84f * s),
            cornerRadius = CornerRadius(12f * s, 12f * s)
        )
        drawRoundRect(
            BW.Edge,
            topLeft = Offset(px(2f), py(10f)),
            size = Size(96f * s, 84f * s),
            cornerRadius = CornerRadius(12f * s, 12f * s),
            style = Stroke(width = s * 0.7f)
        )

        // printed traces
        val traceAlpha = if (active.isEmpty()) 0.40f else 0.16f
        TRACES.forEach { (channel, points) ->
            val path = Path()
            points.forEachIndexed { i, (x, y) ->
                if (i == 0) path.moveTo(px(x), py(y)) else path.lineTo(px(x), py(y))
            }
            drawPath(
                path,
                BW.channel(channel).copy(alpha = traceAlpha),
                style = Stroke(width = s * 2.6f, join = StrokeJoin.Round, cap = StrokeCap.Round)
            )
        }

        // sockets
        SOCKETS.forEach { socket ->
            val on = socket.id in active
            val c = BW.channel(socket.channel)
            val center = Offset(px(socket.x), py(socket.y))
            if (on) {
                drawCircle(c, radius = s * 5.4f, center = center)
                drawCircle(
                    c.copy(alpha = 0.35f),
                    radius = s * 9f,
                    center = center,
                    style = Stroke(width = s * 1.4f)
                )
            } else {
                drawCircle(Color(0xFF252A32), radius = s * 3.2f, center = center)
                drawCircle(
                    Color(0xFF333944),
                    radius = s * 3.2f,
                    center = center,
                    style = Stroke(width = s * 1f)
                )
            }
        }
    }
}
