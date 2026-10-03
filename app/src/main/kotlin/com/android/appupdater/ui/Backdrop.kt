package com.android.appupdater.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toIntSize
import com.android.appupdater.ui.theme.Glass
import com.android.appupdater.ui.theme.Space

internal fun Modifier.captureBackdrop(backdrop: GraphicsLayer, background: Color): Modifier =
    drawWithContent {
        backdrop.record {
            drawRect(background)
            this@drawWithContent.drawContent()
        }
        drawLayer(backdrop)
    }

internal fun backdropEffect(radius: Float, saturation: Float): ComposeRenderEffect =
    RenderEffect.createBlurEffect(
        radius,
        radius,
        RenderEffect.createColorFilterEffect(
            ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(saturation) })
        ),
        Shader.TileMode.CLAMP
    ).asComposeRenderEffect()

private fun CacheDrawScope.topHighlight(shape: Shape): Path {
    val outline = shape.createOutline(size, layoutDirection, this)
    val edge = Path().apply { addOutline(outline) }
    val inner = Path().apply {
        addOutline(outline)
        translate(Offset(0f, Space.hairline.toPx()))
    }
    return Path().apply { op(edge, inner, PathOperation.Difference) }
}

internal fun Modifier.innerTopHighlight(shape: Shape, color: Color): Modifier =
    drawWithCache {
        val highlight = topHighlight(shape)
        onDrawWithContent {
            drawPath(highlight, color)
            drawContent()
        }
    }

@Composable
internal fun BackdropSurface(
    backdrop: GraphicsLayer,
    tint: Color,
    shape: Shape,
    blurRadius: Dp,
    modifier: Modifier = Modifier,
    highlight: Color = Color.Transparent,
    observe: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {}
) {
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val effect = remember(blurRadius, density) {
        backdropEffect(with(density) { blurRadius.toPx() }, Glass.SATURATION)
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .drawWithCache {
                val outline = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
                val edge = topHighlight(shape)
                onDrawBehind {
                    observe()
                    layer.renderEffect = effect
                    layer.record(this, layoutDirection, size.toIntSize()) {
                        translate(-origin.x, -origin.y) { drawLayer(backdrop) }
                    }
                    clipPath(outline) {
                        drawLayer(layer)
                        drawRect(tint)
                    }
                    drawPath(edge, highlight)
                }
            },
        content = content
    )
}
