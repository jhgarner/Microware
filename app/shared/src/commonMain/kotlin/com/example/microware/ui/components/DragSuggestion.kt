package com.example.microware.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.microware.ui.gestures.racing
import com.example.microware.ui.gestures.sequence
import com.example.microware.ui.icons.TAP_DOWN
import com.example.microware.ui.icons.TAP_START
import com.example.microware.ui.icons.drawTapCircle
import com.example.microware.ui.icons.drawTapHand
import kotlin.math.PI
import kotlin.math.sin
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

fun Modifier.suggestDrag() = composed {
    val location = remember { mutableStateOf<(Offset) -> Offset>({ it }) }
    var lastShown by remember { mutableStateOf(Instant.DISTANT_PAST) }
    onGloballyPositioned { location.value = it::localToWindow }
        .pointerInput(Unit) {
            racing {
                sequence {
                    val pointer = expectTap();
                    if (Clock.System.now() >= lastShown + 15.seconds) {
                        lastShown = Clock.System.now()
                        setOverlayAnimation(2.0.seconds) { t ->
                            val offset =
                                runCatching { location.value(pointer.position) }.getOrNull()
                                    ?: return@setOverlayAnimation
                            val startX = offset.x
                            val horiz =
                                lerp(0f, 2 * PI.toFloat(), (t - 0.2f).coerceIn(0f, 0.8f) / 0.8f)
                            val left = sin(horiz) * 200f + startX
                            val top = offset.y
                            val iconSize = 40.dp.toPx()
                            translate(left = left, top = top) {
                                inset(
                                    left = 0f,
                                    top = 0f,
                                    right = size.width - iconSize,
                                    bottom = size.height - iconSize,
                                ) {
                                    drawTapCircle(
                                        lerp(
                                            TAP_START,
                                            0.4f,
                                            t.coerceIn(0f, 0.2f) / 0.2f,
                                        ),
                                    )
                                    drawTapHand(
                                        lerp(
                                            TAP_START,
                                            TAP_DOWN,
                                            t.coerceIn(0f, 0.2f) / 0.2f,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
}
