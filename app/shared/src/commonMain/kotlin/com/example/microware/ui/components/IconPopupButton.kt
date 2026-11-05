package com.example.microware.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.microware.Palette

@Composable
internal fun IconPopupButton(
    description: String,
    modifier: Modifier = Modifier,
    icon: DrawScope.(Float) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            description = description,
            modifier = modifier,
            selected = expanded,
            onClick = { expanded = !expanded },
            icon = icon,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RectangleShape,
            containerColor = Palette.OffWhite.color,
            border = BorderStroke(2.dp, Palette.Black.color),
            content = content,
        )
    }
}
