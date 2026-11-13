package com.example.microware.editor.logic

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.TagFilter
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.icons.drawLogicTag
import com.example.microware.ui.icons.drawLogicTagAt
import com.example.microware.ui.icons.drawLogicTags
import kotlin.math.ceil
import kotlin.math.floor

private const val logicTagBackgroundScrollDurationMillis = 44000
private const val logicTagBackgroundTagSizeFraction = .3f
private const val logicTagBackgroundTagGapFraction = .12f
private const val logicTagBackgroundVerticalGapFraction = .05f

private val logicTagColors = Palette.entries.filter { it != Palette.Clear }
private const val logicTagShapeCount = 4
private val logicTagCount = logicTagColors.size * logicTagShapeCount

private val allLogicTags: List<LogicTag> = (0..<logicTagCount).map(::LogicTag)

private val logicTagBorderColors = Palette.entries
    .filter { it != Palette.Clear }
    .map { it.color }

private val logicTagBorder = BorderStroke(
    width = 2.dp,
    brush = Brush.sweepGradient(*discreteColorStops(logicTagBorderColors)),
)

private fun discreteColorStops(colors: List<Color>): Array<Pair<Float, Color>> {
    val sectionSize = 1f / colors.size
    val transitionSize = .001f

    return colors.flatMapIndexed { index, color ->
        val sectionStart = index * sectionSize
        val sectionEnd = if (index == colors.lastIndex) {
            1f
        } else {
            (index + 1) * sectionSize - transitionSize
        }
        listOf(
            sectionStart to color,
            sectionEnd to color,
        )
    }.toTypedArray()
}

@Composable
fun LogicTagPicker(
    selectedTags: Set<LogicTag>,
    onToggleTag: (LogicTag) -> Unit,
    modifier: Modifier = Modifier,
    usedTags: Set<LogicTag> = allLogicTags.toSet(),
    selectedIcon: (DrawScope.() -> Unit)? = null,
    parameters: @Composable () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var unusedExpanded by remember { mutableStateOf(false) }
    // Toggling a tag can change which tags are in use, so the split is only
    // recomputed on opening to keep options from jumping between the grids.
    val (preferredTags, unusedTags) = remember(expanded) {
        allLogicTags.partition { it in usedTags }
    }

    Box(modifier = modifier) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier
                .size(40.dp)
                .semantics { contentDescription = "Choose logic tag" },
            colors = ButtonDefaults.buttonColors(
                containerColor = Palette.OffWhite.color,
                contentColor = Color.Black,
            ),
            shape = RectangleShape,
            border = logicTagBorder,
            contentPadding = PaddingValues(0.dp),
        ) {
            when {
                selectedIcon != null -> Canvas(Modifier.fillMaxSize().padding(4.dp), selectedIcon)
                selectedTags.isEmpty() -> ScrollingLogicTagBackground(Modifier.fillMaxSize())
                else -> Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    drawLogicTags(selectedTags)
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(280.dp),
        ) {
            Column(Modifier.width(280.dp).padding(8.dp)) {
                parameters()
                if (preferredTags.isNotEmpty()) {
                    LogicTagGrid(
                        tags = preferredTags,
                        selectedTags = selectedTags,
                        modifier = Modifier.fillMaxWidth().height(gridHeight(preferredTags.size)),
                        onToggleTag = onToggleTag,
                    )
                }
                if (unusedTags.isNotEmpty()) {
                    IconButton(
                        description = if (unusedExpanded) "Hide unused logic tags" else "Show unused logic tags",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { unusedExpanded = !unusedExpanded },
                    ) { _ ->
                        val y = if (unusedExpanded) size.height * .65f else size.height * .35f
                        val tipY = if (unusedExpanded) size.height * .25f else size.height * .75f
                        drawLine(
                            Color.Black,
                            Offset(size.width * .2f, y),
                            Offset(size.width * .5f, tipY),
                            2.dp.toPx(),
                        )
                        drawLine(
                            Color.Black,
                            Offset(size.width * .8f, y),
                            Offset(size.width * .5f, tipY),
                            2.dp.toPx(),
                        )
                    }
                    if (unusedExpanded) {
                        LogicTagGrid(
                            tags = unusedTags,
                            selectedTags = selectedTags,
                            modifier = Modifier.fillMaxWidth().height(gridHeight(unusedTags.size)),
                            onToggleTag = onToggleTag,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagFilterPicker(
    filter: TagFilter,
    usedTags: Set<LogicTag>,
    onChange: (TagFilter) -> Unit,
) {
    LogicTagPicker(
        selectedTags = filter.tags,
        onToggleTag = { onChange(filter.toggling(it)) },
        usedTags = usedTags,
    )
}

@Composable
private fun ScrollingLogicTagBackground(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "logic tag background")
    val scrollFraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = logicTagBackgroundScrollDurationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "logic tag background scroll",
    )

    Canvas(modifier) {
        val tagSize = size.minDimension * logicTagBackgroundTagSizeFraction
        val horizontalStep = tagSize * (1f + logicTagBackgroundTagGapFraction)
        val verticalStep = tagSize * (1f + logicTagBackgroundVerticalGapFraction)
        val horizontalCycle = horizontalStep * allLogicTags.size
        val horizontalOffset = -scrollFraction * horizontalCycle
        val verticalOffset = -scrollFraction * verticalStep * 12f
        val firstColumn = floor((horizontalOffset - tagSize) / horizontalStep).toInt()
        val lastColumn = ceil((horizontalOffset + size.width + tagSize) / horizontalStep).toInt()
        val firstRow = floor((verticalOffset - tagSize) / verticalStep).toInt()
        val lastRow = ceil((verticalOffset + size.height + tagSize) / verticalStep).toInt()

        for (row in firstRow..lastRow) {
            for (column in firstColumn..lastColumn) {
                val tag = allLogicTags[positiveModulo(column, allLogicTags.size)]
                val center = Offset(
                    x = column * horizontalStep + tagSize / 2f - horizontalOffset,
                    y = row * verticalStep + tagSize / 2f - verticalOffset,
                )
                drawLogicTagAt(tag, center, tagSize)
            }
        }
    }
}

private fun positiveModulo(value: Int, modulus: Int): Int = (value % modulus + modulus) % modulus

@Composable
private fun LogicTagGrid(
    tags: List<LogicTag>,
    selectedTags: Set<LogicTag>,
    modifier: Modifier,
    onToggleTag: (LogicTag) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = tags, key = { it.tag }) { tag ->
            PickerOption(
                description = "Logic tag",
                selected = tag in selectedTags,
                onClick = { onToggleTag(tag) },
            ) { drawLogicTag(tag) }
        }
    }
}

private fun gridHeight(optionCount: Int) = ((optionCount + 5) / 6 * 44).coerceAtMost(220).dp

@Composable
internal fun PickerOption(
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Palette = Palette.Cyan,
    icon: DrawScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (selected) selectedColor.color else Palette.OffWhite.color,
                shape = RectangleShape,
            )
            .border(1.dp, Color.Black, RectangleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(6.dp), icon)
    }
}
