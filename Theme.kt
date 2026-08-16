package dev.boardwork.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The palette is taken straight off the equipment: matte black plastic and the
 * four silk-screened channel colours. Nothing else is introduced.
 */
object BW {
    val Board = Color(0xFF0E1013)
    val Panel = Color(0xFF191C21)
    val Panel2 = Color(0xFF20242B)
    val Edge = Color(0xFF2B303A)
    val Ink = Color(0xFFEEF1F5)
    val Dim = Color(0xFF8B94A3)
    val Faint = Color(0xFF5A6270)

    val Blue = Color(0xFF3B82F6)     // chest
    val Red = Color(0xFFE0483F)      // shoulder
    val Green = Color(0xFF3FAA5A)    // triceps
    val Yellow = Color(0xFFE8C130)   // back / laterals
    val Violet = Color(0xFF8B5CF6)   // legs (no channel on the board)

    fun channel(name: String): Color = when (name) {
        "blue" -> Blue
        "red" -> Red
        "green" -> Green
        "yellow" -> Yellow
        else -> Faint
    }

    fun parse(hex: String): Color =
        Color(("ff" + hex.removePrefix("#").lowercase()).toLong(16))

    fun dayColor(key: String): Color = when (key) {
        "push_a" -> Blue
        "push_b" -> Red
        "legs_a", "legs_b" -> Violet
        "pull_core" -> Yellow
        "mobility" -> Green
        "test" -> Ink
        else -> Faint
    }
}

/** Moulded-plastic lettering: heavy, tight, uppercase. */
val Display = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = (-1.2).sp,
    fontSize = 30.sp,
    lineHeight = 32.sp
)

/** Silk-screened board label: monospace, wide tracking, small. */
val Label = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 10.sp,
    letterSpacing = 1.6.sp,
    color = BW.Faint
)

val Data = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    letterSpacing = 0.4.sp,
    color = BW.Dim
)

private val Scheme = darkColorScheme(
    primary = BW.Ink,
    onPrimary = BW.Board,
    background = BW.Board,
    onBackground = BW.Ink,
    surface = BW.Panel,
    onSurface = BW.Ink,
    surfaceVariant = BW.Panel2,
    onSurfaceVariant = BW.Dim,
    outline = BW.Edge,
    error = BW.Red
)

@Composable
fun BoardWorkTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION") isSystemInDarkTheme() // the app is dark only
    MaterialTheme(
        colorScheme = Scheme,
        typography = Typography(),
        content = content
    )
}
