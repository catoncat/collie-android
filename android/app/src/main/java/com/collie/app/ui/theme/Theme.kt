package com.collie.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

val Bg = Color(0xFF1C1C1C)
val Surface = Color(0xFF1C1C1C)
val Card = Color(0xFF2A2A2A)
val Raised = Color(0xFF2A2A2A)
val Inset = Color(0xFF141414)
val Fg = Color(0xFFF7F7F7)
val Muted = Color(0xFFA3A3A3)
val Faint = Color(0xFF6E6E6E)
val Accent = Color(0xFFE8E8E8)
val AccentFg = Color(0xFF2A2A2A)
val Blocked = Color(0xFFE07050)
val Working = Color(0xFFE0B44A)
val Ready = Color(0xFF5DC98A)
val Idle = Color(0xFF9AABBA)
val Info = Color(0xFF6AA8D8)
val Claude = Color(0xFFC45C3E)
val Rule = Color(0x3DFFFFFF)
val Border = Color(0x1FFFFFFF)
val ControlOn = Color(0xFF3A4A5C)
val ControlOnFg = Color(0xFFD6E4F0)
val Chrome = Color(0xFF2A2A2A)

val Sharp = RoundedCornerShape(2.dp)

private val HostPalette = listOf(
    Color(0xFF5EC8D8),
    Color(0xFFE08A4A),
    Color(0xFFB07AD4),
    Color(0xFF8FBF4A),
    Color(0xFFE07AA0),
    Color(0xFF4DB8A6),
    Color(0xFF6B8CDE),
    Color(0xFFD46BC0),
    Color(0xFF5AA8E0),
    Color(0xFF8A7AE0),
)

fun hostColor(id: String): Color {
    var h = 0
    for (c in id) h = 31 * h + c.code
    return HostPalette[kotlin.math.abs(h) % HostPalette.size]
}

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = AccentFg,
    background = Bg,
    onBackground = Fg,
    surface = Surface,
    onSurface = Fg,
    surfaceVariant = Raised,
    onSurfaceVariant = Muted,
    outline = Rule,
    error = Blocked,
)

data class CollieColors(
    val bg: Color = Bg,
    val surface: Color = Surface,
    val raised: Color = Raised,
    val inset: Color = Inset,
    val fg: Color = Fg,
    val muted: Color = Muted,
    val faint: Color = Faint,
    val accent: Color = Accent,
    val accentFg: Color = AccentFg,
    val blocked: Color = Blocked,
    val working: Color = Working,
    val ready: Color = Ready,
    val idle: Color = Idle,
    val claude: Color = Claude,
    val rule: Color = Rule,
)

val LocalCollieColors = staticCompositionLocalOf { CollieColors() }

@Composable
fun CollieTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCollieColors provides CollieColors()) {
        MaterialTheme(
            colorScheme = Scheme,
            typography = MaterialTheme.typography.copy(
                displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, color = Fg),
                headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = Fg),
                titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Fg),
                bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = Fg),
                bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Muted),
                labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = Muted),
            ),
            content = content,
        )
    }
}
