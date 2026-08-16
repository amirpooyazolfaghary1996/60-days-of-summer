package dev.boardwork.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BW.Panel)
            .border(1.dp, BW.Edge, RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = content
    )
}

/** The silk-screened label: a ring bullet plus wide-tracked monospace. */
@Composable
fun ChannelLabel(text: String, color: Color = BW.Dim, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .border(1.5.dp, color, CircleShape)
        )
        Text(
            text.uppercase(),
            style = Label.copy(color = color),
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = Label, modifier = modifier)
}

@Composable
fun DisplayText(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 30,
    color: Color = BW.Ink,
    maxLines: Int = 2
) {
    Text(
        text.uppercase(),
        style = Display.copy(fontSize = size.sp, lineHeight = (size * 1.05).sp, color = color),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * The divider is a socket trace, not a rule: a line that jogs between two
 * terminals, the way the routes are printed on the board.
 */
@Composable
fun TraceDivider(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(16.dp)
    ) {
        val w = size.width
        val mid = size.height / 2f
        val hi = size.height * 0.18f
        val lo = size.height * 0.82f
        val a = w * 0.30f
        val b = w * 0.64f
        val sw = 1.5.dp.toPx()
        drawLine(BW.Edge, Offset(0f, mid), Offset(a, mid), sw, StrokeCap.Round)
        drawLine(BW.Edge, Offset(a, mid), Offset(a + w * 0.03f, hi), sw, StrokeCap.Round)
        drawLine(BW.Edge, Offset(a + w * 0.03f, hi), Offset(b, hi), sw, StrokeCap.Round)
        drawLine(BW.Edge, Offset(b, hi), Offset(b + w * 0.03f, lo), sw, StrokeCap.Round)
        drawLine(BW.Edge, Offset(b + w * 0.03f, lo), Offset(w, lo), sw, StrokeCap.Round)
        drawCircle(BW.Blue, 2.6.dp.toPx(), Offset(a + w * 0.03f, hi))
        drawCircle(BW.Yellow, 2.6.dp.toPx(), Offset(b + w * 0.03f, lo))
    }
}

@Composable
fun Pill(text: String, color: Color = BW.Dim, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(text.uppercase(), style = Label.copy(color = color))
    }
}

@Composable
fun BigButton(
    text: String,
    modifier: Modifier = Modifier,
    ghost: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (ghost) Color.Transparent else BW.Ink
    val fg = if (ghost) BW.Ink else BW.Board
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bg.copy(alpha = if (enabled) 1f else 0.4f))
            .then(if (ghost) Modifier.border(1.5.dp, BW.Edge, RoundedCornerShape(13.dp)) else Modifier)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun SetChip(
    index: Int,
    done: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (done) BW.Green else BW.Panel)
            .border(
                BorderStroke(1.5.dp, if (done) BW.Green else BW.Edge),
                RoundedCornerShape(11.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$index",
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
            color = if (done) Color(0xFF08120B) else BW.Dim
        )
    }
}

@Composable
fun StatRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** A tappable pill used for small option sets (sex, activity level, form profile). */
@Composable
fun SelectPill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) BW.Ink else Color.Transparent)
            .border(1.dp, if (selected) BW.Ink else BW.Edge, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) BW.Board else BW.Dim
        )
    }
}

/** A labelled numeric input, styled to match the rest of the moulded-plastic UI. */
@Composable
fun NumberField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    onChange: (String) -> Unit
) {
    Column(modifier) {
        Eyebrow(label)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(BW.Panel2)
                .border(1.dp, BW.Edge, RoundedCornerShape(11.dp))
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = { new -> onChange(new.filter { it.isDigit() || it == '.' }) },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = BW.Ink,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(BW.Ink),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                modifier = Modifier.weight(1f)
            )
            if (suffix != null) {
                Text(suffix, style = Data, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
fun Dot(color: Color, size: Int = 8) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}
