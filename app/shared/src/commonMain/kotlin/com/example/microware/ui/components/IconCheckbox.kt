package com.example.microware.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.ui.icons.drawPlayIcon
import com.example.microware.ui.icons.drawPublishIcon

@Composable
internal fun IconCheckbox(
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .padding(4.dp)
            .background(Palette.OffWhite.color)
            .border(2.dp, Palette.Black.color)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .semantics { contentDescription = description }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Canvas(Modifier.size(40.dp)) { icon() }
        Canvas(Modifier.size(24.dp)) {
            val border = 2.dp.toPx()
            drawRect(if (checked) Palette.Green.color else Palette.OffWhite.color)
            drawRect(
                Palette.Black.color,
                topLeft = Offset(border / 2, border / 2),
                size = size.copy(width = size.width - border, height = size.height - border),
                style = Stroke(border),
            )
            if (checked) {
                val check = Path().apply {
                    moveTo(size.width * .22f, size.height * .52f)
                    lineTo(size.width * .43f, size.height * .73f)
                    lineTo(size.width * .79f, size.height * .28f)
                }
                drawPath(
                    check,
                    Palette.Black.color,
                    style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

@Preview
@Composable
internal fun IconCheckboxPreview() {
    Column {
        for (checked in listOf(true, false)) {
            Row {
                IconCheckbox("Allow publishing into experience", checked, {}, Modifier.size(104.dp, 64.dp)) {
                    drawPublishIcon()
                }
                IconCheckbox("Allow playing experience", checked, {}, Modifier.size(104.dp, 64.dp)) {
                    drawPlayIcon()
                }
            }
        }
    }
}
