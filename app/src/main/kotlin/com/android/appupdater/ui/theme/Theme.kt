package com.android.appupdater.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class Palette(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accentEnd: Color,
    val accentSoft: Color,
    val success: Color,
    val danger: Color,
    val glassEdge: Color,
    val scrim: Color
)

private val DarkPalette = Palette(
    background = Color(0xFF0B0C0F),
    surface = Color(0xFF14161B),
    surface2 = Color(0xFF1B1E25),
    surface3 = Color(0xFF232733),
    border = Color(0xFF272B35),
    text = Color(0xFFEEF1F6),
    muted = Color(0xFF97A0B2),
    accent = Color(0xFF018857),
    accentEnd = Color(0xFF018489),
    accentSoft = Color(0x2E018857),
    success = Color(0xFF23C56E),
    danger = Color(0xFFFF5470),
    glassEdge = Color(0x1FFFFFFF),
    scrim = Color(0x6B04060A)
)

private val LightPalette = Palette(
    background = Color(0xFFF4F5F8),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF0F2F6),
    surface3 = Color(0xFFE6E9EF),
    border = Color(0xFFDFE3EA),
    text = Color(0xFF131720),
    muted = Color(0xFF5F6878),
    accent = Color(0xFF01784D),
    accentEnd = Color(0xFF018489),
    accentSoft = Color(0x2401784D),
    success = Color(0xFF23C56E),
    danger = Color(0xFFD10022),
    glassEdge = Color(0xD9FFFFFF),
    scrim = Color(0x6B04060A)
)

@Immutable
data class TypeScale(
    val h2: TextStyle,
    val h3: TextStyle,
    val body: TextStyle,
    val brand: TextStyle,
    val button: TextStyle,
    val buttonSmall: TextStyle,
    val switchLabel: TextStyle,
    val pill: TextStyle,
    val muted: TextStyle,
    val tiny: TextStyle,
    val tab: TextStyle
)

private val Scale = TypeScale(
    h2 = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.16).sp),
    h3 = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight(600)),
    body = TextStyle(fontSize = 15.sp, lineHeight = 21.8.sp),
    brand = TextStyle(fontSize = 15.sp, lineHeight = 21.8.sp, fontWeight = FontWeight(650), letterSpacing = (-0.15).sp),
    button = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight(570)),
    buttonSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight(570)),
    switchLabel = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight(640)),
    pill = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp, fontWeight = FontWeight(550)),
    muted = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    tiny = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    tab = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight(560))
)

object Radius {
    val s = 10.dp
    val m = 14.dp
    val l = 20.dp
}

val ShapeS = RoundedCornerShape(Radius.s)
val ShapeM = RoundedCornerShape(Radius.m)
val ShapeL = RoundedCornerShape(Radius.l)
val ShapeSheet = RoundedCornerShape(topStart = Radius.l, topEnd = Radius.l)
val ShapePill = RoundedCornerShape(percent = 50)

object Motion {
    val ease = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
    val easeSheet = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    const val FAST = 130
    const val NORMAL = 240
    const val SHEET = 420
}

object Space {
    val hairline = 1.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val touch = 44.dp
}

const val PressedScale = 0.96f
const val PressedOpacity = 0.55f
const val DisabledOpacity = 0.45f

private val LocalPalette = staticCompositionLocalOf { DarkPalette }
private val LocalTypeScale = staticCompositionLocalOf { Scale }

object Design {
    val colors: Palette
        @Composable @ReadOnlyComposable get() = LocalPalette.current
    val type: TypeScale
        @Composable @ReadOnlyComposable get() = LocalTypeScale.current
}

@Composable
fun AppUpdaterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalTypeScale provides Scale,
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = palette.accent,
            backgroundColor = palette.accent.copy(alpha = SELECTION_ALPHA)
        ),
        content = content
    )
}

private const val SELECTION_ALPHA = 0.4f
