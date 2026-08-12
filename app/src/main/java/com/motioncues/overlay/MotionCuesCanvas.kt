package com.motioncues.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.motioncues.sensor.MotionCueVector
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ScatterDot(
    val angle: Float,
    val radiusRatio: Float,
    val sizeJitter: Float,
    val alphaJitter: Float
)

private fun generateScatterCloud(count: Int, seed: Long = 42L): List<ScatterDot> {
    val random = Random(seed)
    val innerBound = 0.60f
    val outerBound = 0.99f
    return List(count) {
        val angle = random.nextFloat() * (2 * Math.PI.toFloat())
        val t = random.nextFloat().toDouble().let { Math.pow(it, 0.6) }.toFloat()
        val radiusRatio = innerBound + (outerBound - innerBound) * t
        ScatterDot(
            angle = angle,
            radiusRatio = radiusRatio,
            sizeJitter = 0.6f + random.nextFloat() * 0.8f,
            alphaJitter = 0.5f + random.nextFloat() * 0.5f
        )
    }
}

@Composable
fun MotionCuesOverlay(motionCueFlow: StateFlow<MotionCueVector>) {
    val cue by motionCueFlow.collectAsState()

    val animatedLateral = remember { Animatable(0f) }
    val animatedLongitudinal = remember { Animatable(0f) }
    val animatedYaw = remember { Animatable(0f) }
    var animatedIntensity by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(cue.lateralOffset) {
        animatedLateral.animateTo(cue.lateralOffset, tween(durationMillis = 90))
    }
    LaunchedEffect(cue.longitudinalOffset) {
        animatedLongitudinal.animateTo(cue.longitudinalOffset, tween(durationMillis = 90))
    }
    LaunchedEffect(cue.yawRate) {
        animatedYaw.animateTo(cue.yawRate, tween(durationMillis = 70))
    }
    LaunchedEffect(cue.intensity) {
        animatedIntensity = cue.intensity
    }

    val scatterCloud = remember { generateScatterCloud(count = 260) }

    Canvas(
        modifier = Modifier
            .background(Color.Transparent)
    ) {
        drawScatterCloud(
            dots = scatterCloud,
            lateral = animatedLateral.value,
            longitudinal = animatedLongitudinal.value,
            yaw = animatedYaw.value,
            intensity = animatedIntensity
        )
    }
}

private fun DrawScope.drawScatterCloud(
    dots: List<ScatterDot>,
    lateral: Float,
    longitudinal: Float,
    yaw: Float,
    intensity: Float
) {
    val w = size.width
    val h = size.height
    val centerX = w / 2f
    val centerY = h / 2f
    val radiusX = w / 2f
    val radiusY = h / 2f
    val baseDotRadius = 2.2f + intensity * 1.8f

    val shiftX = -lateral * radiusX * 0.10f
    val shiftY = -longitudinal * radiusY * 0.10f
    val rotationRad = -yaw * 0.25f

    for (dot in dots) {
        val angle = dot.angle + rotationRad
        val baseX = centerX + radiusX * dot.radiusRatio * cos(angle)
        val baseY = centerY + radiusY * dot.radiusRatio * sin(angle)

        val x = baseX + shiftX
        val y = baseY + shiftY

        val alpha = ((0.10f + intensity * 0.5f) * dot.alphaJitter).coerceIn(0.06f, 0.75f)

        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = baseDotRadius * dot.sizeJitter,
            center = Offset(x, y)
        )
    }
}
