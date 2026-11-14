package com.example.microware.ui.icons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import com.example.microware.Palette
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Parameter
import com.example.microware.game.model.TagFilter
import com.example.microware.game.model.Trigger
import com.example.microware.game.model.parameters
import kotlin.random.Random

private const val CandidateAlpha = .45f
private const val UninvolvedAlpha = .12f
private const val RandomPicksPerLoop = 6

private data class Circle(val center: Offset, val radius: Float, val color: Palette)

/** The trigger's own picture, with the objects the parameter stands for picked out of it. */
internal fun DrawScope.drawParameter(
    parameter: Parameter,
    trigger: Trigger,
    t: Float = 0f,
    seed: Int = 0,
) =
    when (trigger) {
        Trigger.Start -> Unit
        is Trigger.Press -> drawDownIcon()
        is Trigger.Release -> drawTapIcon()
        is Trigger.Contact -> drawContactParameter(parameter, trigger, t, seed)
    }

/**
 * Objects the parameter leaves out fade away. A random parameter dims the objects it picks from
 * and lights them one at a time.
 */
private fun DrawScope.drawContactParameter(
    parameter: Parameter,
    trigger: Trigger.Contact,
    t: Float,
    seed: Int,
) {
    val circles = contactCircles(trigger)
    val involved = when (parameter) {
        Parameter.Tapped -> emptySet()
        Parameter.SecondTarget -> setOf(circles.last())
        Parameter.FirstTargets, Parameter.RandomFirstTarget -> circles.dropLast(1).toSet()
        Parameter.AllTargets, Parameter.RandomTarget -> circles.toSet()
    }
    val uninvolved = circles - involved
    val picked = when (parameter) {
        Parameter.RandomFirstTarget, Parameter.RandomTarget -> {
            val index = (t * RandomPicksPerLoop).toInt() % involved.size
            involved.shuffled(Random(seed))[index]
        }

        else -> null
    }

    for (circle in uninvolved.asSequence() + involved.asSequence()) {
        drawCircle(
            color = circle.color.color,
            radius = size.minDimension * circle.radius,
            center = Offset(size.width * circle.center.x, size.height * circle.center.y),
            alpha = when {
                circle !in involved -> UninvolvedAlpha
                picked != null && circle != picked -> CandidateAlpha
                else -> 1f
            },
        )
    }
}

/** The first target objects followed by the second target object. */
private fun contactCircles(trigger: Trigger.Contact): List<Circle> {
    val spread = Offset(if (trigger is Trigger.Separation) .08f else 0f, 0f)
    val firsts = if (trigger.firstTargetCount == 1) {
        listOf(Circle(Offset(.32f, .5f), .22f, Palette.Cyan))
    } else {
        listOf(Offset(.34f, .5f), Offset(.46f, .22f), Offset(.46f, .78f))
            .map { Circle(it, .15f, Palette.Cyan) }
    }
    return firsts.map { it.copy(center = it.center - spread) } +
            Circle(Offset(.68f, .5f) + spread, .22f, Palette.Red)
}

@Preview
@Composable
fun ParameterIconsPreview() {
    val first = TagFilter(LogicTag(0))
    val second = TagFilter(LogicTag(1))
    val triggers = listOf(
        Trigger.Press(first),
        Trigger.Release(first),
        Trigger.Collision(first, second),
        Trigger.Collision(first, first),
        Trigger.Collision(first, second, firstTargetCount = 2),
        Trigger.Collision(first, first, firstTargetCount = 2),
        Trigger.Separation(first, second),
        Trigger.Separation(first, second, firstTargetCount = 2),
    )
    Column {
        for (trigger in triggers) {
            Row {
                Icon { drawTrigger(trigger) }
                for (parameter in trigger.parameters) {
                    Icon { drawParameter(parameter, trigger) }
                }
            }
        }
    }
}

@Preview
@Composable
fun AnimatedParameterIcons() {
    val first = TagFilter(LogicTag(0))
    val second = TagFilter(LogicTag(1))
    val triggers = listOf(
        Trigger.Collision(first, second, firstTargetCount = 2),
        Trigger.Collision(first, first, firstTargetCount = 2),
    )
    Column {
        for (trigger in triggers) {
            Row {
                Icon { drawTrigger(trigger) }
                for (parameter in trigger.parameters) {
                    Animated { drawParameter(parameter, trigger, it, 36) }
                }
            }
        }
    }
}
