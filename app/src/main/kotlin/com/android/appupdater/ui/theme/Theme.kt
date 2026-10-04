package com.android.appupdater.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
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
import com.android.appupdater.data.model.ThemeMode

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
    val accentText: Color,
    val success: Color,
    val danger: Color,
    val glassEdge: Color,
    val glassRim: Color,
    val shadow: Color,
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
    accentText = Color(0xFF02A66A),
    success = Color(0xFF23C56E),
    danger = Color(0xFFFF5470),
    glassEdge = Color(0x2EFFFFFF),
    glassRim = Color(0x8C000000),
    shadow = Color(0x73000000),
    scrim = Color(0x6B04060A)
)

private val BlackPalette = DarkPalette.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0B0D),
    surface2 = Color(0xFF121319),
    surface3 = Color(0xFF1A1C23),
    border = Color(0xFF23262F),
    text = Color(0xFFF2F5FA),
    muted = Color(0xFF8D95A6),
    glassEdge = Color(0x24FFFFFF),
    glassRim = Color(0xB3000000),
    shadow = Color(0xBF000000)
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
    accentText = Color(0xFF01784D),
    success = Color(0xFF23C56E),
    danger = Color(0xFFD10022),
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
    val name: TextStyle,
    val brand: TextStyle,
    val button: TextStyle,
    val buttonSmall: TextStyle,
    val pill: TextStyle,
    val muted: TextStyle,
    val tiny: TextStyle,
    val tab: TextStyle
)

private val Medium = FontWeight(560)
private val Semibold = FontWeight(650)

private val Scale = TypeScale(
    h1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 21.sp,
        lineHeight = 30.45.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.42).sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    ),
    h2 = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 23.2.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.16).sp),
    body = TextStyle(fontFamily = FontFamily.Default, fontSize = 15.sp, lineHeight = 21.75.sp),
    name = TextStyle(fontFamily = FontFamily.Default, fontSize = 15.sp, lineHeight = 21.75.sp, fontWeight = Medium),
    brand = TextStyle(fontFamily = FontFamily.Default, fontSize = 15.sp, lineHeight = 21.75.sp, fontWeight = Semibold, letterSpacing = (-0.15).sp),
    button = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp, lineHeight = 20.3.sp, fontWeight = Medium),
    buttonSmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 13.sp, lineHeight = 18.85.sp, fontWeight = Medium),
    pill = TextStyle(fontFamily = FontFamily.Default, fontSize = 13.sp, lineHeight = 18.85.sp, fontWeight = Medium),
    muted = TextStyle(fontFamily = FontFamily.Default, fontSize = 13.sp, lineHeight = 18.85.sp),
    tiny = TextStyle(fontFamily = FontFamily.Default, fontSize = 12.sp, lineHeight = 17.4.sp),
    tab = TextStyle(fontFamily = FontFamily.Default, fontSize = 11.sp, lineHeight = 15.95.sp, fontWeight = Medium)
)

object Radius {
    val card = 30.dp
}

val ShapeCard = RoundedCornerShape(Radius.card)
val ShapeSheet = RoundedCornerShape(topStart = Radius.card, topEnd = Radius.card)
val ShapePill = RoundedCornerShape(percent = 50)
val ShapeMark = RoundedCornerShape(
    object : CornerSize {
        override fun toPx(shapeSize: Size, density: Density): Float = shapeSize.minDimension * MARK_CORNER
    }
)

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
    val control = 44.dp
    val controlSmall = 36.dp
    val row = 58.dp
}

object Glass {
    const val BAR = 0.72f
    const val TAB_BAR = 0.62f
    const val TOAST = 0.78f
    const val SHEET = 0.9f
    const val SATURATION = 1.8f
    const val SHEET_BACKDROP_SATURATION = 1.4f
    val blur = 32.dp
    val toastBlur = 18.dp
    val sheetBackdropBlur = 14.dp
    val shadowBlur = 40.dp
    val shadowOffset = 18.dp
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
    themeMode: ThemeMode,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = when {
        !darkTheme -> LightPalette
        themeMode == ThemeMode.Black -> BlackPalette
        else -> DarkPalette
    }
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
private const val MARK_CORNER = 0.2237f
