package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import kotlin.random.Random

private val dots: List<Offset> = mutableListOf<Offset>().apply {
    val rand = Random(11)
    for (x in 0..<20) {
        for (y in 0..<20) {
            if (rand.nextFloat() > .6f) {
                add(Offset(x / 20f, y / 20f))
            }
        }
    }
}

fun DrawScope.drawScanner() {
    drawCamera()
    within(fromPercents(Rect(Offset(x = .35f, y = .40f), Size(width = .3f, height = .3f)))) {
        drawQrCode()
    }
}

fun DrawScope.drawCamera() {
    drawRoundRect(
        Palette.LightGray.color,
        topLeft = fromPercents(Offset(.39f, .15f)),
        size = fromPercents(Size(.2f, .2f)),
        cornerRadius = CornerRadius((.05f * size.width), (.05f * size.height)),
    )
    drawRoundRect(
        Palette.Yellow.color,
        topLeft = fromPercents(Offset(.44f, .19f)),
        size = fromPercents(Size(.1f, .05f)),
        cornerRadius = CornerRadius((.1f * size.width), (.10f * size.height)),
    )
    drawRoundRect(
        Palette.Navy.color,
        topLeft = fromPercents(Offset(.18f, .20f)),
        size = fromPercents(Size(.15f, .1f)),
        cornerRadius = CornerRadius((.05f * size.width), (.05f * size.height)),
    )
    drawRoundRect(
        Palette.LightGray.color,
        topLeft = fromPercents(Offset(.05f, .25f)),
        size = fromPercents(Size(.9f, .6f)),
        cornerRadius = CornerRadius((.1f * size.width), (.1f * size.height)),
    )
    drawRoundRect(
        Palette.Navy.color,
        topLeft = fromPercents(Offset(.05f, .35f)),
        size = fromPercents(Size(.9f, .4f)),
        cornerRadius = CornerRadius((.0f * size.width), (.0f * size.height)),
    )
    drawCircle(
        Palette.LightGray.color,
        center = fromPercents(Offset(.5f, .55f)),
        radius = .24f * size.width,
    )
    drawCircle(
        Palette.OffWhite.color,
        center = fromPercents(Offset(.5f, .55f)),
        radius = .22f * size.width,
    )
}

fun DrawScope.drawQrCode() {
    dots.forEach {
        drawRect(
            Palette.Navy.color,
            topLeft = fromPercents(it),
            size = fromPercents(Size(.05f, .05f)),
        )
    }
    drawTracker(Offset.Zero)
    drawTracker(Offset(.7f, 0f))
    drawTracker(Offset(0f, .7f))
}

private fun DrawScope.drawTracker(start: Offset) {
    drawRect(
        Palette.Navy.color,
        topLeft = fromPercents(Offset.Zero + start),
        size = fromPercents(Size(.3f, .3f)),
    )
    drawRect(
        Palette.OffWhite.color,
        topLeft = fromPercents(Offset(.05f, .05f) + start),
        size = fromPercents(Size(.2f, .2f)),
    )
    drawRect(
        Palette.Navy.color,
        topLeft = fromPercents(Offset(.1f, .1f) + start),
        size = fromPercents(Size(.1f, .1f)),
    )
}

@Preview
@Composable
private fun CameraIconPreview() = Icon { drawCamera() }

@Preview
@Composable
private fun QrIconPreview() = Icon { drawQrCode() }

@Preview
@Composable
private fun ScannerIconPreview() = Icon(size = 200.dp) { drawScanner() }
