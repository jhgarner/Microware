package com.example.microware.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.microware.game.sprite.Palette
import com.example.microware.util.toPx
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
    X,
    Y,
    Speed,
}

@Composable
fun IntegerSliderControl(
    dimension: IntegerSliderDimension,
    value: Int?,
    maxValue: Int,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Palette = Palette.Navy,
    inactiveColor: Palette = Palette.LightGray,
    allowInfinity: Boolean = false,
    minValue: Int = 1,
) {
    require(maxValue >= minValue) { "maxValue must be at least minValue" }
    require(allowInfinity || value != null) { "value cannot be infinity for this slider" }
    val finiteValue = value?.coerceIn(minValue, maxValue)
    val latestOnValueChange = rememberUpdatedState(onValueChange)
    val hasPreviousValue = value == null || finiteValue != minValue
    val hasNextValue = value != null && (finiteValue!! < maxValue || allowInfinity)

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
            IconButton(
                description = "Decrease ${dimension.name.lowercase()}",
                onClick = {
                    onValueChange(
                        if (value == null) maxValue else (finiteValue!! - 1).coerceAtLeast(minValue)
                    )
                },
                enabled = hasPreviousValue,
                modifier = Modifier
                    .requiredSize(IncrementButtonWidth, IncrementButtonHeight)
                    .align(Alignment.Bottom),
            ) { _ -> drawDecrementIcon() }
        }
        IntegerSlider(
            value = finiteValue,
            maxValue = maxValue,
            onValueChange = latestOnValueChange.value,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            allowInfinity = allowInfinity,
            minValue = minValue,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            description = "Increase ${dimension.name.lowercase()}",
            onClick = {
                onValueChange(
                    if (finiteValue!! < maxValue) finiteValue + 1 else null
                )
            },
            enabled = hasNextValue,
            modifier = Modifier
                .requiredSize(IncrementButtonWidth, IncrementButtonHeight)
                .align(Alignment.Bottom),
        ) { _ -> drawIncrementIcon() }
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

        when (dimension) {
            IntegerSliderDimension.Width, IntegerSliderDimension.X -> {
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
            }

            IntegerSliderDimension.Height, IntegerSliderDimension.Y -> {
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

            else -> {
                drawLine(
                    color = color,
                    start = Offset(size.width * .15f, size.height * .3f),
                    end = Offset(size.width * .38f, size.height * .3f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * .15f, size.height * .7f),
                    end = Offset(size.width * .28f, size.height * .7f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * .3f, center),
                    end = Offset(size.width * .85f, center),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * .67f, center - arrowSize),
                    end = Offset(size.width * .85f, center),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * .67f, center + arrowSize),
                    end = Offset(size.width * .85f, center),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun DrawScope.drawDecrementIcon() {
    val side = size.minDimension
    val center = Offset(size.width / 2f, size.height / 2f)
    val halfLength = side * .35f
    drawLine(
        Color.Black,
        Offset(center.x - halfLength, center.y),
        Offset(center.x + halfLength, center.y),
        3.dp.toPx(),
    )
}

private fun DrawScope.drawIncrementIcon() {
    val side = size.minDimension
    val center = Offset(size.width / 2f, size.height / 2f)
    val halfLength = side * .35f
    drawLine(
        Color.Black,
        Offset(center.x, center.y - halfLength),
        Offset(center.x, center.y + halfLength),
        3.dp.toPx(),
    )
    drawLine(
        Color.Black,
        Offset(center.x - halfLength, center.y),
        Offset(center.x + halfLength, center.y),
        3.dp.toPx(),
    )
}

@Composable
private fun IntegerSlider(
    value: Int?,
    maxValue: Int,
    onValueChange: (Int?) -> Unit,
    activeColor: Palette,
    inactiveColor: Palette,
    allowInfinity: Boolean,
    minValue: Int,
    modifier: Modifier = Modifier,
) {
    val horizontalPaddingPx = SliderHorizontalPadding.toPx()
    val latestOnValueChange = rememberUpdatedState(onValueChange)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(SliderHeight)
            .pointerInput(minValue, maxValue) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun updateFromPosition(position: Offset) {
                        val usableWidth = size.width - horizontalPaddingPx * 2f
                        val fraction = ((position.x - horizontalPaddingPx) / usableWidth)
                            .coerceIn(0f, 1f)
                        val lastIndex = maxValue - minValue + if (allowInfinity) 1 else 0
                        val index = (fraction * lastIndex).roundToInt().coerceIn(0, lastIndex)
                        latestOnValueChange.value(
                            if (allowInfinity && index == lastIndex) null else index + minValue
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
        val lastIndex = maxValue - minValue + if (allowInfinity) 1 else 0
        val selectedIndex = if (value == null) lastIndex else value - minValue
        val fraction = if (lastIndex == 0) 0f else selectedIndex.toFloat() / lastIndex
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
    value: Int?,
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
            text = value?.toString() ?: "∞",
            color = Palette.OffWhite.color,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-3).dp),
        )
    }
}
