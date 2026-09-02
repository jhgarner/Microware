package com.example.microware

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun LogicTagPicker(
    selectedTag: LogicTag?,
    onSelectTag: (LogicTag?) -> Unit,
    modifier: Modifier = Modifier,
    usedTags: Set<LogicTag> = LogicTag.all.toSet(),
    allowClear: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var unusedExpanded by remember { mutableStateOf(false) }
    val preferredTags = LogicTag.all.filter { it in usedTags }
    val unusedTags = LogicTag.all.filterNot { it in usedTags }

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
            border = BorderStroke(2.dp, Color.Black),
            contentPadding = PaddingValues(0.dp),
        ) {
            selectedTag?.let { tag ->
                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    with(tag) { draw() }
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(280.dp),
        ) {
            Column(Modifier.width(280.dp).padding(8.dp)) {
                val preferredOptionCount = preferredTags.size + if (allowClear) 1 else 0
                if (preferredOptionCount > 0) {
                    LogicTagGrid(
                        tags = preferredTags,
                        selectedTag = selectedTag,
                        allowClear = allowClear,
                        modifier = Modifier.fillMaxWidth().height(gridHeight(preferredOptionCount)),
                        onSelectTag = {
                            onSelectTag(it)
                            expanded = false
                        },
                    )
                }
                if (unusedTags.isNotEmpty() && usedTags.size < LogicTag.all.size) {
                    IconButton(
                        description = if (unusedExpanded) "Hide unused logic tags" else "Show unused logic tags",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { unusedExpanded = !unusedExpanded },
                    ) {
                        val y = if (unusedExpanded) size.height * .65f else size.height * .35f
                        val tipY = if (unusedExpanded) size.height * .25f else size.height * .75f
                        drawLine(Color.Black, Offset(size.width * .2f, y), Offset(size.width * .5f, tipY), 2.dp.toPx())
                        drawLine(Color.Black, Offset(size.width * .8f, y), Offset(size.width * .5f, tipY), 2.dp.toPx())
                    }
                    if (unusedExpanded) {
                        LogicTagGrid(
                            tags = unusedTags,
                            selectedTag = selectedTag,
                            allowClear = false,
                            modifier = Modifier.fillMaxWidth().height(gridHeight(unusedTags.size)),
                            onSelectTag = {
                                onSelectTag(it)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogicTagGrid(
    tags: List<LogicTag>,
    selectedTag: LogicTag?,
    allowClear: Boolean,
    modifier: Modifier,
    onSelectTag: (LogicTag?) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (allowClear) {
            item(key = "clear") {
                LogicTagOption(
                    selected = selectedTag == null,
                    onClick = { onSelectTag(null) },
                )
            }
        }
        items(items = tags, key = { it.tag }) { tag ->
            LogicTagOption(
                tag = tag,
                selected = selectedTag == tag,
                onClick = { onSelectTag(tag) },
            )
        }
    }
}

private fun gridHeight(optionCount: Int) = ((optionCount + 5) / 6 * 44).coerceAtMost(220).dp

@Composable
private fun LogicTagOption(
    tag: LogicTag? = null,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (selected) Palette.Cyan.color else Palette.OffWhite.color,
                shape = RectangleShape,
            )
            .border(1.dp, Color.Black, RectangleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (tag == null) "Clear logic tag" else "Logic tag"
            },
        contentAlignment = Alignment.Center,
    ) {
        if (tag == null) {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                drawLine(
                    color = Color.Black,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(size.width, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            }
        } else {
            Canvas(Modifier.fillMaxSize().padding(6.dp)) {
                with(tag) { draw() }
            }
        }
    }
}
