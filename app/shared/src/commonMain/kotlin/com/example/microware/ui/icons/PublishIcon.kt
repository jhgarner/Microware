package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import com.example.microware.Palette

internal fun DrawScope.drawPublishIcon(t: Float = 0f) {
    val lift = animate(t) {
        anim(.12f, .35f)
        anim(0f, .35f)
        hold()
    }
    val stroke = Stroke(size.minDimension * .08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawRect(
        Palette.Cyan.color,
        Offset(size.width * .18f, size.height * .65f),
        Size(size.width * .64f, size.height * .24f),
    )
    val tray = Path().apply {
        moveTo(size.width * .12f, size.height * .62f)
        lineTo(size.width * .12f, size.height * .90f)
        lineTo(size.width * .88f, size.height * .90f)
        lineTo(size.width * .88f, size.height * .62f)
    }
    drawPath(tray, Palette.Black.color, style = stroke)
    translate(top = -lift * size.height) {
        val arrow = Path().apply {
            moveTo(size.width * .5f, size.height * .70f)
            lineTo(size.width * .5f, size.height * .24f)
            moveTo(size.width * .28f, size.height * .46f)
            lineTo(size.width * .5f, size.height * .24f)
            lineTo(size.width * .72f, size.height * .46f)
        }
        drawPath(arrow, Palette.Black.color, style = stroke)
    }
}

@Preview
@Composable
private fun PublishIconPreview() = Animated { drawPublishIcon(it) }
