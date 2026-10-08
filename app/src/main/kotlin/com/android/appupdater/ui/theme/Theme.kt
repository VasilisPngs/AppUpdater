package com.android.appupdater.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class Palette(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val fill: Color,
    val text: Color,
    val muted: Color,
    val chevron: Color,
    val accent: Color,
    val accentSoft: Color,
    val success: Color,
    val danger: Color,
    val tabLens: Color,
    val glassEdge: Color,
    val glassRim: Color,
    val shadow: Color,
    val scrim: Color
)

private val DarkAccent = Color(0xFF0091FF)
private val LightAccent = Color(0xFF0088FF)

private val DarkPalette = Palette(
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surface2 = Color(0xFF2C2C2E),
    surface3 = Color(0xFF3A3A3C),
    fill = Color(0x3D767680),
    text = Color(0xFFFFFFFF),
    muted = Color(0x99EBEBF5),
    chevron = Color(0xFF98989D),
    accent = DarkAccent,
    accentSoft = DarkAccent.copy(alpha = ACCENT_SOFT_DARK),
    success = Color(0xFF30D158),
    danger = Color(0xFFFF4245),
    tabLens = Color(0xB3000000),
    glassEdge = Color(0x2EFFFFFF),
    glassRim = Color(0x8C000000),
    shadow = Color(0x73000000),
    scrim = Color(0x6B04060A)
)

private val LightPalette = Palette(
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF2F2F7),
    surface3 = Color(0xFFD1D1D6),
    fill = Color(0x1F767680),
    text = Color(0xFF000000),
    muted = Color(0x993C3C43),
    chevron = Color(0xFF8A8A8E),
    accent = LightAccent,
    accentSoft = LightAccent.copy(alpha = ACCENT_SOFT_LIGHT),
    success = Color(0xFF34C759),
    danger = Color(0xFFFF383C),
    tabLens = Color(0x13000000),
    glassEdge = Color(0xD9FFFFFF),
    glassRim = Color(0x24121828),
    shadow = Color(0x1F121828),
    scrim = Color(0x6B04060A)
)

@Immutable
data class TypeScale(
    val h1: TextStyle,
    val h2: TextStyle,
    val body: TextStyle,
    val barTitle: TextStyle,
    val button: TextStyle,
    val buttonSmall: TextStyle,
    val muted: TextStyle,
    val tab: TextStyle
)

private val Medium = FontWeight(560)
private val Semibold = FontWeight(650)

private fun text(size: Float, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size.sp,
    lineHeight = (size * LINE_HEIGHT).sp,
    fontWeight = weight,
    letterSpacing = (size * tracking).sp
)

private val Scale = TypeScale(
    h1 = text(LARGE_TITLE, FontWeight.Bold).copy(
        lineHeight = (LARGE_TITLE * TITLE_LINE_HEIGHT).sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    ),
    h2 = text(BODY, Semibold, TIGHT_TRACKING),
    body = text(BODY),
    barTitle = text(BODY, Semibold),
    button = text(BODY, Medium),
    buttonSmall = text(SUBHEAD, Medium),
    muted = text(FOOTNOTE),
    tab = text(TAB, Medium).copy(lineHeight = TAB.sp)
)

object Radius {
    val card = 30.dp
}

val ShapeCard = RoundedCornerShape(Radius.card)
val ShapePill = RoundedCornerShape(percent = 50)
val ShapeIcon = RoundedCornerShape(
    object : CornerSize {
        override fun toPx(shapeSize: Size, density: Density): Float = shapeSize.minDimension * ICON_CORNER
    }
)

private class SampledEasing(private vararg val samples: Float) : Easing {
    override fun transform(fraction: Float): Float {
        if (fraction >= 1f) return 1f
        val position = fraction.coerceAtLeast(0f) * (samples.size - 1)
        val index = position.toInt()
        return samples[index] + (samples[index + 1] - samples[index]) * (position - index)
    }
}

object Motion {
    val ease = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
    val easeSheet: Easing = SampledEasing(
        0f, 0.05648f, 0.1775f, 0.3165f, 0.4499f, 0.5674f, 0.6656f, 0.745f, 0.8077f, 0.8562f, 0.8933f, 0.9213f, 0.9423f,
        0.9579f, 0.9694f, 0.9778f, 0.984f, 0.9885f, 0.9917f, 0.9941f, 0.9958f, 0.997f, 0.9978f, 0.9985f, 0.9989f
    )
    val easeSpring: Easing = SampledEasing(
        0f, 0.05029f, 0.1659f, 0.3077f, 0.4513f, 0.5827f, 0.6951f, 0.7866f, 0.8577f, 0.911f, 0.9492f, 0.9754f, 0.9924f,
        1.003f, 1.008f, 1.011f, 1.011f, 1.01f, 1.009f, 1.007f, 1.006f, 1.004f, 1.003f, 1.002f, 1.001f
    )
    const val FAST = 130
    const val NORMAL = 240
    const val SPRING = 400
    const val SHEET = 480
    const val SPIN = 1000
}

object Space {
    val hairline = 1.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val control = 44.dp
    val controlSmall = 36.dp
    val row = 52.dp
}

object Glass {
    const val BAR = 0.72f
    const val MATERIAL = 0.73f
    const val TOAST = 0.78f
    const val SHEET = 0.9f
    const val SATURATION = 1.8f
    const val SHEET_BACKDROP_SATURATION = 1.4f
    val blur = 32.dp
    val toastBlur = 18.dp
    val sheetBackdropBlur = 14.dp
    val shadowBlur = 40.dp
    val shadowOffset = 18.dp
    val edgeFade = 24.dp
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
private const val ACCENT_SOFT_DARK = 0.18f
private const val ACCENT_SOFT_LIGHT = 0.14f
private const val LARGE_TITLE = 34f
private const val BODY = 17f
private const val SUBHEAD = 15f
private const val FOOTNOTE = 13f
private const val TAB = 10f
private const val LINE_HEIGHT = 1.3f
private const val TITLE_LINE_HEIGHT = 1.15f
private const val TIGHT_TRACKING = -0.01f
private const val ICON_CORNER = 0.2237f
