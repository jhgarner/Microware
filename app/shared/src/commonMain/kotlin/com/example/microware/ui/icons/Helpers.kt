package com.example.microware.ui.icons

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.DpRect

fun DrawScope.fromPercents(rect: Rect): Rect = Rect(
    left = rect.left * size.width,
    right = rect.right * size.width,
    top = rect.top * size.height,
    bottom = rect.bottom * size.height,
)

fun DrawScope.fromPercents(offset: Offset): Offset = Offset(
    x = offset.x * size.width,
    y = offset.y * size.height,
)

fun DrawScope.fromPercents(percents: Size): Size = Size(
    width = percents.width * size.width,
    height = percents.height * size.height,
)

fun DrawScope.within(rect: DpRect, inner: DrawScope.() -> Unit) = within(rect.toRect(), inner)

fun DrawScope.within(rect: Rect, inner: DrawScope.() -> Unit) {
    translate(left = rect.left, top = rect.top) {
        inset(
            left = 0f,
            top = 0f,
            right = size.width - rect.width,
            bottom = size.height - rect.height,
        ) {
            clipRect(0f, 0f, rect.width, rect.height) {
                inner()
            }
        }
    }
}
