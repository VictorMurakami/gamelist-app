package com.kami.gamelist.core.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.neonGlow(
    color: Color = Color(0xFF00E5FF),
    radius: Dp = 8.dp,
    alpha: Float = 0.25f,
    cornerRadius: Dp = 6.dp
): Modifier = this.drawBehind {
    val radiusPx = radius.toPx()
    val cornerPx = cornerRadius.toPx()

    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            this.color = color.copy(alpha = alpha).toArgb()
        }

        canvas.drawRoundRect(
            left = -radiusPx / 2,
            top = -radiusPx / 2,
            right = size.width + radiusPx / 2,
            bottom = size.height + radiusPx / 2,
            radiusX = cornerPx,
            radiusY = cornerPx,
            paint = androidx.compose.ui.graphics.Paint().apply {
                this.color = color.copy(alpha = alpha * 0.3f)
            }
        )

        canvas.drawRoundRect(
            left = -radiusPx / 4,
            top = -radiusPx / 4,
            right = size.width + radiusPx / 4,
            bottom = size.height + radiusPx / 4,
            radiusX = cornerPx,
            radiusY = cornerPx,
            paint = androidx.compose.ui.graphics.Paint().apply {
                this.color = color.copy(alpha = alpha * 0.6f)
            }
        )
    }
}
