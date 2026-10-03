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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.android.appupdater.ui.theme.Space

private val BlurRadius = 18.dp
private const val Saturation = 1.8f

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

@Composable
internal fun BackdropSurface(
    backdrop: GraphicsLayer,
    tint: Color,
    shape: Shape,
    edgeHighlight: Color,
    modifier: Modifier = Modifier,
    blurRadius: Dp = BlurRadius,
    observe: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {}
) {
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val effect = remember(blurRadius, density) {
        backdropEffect(with(density) { blurRadius.toPx() }, Saturation)
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .clip(shape)
            .drawBehind {
                observe()
                layer.renderEffect = effect
                layer.record(this, layoutDirection, size.toIntSize()) {
                    translate(-origin.x, -origin.y) { drawLayer(backdrop) }
                }
                drawLayer(layer)
                drawRect(tint)
                drawRect(
                    color = edgeHighlight,
                    size = Size(size.width, Space.hairline.toPx())
                )
            },
        content = content
    )
}
