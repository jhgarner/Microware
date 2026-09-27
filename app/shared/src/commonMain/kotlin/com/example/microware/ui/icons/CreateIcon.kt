package com.example.microware.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

fun DrawScope.drawCreateIcon(t: Float) {
    var mountainMoveAt = 0f
    var mountainDone = 0f
    val mountainStart = Offset(.02f, .02f)
    val mountainsFinal = Offset(.24f, .02f)
    val mountains = animate(t, initial = mountainStart) {
        hold(0.1f)
        mountainMoveAt = at
        anim(mountainsFinal, 0.1f)
        mountainDone = at
        hold(until(.97f))
        anim(initial, until(1f))
        hold()
    }
    within(fromPercents(Rect(mountains, Size(.6f, 1.2f)))) {
        drawMountains()
    }

    var houseMoveAt = 0f
    var houseDone = 0f
    val houseStart = Offset(.55f, .3f)
    val houseFinal = Offset(.32f, .165f)
    val house = animate(t, initial = houseStart) {
        hold(mountainDone + 0.2f)
        houseMoveAt = at
        anim(houseFinal, 0.1f)
        houseDone = at
        hold(until(.97f))
        anim(initial, until(1f))
        hold()
    }
    within(fromPercents(Rect(house, Size(.4f, .8f)))) {
        drawHouse()
    }

    var trainMoveAt = 0f
    var trainDone = 0f
    val trainStart = Offset(.725f, .105f)
    val trainFinal = Offset(.65f, .34f)
    val train = animate(t, initial = trainStart) {
        hold(houseDone + 0.1f)
        trainMoveAt = at
        anim(trainFinal, 0.1f)
        trainDone = at
        hold(until(.97f))
        anim(initial, until(1f))
        hold()
    }
    within(fromPercents(Rect(train, Size(.3f, .6f)))) {
        drawTrainIcon()
    }

    val pressTime = 0.02f
    val hand = animate(t, initial = Offset(.5f, -.3f)) {
        val mountainTarget = Offset(.3f, .5f)
        anim(mountainTarget, until(mountainMoveAt - pressTime))
        hold(pressTime)
        val handEndMountain = mountainsFinal - mountainStart + mountainTarget
        anim(handEndMountain, until(mountainDone))

        val houseTarget = Offset(.65f, .8f)
        anim(houseTarget, until(houseMoveAt - pressTime))
        hold(pressTime)
        val handEndHouse = houseFinal - houseStart + houseTarget
        anim(handEndHouse, until(houseDone))

        val trainTarget = Offset(.8f, .4f)
        anim(trainTarget, until(trainMoveAt - pressTime))
        hold(pressTime)
        val handEndTrain = trainFinal - trainStart + trainTarget
        anim(handEndTrain, until(trainDone))
        hold(.1f)
        anim(initial, 0.1f)
        hold()
    }
    within(fromPercents(Rect(hand, Size(.15f, .3f)))) {
        drawTapHand(0f)
    }
}

@Composable
private fun PreviewCanvas(t: Float) = Canvas(
    modifier = Modifier.width(300.dp).height(150.dp),
) {
    drawCreateIcon(t)
}

@Preview
@Composable
fun StartState() = PreviewCanvas(0f)

@Preview
@Composable
fun Dynamic() = PreviewCanvas(0.6f)

@Preview
@Composable
fun Anim() = Animated(duration = 2000, size = DpSize(300.dp, 150.dp)) { drawCreateIcon(it) }
