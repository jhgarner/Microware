package com.example.microware.ui.icons

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.LogicTag

internal fun DrawScope.drawAction(action: Action, t: Float = 0f) = when (action) {
    is Action.Move -> drawMoveIcon(action.target, t)
    is Action.MoveTo -> drawMoveToIcon(t, teleport = action.speed == null)
    is Action.MoveWithin -> drawMoveWithinIcon(t, teleport = action.speed == null)
    Action.Win -> drawWinIcon()
    Action.Lose -> drawLoseIcon()
}

@Preview
@Composable
fun ActionIconsPreview() {
    Row {
        Icon { drawAction(Action.Move(LogicTag(0), Direction.E, speed = 1)) }
        Icon { drawAction(Action.MoveTo(LogicTag(0), LogicTag(1), speed = 1)) }
        Icon { drawAction(Action.MoveWithin(LogicTag(0), LogicTag(1), speed = 1)) }
        Icon { drawAction(Action.Win) }
        Icon { drawAction(Action.Lose) }
    }
}
