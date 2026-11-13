package com.example.microware.ui.icons

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import com.example.microware.Palette
import kotlin.math.min

/** Draws [number] as large as the canvas allows. */
internal fun DrawScope.drawNumberIcon(number: Int, textMeasurer: TextMeasurer) {
    val text = textMeasurer.measure(
        number.toString(),
        TextStyle(
            color = Palette.Black.color,
            fontSize = size.height.toSp(),
            fontWeight = FontWeight.Bold,
        ),
    )
    val fit = min(size.width / text.size.width, size.height / text.size.height)
    scale(fit) { drawText(text, topLeft = center - text.size.center.toOffset()) }
}

@Preview
@Composable
fun NumberIconsPreview() {
    val textMeasurer = rememberTextMeasurer()
    Row {
        for (number in listOf(1, 2, 37, 100)) {
            Icon { drawNumberIcon(number, textMeasurer) }
        }
    }
}
