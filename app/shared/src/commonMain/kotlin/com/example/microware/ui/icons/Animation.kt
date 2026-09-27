package com.example.microware.ui.icons

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.Easing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.util.lerp

data class Frame<T>(val start: T, val stop: T, val ease: Easing, val duration: Float)
data class AnimBuilder<T>(
    val initial: T,
    val actions: MutableList<Frame<T>>,
    val lerp: (T, T, Float) -> T,
) {
    var at = 0f

    fun get(t: Float): T {
        if (t <= 0f || t >= 1f) {
            return initial
        }
        var remaining = t
        for (action in actions) {
            if (remaining <= action.duration) {
                return lerp(
                    action.start,
                    action.stop,
                    action.ease.transform(remaining / action.duration),
                )
            }
            remaining -= action.duration
        }
        return initial
    }

    fun hold(duration: Float = 1f - at) {
        anim(actions.lastOrNull()?.stop ?: initial, duration)
    }

    fun until(time: Float): Float = time - at

    fun anim(newValue: T, duration: Float, ease: Easing = EaseInOut) {
        at += duration
        val from = actions.lastOrNull()?.stop ?: initial
        actions.add(Frame(from, newValue, ease, duration))
    }
}

fun animate(t: Float, initial: Float = 0f, builder: AnimBuilder<Float>.() -> Unit): Float =
    animate(t, initial, builder, ::lerp)

fun animate(
    t: Float,
    initial: Offset,
    builder: AnimBuilder<Offset>.() -> Unit,
): Offset =
    animate(t, initial, builder, ::lerp)

fun <T> animate(
    t: Float,
    initial: T,
    builder: AnimBuilder<T>.() -> Unit,
    lerp: (T, T, Float) -> T,
): T {
    val state = AnimBuilder(initial, mutableListOf(), lerp)
    state.builder()
    return state.get(t)
}

