package dev.boardwork.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.boardwork.data.BodyGoal
import dev.boardwork.data.WeightEntry
import kotlin.math.max
import kotlin.math.min

/**
 * A minimal line chart: the logged weigh-ins as a solid line, the goal path
 * as a dashed reference line from the goal's start to its target date.
 * No external charting library — this is the same custom-Canvas approach
 * used for the exercise rig, so the whole app stays dependency-light.
 */
@Composable
fun WeightChart(
    entries: List<WeightEntry>,
    goal: BodyGoal?,
    modifier: Modifier = Modifier
) {
    val sorted = remember(entries) { entries.sortedBy { it.dateEpochDay } }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = BW.Faint, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (sorted.isEmpty()) {
            drawText(
                textMeasurer, "No weigh-ins yet — add your first one below.",
                topLeft = Offset(4.dp.toPx(), size.height / 2 - 7.dp.toPx()),
                style = labelStyle
            )
            return@Canvas
        }

        val padL = 34.dp.toPx()
        val padR = 8.dp.toPx()
        val padT = 10.dp.toPx()
        val padB = 8.dp.toPx()
        val plotW = size.width - padL - padR
        val plotH = size.height - padT - padB

        var minX = sorted.first().dateEpochDay.toDouble()
        var maxX = sorted.last().dateEpochDay.toDouble()
        var minY = sorted.minOf { it.kg }
        var maxY = sorted.maxOf { it.kg }
        if (goal != null) {
            minX = min(minX, goal.startDateEpochDay.toDouble())
            maxX = max(maxX, goal.targetDateEpochDay.toDouble())
            minY = min(minY, min(goal.startWeightKg, goal.goalWeightKg))
            maxY = max(maxY, max(goal.startWeightKg, goal.goalWeightKg))
        }
        if (maxX <= minX) maxX = minX + 1.0
        val yPad = max(0.5, (maxY - minY) * 0.18)
        minY -= yPad
        maxY += yPad
        if (maxY <= minY) maxY = minY + 1.0

        fun px(x: Double): Float = padL + ((x - minX) / (maxX - minX) * plotW).toFloat()
        fun py(y: Double): Float = padT + ((maxY - y) / (maxY - minY) * plotH).toFloat()

        for (i in 0..2) {
            val v = minY + (maxY - minY) * i / 2.0
            val y = py(v)
            drawLine(BW.Edge, Offset(padL, y), Offset(size.width - padR, y), strokeWidth = 1.dp.toPx())
            drawText(
                textMeasurer, "%.1f".format(v),
                topLeft = Offset(0f, y - 6.dp.toPx()),
                style = labelStyle
            )
        }

        if (goal != null) {
            val p1 = Offset(px(goal.startDateEpochDay.toDouble()), py(goal.startWeightKg))
            val p2 = Offset(px(goal.targetDateEpochDay.toDouble()), py(goal.goalWeightKg))
            drawLine(
                BW.Violet.copy(alpha = 0.6f), p1, p2,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))
            )
            drawCircle(BW.Violet.copy(alpha = 0.6f), radius = 3.dp.toPx(), center = p2)
        }

        val points = sorted.map { Offset(px(it.dateEpochDay.toDouble()), py(it.kg)) }
        for (i in 0 until points.size - 1) {
            drawLine(BW.Blue, points[i], points[i + 1], strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
        }
        points.forEach { drawCircle(BW.Blue, radius = 3.2.dp.toPx(), center = it) }
        points.lastOrNull()?.let {
            drawCircle(BW.Ink, radius = 5.dp.toPx(), center = it, style = Stroke(width = 1.8.dp.toPx()))
        }
    }
}
