package com.example.microware.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.microware.Palette

@Composable
internal fun SlidingIconSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String,
    stateDescription: String,
    firstIcon: DrawScope.() -> Unit,
    secondIcon: DrawScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val slide by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(180),
        label = "switch cover",
    )
    Canvas(
        modifier
            .padding(4.dp)
            .size(80.dp, 40.dp)
            .background(Palette.OffWhite.color)
            .border(2.dp, Palette.Black.color)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics {
                contentDescription = description
                this.stateDescription = stateDescription
            },
    ) {
        val border = 2.dp.toPx()
        val slotWidth = (size.width - border * 2) / 2
        drawRect(
            color = Palette.Cyan.color,
            topLeft = Offset(border + slotWidth * slide, border),
            size = Size(slotWidth, size.height - border * 2),
        )
        val iconPadding = 10.dp.toPx()
        inset(iconPadding, iconPadding, size.width / 2 + iconPadding, iconPadding) {
            firstIcon()
        }
        inset(size.width / 2 + iconPadding, iconPadding, iconPadding, iconPadding) {
            secondIcon()
        }
    }
}
