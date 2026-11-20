package com.example.microware.editor.logic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Parameter
import com.example.microware.game.model.Selector
import com.example.microware.game.model.TagFilter
import com.example.microware.game.model.Trigger
import com.example.microware.game.model.parameters
import com.example.microware.ui.icons.drawParameter

private const val parameterAnimationDurationMillis = 2400

/** Picks either tags or one of the parameters [trigger] offers its actions. */
@Composable
internal fun SelectorPicker(
    selector: Selector,
    trigger: Trigger,
    usedTags: Set<LogicTag>,
    onChange: (Selector) -> Unit,
) {
    val filter = selector as? TagFilter
    val parameter = selector as? Parameter

    LogicTagPicker(
        selectedTags = filter?.tags.orEmpty(),
        onToggleTag = { onChange(filter?.toggling(it) ?: TagFilter(it)) },
        usedTags = usedTags,
        selectedIcon = parameter?.let { { drawParameter(it, trigger) } },
    ) {
        ParameterRow(trigger, selected = parameter, onSelect = onChange)
    }
}

@Composable
private fun ParameterRow(
    trigger: Trigger,
    selected: Parameter?,
    onSelect: (Parameter) -> Unit,
) {
    val parameters = trigger.parameters
    if (parameters.isEmpty()) return

    val loops by produceState(0f) {
        val start = withInfiniteAnimationFrameNanos { it }
        while (true) {
            withInfiniteAnimationFrameNanos { value = (it - start) / 1_000_000f / parameterAnimationDurationMillis }
        }
    }

    Row(
        modifier = Modifier.padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (parameter in parameters) {
            PickerOption(
                description = "Trigger parameter",
                selected = parameter == selected,
                onClick = { onSelect(parameter) },
                selectedColor = Palette.LightGray,
            ) { drawParameter(parameter, trigger, t = loops % 1f, seed = loops.toInt()) }
        }
    }
}

@Preview
@Composable
fun ParameterRowPreview() {
    val trigger = Trigger.Collision(
        firstTarget = TagFilter(LogicTag(0)),
        secondTarget = TagFilter(LogicTag(9)),
        firstTargetCount = 2,
    )
    var selected by remember { mutableStateOf(Parameter.RandomFirstTarget) }
    Box(Modifier.background(Palette.OffWhite.color).padding(8.dp)) {
        ParameterRow(trigger, selected) { selected = it }
    }
}
