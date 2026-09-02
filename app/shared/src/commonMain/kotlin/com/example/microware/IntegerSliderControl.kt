package com.example.microware

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val SliderHeight = 72.dp
private val ValueIndicatorWidth = 36.dp
private val ValueIndicatorHeight = 30.dp
private val SliderTrackY = 54.dp
private val SliderTrackThickness = 6.dp
private val SliderThumbHeight = 28.dp
private val SliderThumbWidth = 4.dp
private val SliderHorizontalPadding = 20.dp
private val IncrementButtonWidth = 36.dp
private val IncrementButtonHeight = 48.dp

enum class IntegerSliderDimension {
    Width,
    Height,
}

@Composable
fun IntegerSliderControl(
    dimension: IntegerSliderDimension,
    value: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Palette = Palette.Navy,
    inactiveColor: Palette = Palette.LightGray,
) {
    require(maxValue >= 1) { "maxValue must be at least 1" }
    val clampedValue = value.coerceIn(1, maxValue)
    val latestOnValueChange = rememberUpdatedState(onValueChange)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            modifier = Modifier.height(IncrementButtonHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DimensionIcon(
                dimension = dimension,
                color = activeColor.color,
            )
            OutlinedButton(
                onClick = { onValueChange((clampedValue - 1).coerceAtLeast(1)) },
                enabled = clampedValue > 1,
                modifier = Modifier
                    .requiredSize(IncrementButtonWidth, IncrementButtonHeight)
                    .align(Alignment.Bottom),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(
                    1.dp,
                    if (clampedValue > 1) activeColor.color else inactiveColor.color,
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = activeColor.color,
                    disabledContentColor = inactiveColor.color,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = "−",
                )
            }
        }
        IntegerSlider(
            value = clampedValue,
            maxValue = maxValue,
            onValueChange = latestOnValueChange.value,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = { onValueChange((clampedValue + 1).coerceAtMost(maxValue)) },
            enabled = clampedValue < maxValue,
            modifier = Modifier
                .requiredSize(IncrementButtonWidth, IncrementButtonHeight)
                .align(Alignment.Bottom),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(
                1.dp,
                if (clampedValue < maxValue) activeColor.color else inactiveColor.color,
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = activeColor.color,
                disabledContentColor = inactiveColor.color,
            ),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                text = "+",
            )
        }
    }
}

@Composable
private fun RowScope.DimensionIcon(
    dimension: IntegerSliderDimension,
    color: Color,
) {
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .align(Alignment.CenterVertically)
            .semantics {
                contentDescription = dimension.name
            },
    ) {
        val strokeWidth = 2.dp.toPx()
        val inset = size.minDimension * 0.2f
        val center = size.minDimension / 2f
        val arrowSize = size.minDimension * 0.18f

        if (dimension == IntegerSliderDimension.Width) {
            drawLine(
                color = color,
                start = Offset(inset, center),
                end = Offset(size.width - inset, center),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(inset, center),
                end = Offset(inset + arrowSize, center - arrowSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(inset, center),
                end = Offset(inset + arrowSize, center + arrowSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(size.width - inset, center),
                end = Offset(size.width - inset - arrowSize, center - arrowSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(size.width - inset, center),
                end = Offset(size.width - inset - arrowSize, center + arrowSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        } else {
            drawLine(
                color = color,
                start = Offset(center, inset),
                end = Offset(center, size.height - inset),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(center, inset),
                end = Offset(center - arrowSize, inset + arrowSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(center, inset),
                end = Offset(center + arrowSize, inset + arrowSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(center, size.height - inset),
                end = Offset(center - arrowSize, size.height - inset - arrowSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(center, size.height - inset),
                end = Offset(center + arrowSize, size.height - inset - arrowSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun IntegerSlider(
    value: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    activeColor: Palette,
    inactiveColor: Palette,
    modifier: Modifier = Modifier,
) {
    val horizontalPaddingPx = SliderHorizontalPadding.toPx()
    val latestOnValueChange = rememberUpdatedState(onValueChange)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(SliderHeight)
            .pointerInput(maxValue) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun updateFromPosition(position: Offset) {
                        val usableWidth = size.width - horizontalPaddingPx * 2f
                        val fraction = ((position.x - horizontalPaddingPx) / usableWidth)
                            .coerceIn(0f, 1f)
                        latestOnValueChange.value(
                            (1f + fraction * (maxValue - 1)).roundToInt()
                                .coerceIn(1, maxValue)
                        )
                    }

                    updateFromPosition(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: break
                        if (change.changedToUp()) {
                            change.consume()
                            break
                        }
                        if (change.positionChange() != Offset.Zero) {
                            change.consume()
                            updateFromPosition(change.position)
                        }
                    }
                }
            },
    ) {
        val bubbleWidthPx = ValueIndicatorWidth.toPx()
        val bubbleHeightPx = ValueIndicatorHeight.toPx()
        val trackStart = horizontalPaddingPx
        val trackEnd = maxWidth.toPx() - horizontalPaddingPx
        val fraction = (value - 1).toFloat() / (maxValue - 1)
        val thumbX = trackStart + (trackEnd - trackStart) * fraction
        val trackY = SliderTrackY.toPx()
        val trackThickness = SliderTrackThickness.toPx()
        val thumbHeight = SliderThumbHeight.toPx()
        val thumbWidth = SliderThumbWidth.toPx()

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = inactiveColor.color,
                topLeft = Offset(trackStart, trackY - trackThickness / 2f),
                size = Size(trackEnd - trackStart, trackThickness),
                cornerRadius = CornerRadius(trackThickness / 2f),
            )
            drawRoundRect(
                color = activeColor.color,
                topLeft = Offset(trackStart, trackY - trackThickness / 2f),
                size = Size(thumbX - trackStart, trackThickness),
                cornerRadius = CornerRadius(trackThickness / 2f),
            )
            drawLine(
                color = activeColor.color,
                start = Offset(thumbX, trackY - thumbHeight / 2f),
                end = Offset(thumbX, trackY + thumbHeight / 2f),
                strokeWidth = thumbWidth,
                cap = StrokeCap.Round,
            )
        }
        ValueIndicator(
            value = value,
            color = activeColor.color,
            modifier = Modifier.offset {
                IntOffset(
                    (thumbX - bubbleWidthPx / 2f).roundToInt(),
                    (trackY - thumbHeight / 2f - bubbleHeightPx).roundToInt(),
                )
            },
        )
    }
}

@Composable
private fun ValueIndicator(
    value: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(ValueIndicatorWidth, ValueIndicatorHeight),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val arrowHeight = size.height * 0.2f
            drawRoundRect(
                color = color,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height - arrowHeight),
                cornerRadius = CornerRadius(size.height * 0.15f),
            )
            val path = Path().apply {
                moveTo(size.width * 0.35f, size.height - arrowHeight)
                lineTo(size.width * 0.5f, size.height)
                lineTo(size.width * 0.65f, size.height - arrowHeight)
                close()
            }
            drawPath(path, color)
        }
        Text(
            text = value.toString(),
            color = Palette.OffWhite.color,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-3).dp),
        )
    }
}
