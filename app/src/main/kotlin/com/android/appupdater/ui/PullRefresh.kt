package com.android.appupdater.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import com.android.appupdater.ui.theme.Motion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

internal val PullBand = 60.dp
internal val PullSpinnerSize = 30.dp
internal val LoadingSpinnerSize = 32.dp
private val PullStart = 40.dp
private val PullTrigger = 150.dp
private const val RUBBER = 0.55f
private const val SPOKES = 8
private const val SPOKE_INNER = 0.36f
private const val SPOKE_WIDTH = 0.23f
private const val SPOKE_FADE = 0.135f
private const val FORM_STEP = 0.25f
private const val HALF_TURN = 180f
private const val FULL_TURN = 360f
private const val SPIN_STEP = Motion.SPIN / SPOKES
private const val LOADING_STEP_MILLIS = 100L
private const val LOADING_TAIL = 4
private const val LOADING_FLOOR = 0.41f

internal class PullRefreshState(
    private val scope: CoroutineScope,
    density: Density,
    private val atTop: () -> Boolean,
    private val onRefresh: () -> Unit
) : NestedScrollConnection {
    private val band = with(density) { PullBand.toPx() }
    private val start = with(density) { PullStart.toPx() }
    private val trigger = with(density) { PullTrigger.toPx() }
    var touching = false
    private var dragging = false
    private var fired = false
    private var pull = 0f
    private var animation: Job? = null

    var offset by mutableFloatStateOf(0f)
        private set
    var refreshing by mutableStateOf(false)
        private set
    private var forming by mutableStateOf(false)
    var enabled = false
    var extent = 1f

    val progress: Float
        get() = if (forming) ((offset - start) / (trigger - start)).coerceIn(0f, 1f) else 0f

    val lift: Float
        get() = if (refreshing) min(0f, offset - band) else 0f

    private val base: Float
        get() = if (refreshing) band else 0f

    fun update(refreshing: Boolean) {
        if (refreshing == this.refreshing) return
        this.refreshing = refreshing
        forming = false
        if (dragging) {
            pull = unstretch(max(0f, offset - base))
        } else if (!refreshing || atTop()) {
            settle(base, Motion.SPRING)
        }
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && touching) grab()
        if (available.y >= 0f || offset <= 0f) return Offset.Zero
        var remaining = available.y
        var value = offset
        if (dragging && value > base) {
            val next = max(0f, pull + remaining)
            remaining -= next - pull
            pull = next
            value = base + stretch(pull)
        }
        if (refreshing && value <= band && remaining < 0f) {
            val next = max(0f, value + remaining)
            remaining -= next - value
            value = next
        }
        if (remaining == available.y) return Offset.Zero
        move(value)
        return Offset(0f, available.y - remaining)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y <= 0f) return Offset.Zero
        var remaining = available.y
        var value = offset
        if (refreshing && value < band) {
            val next = min(band, value + remaining)
            remaining -= next - value
            value = next
        }
        if (remaining > 0f && dragging && enabled) {
            pull += remaining
            remaining = 0f
            value = base + stretch(pull)
            if (!refreshing && !fired && value >= trigger) {
                fired = true
                onRefresh()
            }
        }
        if (remaining == available.y) return Offset.Zero
        move(value)
        return Offset(0f, available.y - remaining)
    }

    fun release() {
        dragging = false
        fired = false
        pull = 0f
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!dragging) return Velocity.Zero
        release()
        if (offset <= base) return Velocity.Zero
        settle(base, Motion.SPRING * 2)
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (refreshing && offset < band && available.y > 0f) settle(band, Motion.SPRING)
        return Velocity.Zero
    }

    private fun grab() {
        if (dragging) return
        dragging = true
        animation?.cancel()
        pull = unstretch(max(0f, offset - base))
        if (!refreshing) forming = true
    }

    private fun move(value: Float) {
        animation?.cancel()
        offset = value
    }

    private fun settle(target: Float, duration: Int) {
        animation?.cancel()
        animation = scope.launch {
            animate(offset, target, animationSpec = tween(duration, easing = Motion.easeSheet)) { value, _ -> offset = value }
        }
    }

    private fun stretch(distance: Float) = extent * (1f - 1f / (distance * RUBBER / extent + 1f))

    private fun unstretch(value: Float) = extent * (1f / (1f - value / extent) - 1f) / RUBBER
}

internal fun Modifier.pullRefresh(state: PullRefreshState): Modifier =
    pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                state.touching = awaitPointerEvent(PointerEventPass.Initial).changes.fastAny { it.pressed }
            }
        }
    }.nestedScroll(state)

@Composable
internal fun rememberPullRefreshState(atTop: () -> Boolean, onRefresh: () -> Unit): PullRefreshState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    return remember(scope, density) { PullRefreshState(scope, density, atTop, onRefresh) }
}

@Composable
internal fun RefreshIndicator(
    color: Color,
    spinning: Boolean,
    progress: () -> Float,
    modifier: Modifier = Modifier
) {
    val clock = remember { mutableLongStateOf(0L) }

    LaunchedEffect(spinning) {
        clock.longValue = 0L
        if (spinning) {
            val origin = withFrameMillis { it }
            while (true) {
                val time = withFrameMillis { it } - origin
                clock.longValue = time
                if (time >= Motion.SPIN) delay(SPIN_STEP - time % SPIN_STEP)
            }
        }
    }

    Canvas(modifier = modifier) {
        val time = clock.longValue
        val turn = min(1f, time.toFloat() / Motion.SPIN)
        val settle = turn * (2f - turn)
        val head = (time / SPIN_STEP).toInt()
        val formed = if (spinning) 1f else progress()
        val lead = 1f - (1f - formed) * (1f - formed)
        val step = FORM_STEP * min(1f, 2f * (1f - formed))
        rotate(HALF_TURN * settle) {
            spokes(color) { index ->
                if (spinning) {
                    1f - (head - index).mod(SPOKES) * SPOKE_FADE * settle
                } else {
                    (lead - index * step).coerceIn(0f, 1f)
                }
            }
        }
    }
}

@Composable
internal fun LoadingIndicator(color: Color, modifier: Modifier = Modifier) {
    val head = remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(LOADING_STEP_MILLIS)
            head.intValue = (head.intValue + 1) % SPOKES
        }
    }

    Canvas(modifier = modifier) {
        spokes(color) { index ->
            val trail = (head.intValue - index).mod(SPOKES)
            if (trail < LOADING_TAIL) 1f - trail * SPOKE_FADE else LOADING_FLOOR
        }
    }
}

private inline fun DrawScope.spokes(color: Color, alpha: (Int) -> Float) {
    val radius = size.minDimension / 2
    val width = radius * SPOKE_WIDTH
    repeat(SPOKES) { index ->
        val value = alpha(index)
        if (value > 0f) {
            rotate(index * FULL_TURN / SPOKES) {
                drawLine(
                    color = color.copy(alpha = color.alpha * value),
                    start = Offset(center.x, center.y - radius * SPOKE_INNER - width / 2),
                    end = Offset(center.x, center.y - radius + width / 2),
                    strokeWidth = width,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
