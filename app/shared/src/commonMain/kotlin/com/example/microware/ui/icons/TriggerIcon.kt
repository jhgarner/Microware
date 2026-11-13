package com.example.microware.ui.icons

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.TagFilter
import com.example.microware.game.model.Trigger

internal fun DrawScope.drawTrigger(trigger: Trigger) = when (trigger) {
    Trigger.Start -> drawStartIcon()
    is Trigger.Press -> drawDownIcon()
    is Trigger.Release -> drawTapIcon()
    is Trigger.Collision -> drawCollisionIcon()
    is Trigger.Separation -> drawSeparationIcon()
}

@Preview
@Composable
fun TriggerIconsPreview() {
    Row {
        Icon { drawTrigger(Trigger.Start) }
        Icon { drawTrigger(Trigger.Press(TagFilter(LogicTag(0)))) }
        Icon { drawTrigger(Trigger.Release(TagFilter(LogicTag(0)))) }
        Icon { drawTrigger(Trigger.Collision(TagFilter(LogicTag(0)), TagFilter(LogicTag(1)))) }
        Icon { drawTrigger(Trigger.Separation(TagFilter(LogicTag(0)), TagFilter(LogicTag(1)))) }
    }
}
