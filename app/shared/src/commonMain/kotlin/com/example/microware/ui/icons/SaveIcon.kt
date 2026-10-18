package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import com.example.microware.Palette


fun DrawScope.drawSaveIcon(t: Float = 0f) {
    val radius = .1f * size.minDimension;
    drawRoundRect(
        Palette.Navy.color,
        cornerRadius = CornerRadius(radius, radius),
        topLeft = fromPercents(Offset(.1f, .1f)),
        size = fromPercents(Size(.8f, .8f)),
    )

    drawRect(
        Palette.OffWhite.color,
        topLeft = fromPercents(Offset(.3f, .52f)),
        size = fromPercents(Size(.4f, .3f)),
    )
    drawRect(
        Palette.OffWhite.color,
        topLeft = fromPercents(Offset(.3f, .17f)),
        size = fromPercents(Size(.4f, .25f)),
    )

    drawRect(
        Palette.Navy.color,
        topLeft = fromPercents(Offset(.5f, .19f)),
        size = fromPercents(Size(.1f, .2f)),
    )
    rotate(45f) {
        drawRect(
            Palette.OffWhite.color,
            topLeft = fromPercents(Offset(.3f, -.40f)),
            size = fromPercents(Size(.45f, .45f)),
        )
    }
}

@Preview
@Composable
private fun CreateCommunityIconPreview() = Icon { drawSaveIcon() }
