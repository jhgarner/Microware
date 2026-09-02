package com.example.microware

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
) {
    var expanded by remember { mutableStateOf(false) }

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
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.size(width = 280.dp, height = 320.dp).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(key = "clear") {
                    LogicTagOption(
                        selected = selectedTag == null,
                        onClick = {
                            onSelectTag(null)
                            expanded = false
                        },
                    )
                }
                items(
                    items = LogicTag.all,
                    key = { it.tag },
                ) { tag ->
                    LogicTagOption(
                        tag = tag,
                        selected = selectedTag == tag,
                        onClick = {
                            onSelectTag(tag)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

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
