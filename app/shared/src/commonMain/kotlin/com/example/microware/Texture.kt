package com.example.microware

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

val transparent = generateTransparency()
val transparentBrush = ShaderBrush(
    ImageShader(
        image = transparent,
        tileModeX = TileMode.Repeated,
        tileModeY = TileMode.Repeated
    )
)

fun tiled(bitmap: ImageBitmap): ShaderBrush {
    return ShaderBrush(
        ImageShader(
            image = bitmap,
            tileModeX = TileMode.Repeated,
            tileModeY = TileMode.Repeated
        )
    )
}

fun scaled(bitmap: ImageBitmap, width: Int, height: Int): ImageBitmap {
    val scaled = ImageBitmap(width, height)
    val canvas = Canvas(scaled)
    val paint = Paint().apply { filterQuality = FilterQuality.None }
    canvas.drawImageRect(
        image = bitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bitmap.width, bitmap.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(width, height),
        paint = paint
    )
    return scaled
}

private fun generateTransparency(): ImageBitmap {
    val bitmap = ImageBitmap(2, 2)
    val canvas = Canvas(bitmap)
    val light = Paint().apply {
        color = Color(0XFFa2a2a2)
        style = PaintingStyle.Fill
    }
    val dark = Paint().apply {
        color = Color(0XFF919191)
        style = PaintingStyle.Fill
    }
    canvas.drawRect(0f, 0f, 1f, 1f, light)
    canvas.drawRect(1f, 0f, 2f, 1f, dark)
    canvas.drawRect(0f, 1f, 1f, 2f, dark)
    canvas.drawRect(1f, 1f, 2f, 2f, light)
    return bitmap
}
