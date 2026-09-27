package com.example.microware.ui.icons

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Trigger

internal fun DrawScope.drawTrigger(trigger: Trigger) = when (trigger) {
    Trigger.Start -> drawStartIcon()
    is Trigger.Tap -> drawTapIcon()
    is Trigger.Collision -> drawCollisionIcon()
}

@Preview
@Composable
fun TriggerIconsPreview() {
    Row {
        Icon { drawTrigger(Trigger.Start) }
        Icon { drawTrigger(Trigger.Tap(LogicTag(0))) }
        Icon { drawTrigger(Trigger.Collision(LogicTag(0), LogicTag(1))) }
    }
}
