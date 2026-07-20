package com.kami.gamelist.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max

private val SplashBg = Color(0xFF1A1A2E)

@Composable
fun AnimatedSplashScreen() {
    val centerAlpha = remember { Animatable(0f) }
    val leftAlpha = remember { Animatable(0f) }
    val rightAlpha = remember { Animatable(0f) }
    val barAlpha = remember { Animatable(0f) }

    val infinite = rememberInfiniteTransition()

    val centerFloat by infinite.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse)
    )
    val leftFloat by infinite.animateFloat(
        initialValue = 0f, targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse)
    )
    val rightFloat by infinite.animateFloat(
        initialValue = 0f, targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse)
    )
    val shinePhase by infinite.animateFloat(
        initialValue = -0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine))
    )
    val shimmerX by infinite.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing))
    )

    LaunchedEffect(Unit) {
        centerAlpha.animateTo(1f, tween(350, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        delay(120)
        leftAlpha.animateTo(1f, tween(350, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        delay(240)
        rightAlpha.animateTo(1f, tween(350, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        delay(500)
        barAlpha.animateTo(1f, tween(300))
    }

    Box(
        modifier = Modifier.fillMaxSize().background(SplashBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val s = size.width / 512f

                drawCrystalGroup(
                    s = s,
                    alpha = centerAlpha.value,
                    floatY = centerFloat * s,
                    shinePhase = shinePhase,
                    faces = centerFaces
                )

                drawCrystalGroup(
                    s = s,
                    alpha = leftAlpha.value,
                    floatY = leftFloat * s,
                    shinePhase = shinePhase,
                    faces = leftFaces
                )

                drawCrystalGroup(
                    s = s,
                    alpha = rightAlpha.value,
                    floatY = rightFloat * s,
                    shinePhase = shinePhase,
                    faces = rightFaces
                )
            }

            Spacer(Modifier.height(40.dp))

            Canvas(
                modifier = Modifier
                    .width(100.dp)
                    .height(2.5.dp)
                    .graphicsLayer { alpha = barAlpha.value }
            ) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF5599FF).copy(alpha = 0.5f),
                            Color(0xFF9966FF).copy(alpha = 0.8f),
                            Color(0xFF5599FF).copy(alpha = 0.5f),
                            Color.Transparent,
                        ),
                        start = Offset(size.width * shimmerX, 0f),
                        end = Offset(size.width * (shimmerX + 0.5f), 0f)
                    ),
                    cornerRadius = CornerRadius(4f),
                    size = size
                )
            }
        }
    }
}

private data class CrystalFace(
    val p1x: Float, val p1y: Float,
    val p2x: Float, val p2y: Float,
    val p3x: Float, val p3y: Float,
    val color: Color,
    val shineStrength: Float = 0.35f
) {
    val centerDiag: Float = ((p1x + p2x + p3x) / 3f + (p1y + p2y + p3y) / 3f) / 1024f
}

private val centerFaces = listOf(
    CrystalFace(256f, 78f, 173f, 286f, 256f, 256.286f, Color(0xFFCCEBFF), shineStrength = 0.5f),
    CrystalFace(256f, 78f, 339f, 286f, 256f, 256.286f, Color(0xFFEBD6FF), shineStrength = 0.45f),
    CrystalFace(173f, 285.667f, 256f, 434f, 256f, 256f, Color(0xFF5599FF), shineStrength = 0.3f),
    CrystalFace(339f, 285.667f, 256f, 434f, 256f, 256f, Color(0xFF9966FF), shineStrength = 0.3f),
)

private val leftFaces = listOf(
    CrystalFace(128f, 197f, 69f, 316f, 113.25f, 286.25f, Color(0xFFAEE2FF), shineStrength = 0.5f),
    CrystalFace(128f, 197f, 143f, 301f, 113f, 286.143f, Color(0xFF77B3FF), shineStrength = 0.35f),
    CrystalFace(69f, 315.75f, 158f, 405f, 113.5f, 286f, Color(0xFF4477DD), shineStrength = 0.25f),
    CrystalFace(143f, 300.875f, 158f, 405f, 113f, 286f, Color(0xFF3355AA), shineStrength = 0.2f),
)

private val rightFaces = listOf(
    CrystalFace(384f, 197f, 443f, 316f, 398.75f, 286.25f, Color(0xFFC299FF), shineStrength = 0.45f),
    CrystalFace(384f, 197f, 369f, 301f, 399f, 286.143f, Color(0xFFE2C2FF), shineStrength = 0.5f),
    CrystalFace(443f, 315.75f, 354f, 405f, 398.5f, 286f, Color(0xFF9966FF), shineStrength = 0.3f),
    CrystalFace(369f, 300.875f, 354f, 405f, 399f, 286f, Color(0xFF7744DD), shineStrength = 0.2f),
)

private fun DrawScope.drawCrystalGroup(
    s: Float,
    alpha: Float,
    floatY: Float,
    shinePhase: Float,
    faces: List<CrystalFace>
) {
    if (alpha <= 0.01f) return

    withTransform({
        translate(top = floatY)
    }) {
        faces.forEach { face ->
            val path = Path().apply {
                moveTo(face.p1x * s, face.p1y * s)
                lineTo(face.p2x * s, face.p2y * s)
                lineTo(face.p3x * s, face.p3y * s)
                close()
            }
            drawPath(path, face.color.copy(alpha = alpha))

            val dist = abs(face.centerDiag - shinePhase)
            val shine = max(0f, 1f - dist * 6f) * face.shineStrength * alpha
            if (shine > 0.01f) {
                drawPath(path, Color.White.copy(alpha = shine))
            }
        }
    }
}
