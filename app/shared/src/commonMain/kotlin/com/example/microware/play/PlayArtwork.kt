package com.example.microware.play

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.example.microware.Palette
import com.example.microware.ui.icons.within
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun PlayNumber(value: Int, modifier: Modifier, description: String) {
    Canvas(modifier.semantics {
        contentDescription = description
        stateDescription = value.toString()
    }) { drawPlayNumber(value) }
}

private val digitSegments = listOf("abcdef", "bc", "abdeg", "abcdg", "bcfg", "acdfg", "acdefg", "abc", "abcdefg", "abcdfg")
private val segments = listOf(
    'a' to (Offset(.2f, .12f) to Offset(.8f, .12f)),
    'b' to (Offset(.86f, .2f) to Offset(.86f, .44f)),
    'c' to (Offset(.86f, .56f) to Offset(.86f, .8f)),
    'd' to (Offset(.2f, .88f) to Offset(.8f, .88f)),
    'e' to (Offset(.14f, .56f) to Offset(.14f, .8f)),
    'f' to (Offset(.14f, .2f) to Offset(.14f, .44f)),
    'g' to (Offset(.2f, .5f) to Offset(.8f, .5f)),
)

private fun DrawScope.drawPlayNumber(value: Int) {
    val digits = value.toString()
    val height = min(size.height, size.width / (digits.length * .65f))
    val width = height * .55f
    val spacing = height * .1f
    val origin = Offset((size.width - digits.length * (width + spacing) + spacing) / 2, (size.height - height) / 2)
    digits.forEachIndexed { index, digit ->
        within(Rect(origin + Offset(index * (width + spacing), 0f), Size(width, height))) {
            for ((name, points) in segments) {
                if (name in digitSegments[digit.digitToInt()]) {
                    drawLine(
                        Palette.Navy.color,
                        Offset(points.first.x * size.width, points.first.y * size.height),
                        Offset(points.second.x * size.width, points.second.y * size.height),
                        strokeWidth = size.width * .16f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

internal fun DrawScope.drawLife(filled: Boolean) {
    val heart = Path().apply {
        moveTo(size.width * .5f, size.height * .88f)
        cubicTo(-size.width * .18f, size.height * .35f, size.width * .13f, -size.height * .1f, size.width * .5f, size.height * .25f)
        cubicTo(size.width * .87f, -size.height * .1f, size.width * 1.18f, size.height * .35f, size.width * .5f, size.height * .88f)
        close()
    }
    drawPath(heart, if (filled) Palette.Red.color else Palette.LightGray.color)
    drawPath(heart, Palette.Navy.color, style = Stroke(size.minDimension * .06f))
}

internal fun DrawScope.drawPauseIcon() {
    for (x in listOf(.28f, .60f)) {
        drawRect(Palette.Navy.color, Offset(size.width * x, size.height * .22f), Size(size.width * .14f, size.height * .56f))
    }
}

internal fun DrawScope.drawReplayIcon(t: Float) {
    rotate(-t * 360f) {
        drawArc(Palette.Cyan.color, 35f, 280f, false, Offset(size.width * .18f, size.height * .18f), Size(size.width * .64f, size.height * .64f), style = Stroke(size.minDimension * .10f, cap = StrokeCap.Round))
        val arrow = Path().apply {
            moveTo(size.width * .68f, size.height * .10f)
            lineTo(size.width * .92f, size.height * .34f)
            lineTo(size.width * .60f, size.height * .40f)
            close()
        }
        drawPath(arrow, Palette.Navy.color)
    }
}

internal fun DrawScope.drawSpeedIcon(t: Float) {
    val pulse = 1f + sin(t * PI.toFloat()) * .12f
    scale(pulse) {
        for (x in listOf(.18f, .5f)) {
            drawLine(Palette.Orange.color, Offset(size.width * x, size.height * .24f), Offset(size.width * (x + .23f), size.height * .5f), size.minDimension * .10f, StrokeCap.Round)
            drawLine(Palette.Orange.color, Offset(size.width * (x + .23f), size.height * .5f), Offset(size.width * x, size.height * .76f), size.minDimension * .10f, StrokeCap.Round)
        }
    }
}
