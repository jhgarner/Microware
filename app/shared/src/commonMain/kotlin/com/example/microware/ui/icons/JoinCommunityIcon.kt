package com.example.microware.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import com.example.microware.Palette

internal fun DrawScope.drawJoinCommunityIcon(t: Float = 0f) {
    val approach = animate(t) {
        hold(.08f)
        anim(1f, .37f)
        hold(until(.9f))
        anim(initial, until(1f))
    }
    val cyanPosition = Offset(-.1f, .48f) + Offset(.24f, -.26f) * approach
    val cyanSize = Size(.44f - .20f * approach, .60f - .22f * approach)
    drawCommunityPerson(cyanPosition, cyanSize, Palette.Cyan, communityBounce(t, .46f))
    drawCommunityPerson(
        Offset(.44f, .22f),
        Size(.24f, .38f),
        Palette.Purple,
        communityBounce(t, .58f),
    )
    drawCommunityPerson(
        Offset(.72f, .22f),
        Size(.24f, .38f),
        Palette.Purple,
        communityBounce(t, .70f),
    )

    val arrow = Path().apply {
        moveTo(size.width * .45f, size.height * .88f)
        lineTo(size.width * .67f, size.height * .88f)
        moveTo(size.width * .59f, size.height * .80f)
        lineTo(size.width * .67f, size.height * .88f)
        lineTo(size.width * .59f, size.height * .96f)
    }
    rotate(-45f, pivot = Offset(size.width * .51f, size.height * .88f)) {
        drawPath(
            arrow,
            Palette.Black.color.copy(alpha = 1f - approach),
            style = Stroke(
                size.minDimension * .065f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

fun DrawScope.drawCreateCommunityIcon(t: Float = 0f) {
    drawJoinCommunityIcon(t)

    val center = Offset(size.width * .78f, size.height * .78f)
    val radius = size.minDimension * .18f
    drawCircle(Palette.Forest.color, radius, center)
    drawLine(
        color = Palette.OffWhite.color,
        start = Offset(center.x - radius * .5f, center.y),
        end = Offset(center.x + radius * .5f, center.y),
        strokeWidth = radius * .22f,
    )
    drawLine(
        color = Palette.OffWhite.color,
        start = Offset(center.x, center.y - radius * .5f),
        end = Offset(center.x, center.y + radius * .5f),
        strokeWidth = radius * .22f,
    )
}

private fun communityBounce(t: Float, start: Float): Float = animate(t - start) {
    anim(.09f, .10f)
    anim(0f, .12f)
    hold()
}

private fun DrawScope.drawCommunityPerson(
    position: Offset,
    personSize: Size,
    color: Palette,
    bounce: Float,
) {
    within(fromPercents(Rect(position - Offset(0f, bounce), personSize))) {
        drawCircle(
            color.color,
            radius = size.width * .30f,
            center = Offset(size.width * .5f, size.height * .22f),
        )
        val shoulders = Path().apply {
            moveTo(0f, size.height)
            cubicTo(
                0f, size.height * .35f,
                size.width, size.height * .35f,
                size.width, size.height,
            )
            close()
        }
        drawPath(shoulders, color.color)
    }
}

@Preview
@Composable
private fun JoinCommunityIconPreview() = Icon { drawJoinCommunityIcon() }

@Preview
@Composable
private fun JoinCommunityAnimationPreview() = Animated { drawJoinCommunityIcon(it) }
