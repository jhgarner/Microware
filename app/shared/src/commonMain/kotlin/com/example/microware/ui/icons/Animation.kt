package com.example.microware.ui.icons

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.Easing
import androidx.compose.ui.util.lerp

data class Frame(val start: Float, val stop: Float, val ease: Easing, val duration: Float)
data class AnimBuilder(val initial: Float, val actions: MutableList<Frame>) {
    fun get(t: Float): Float {
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

    fun anim(newValue: Float, duration: Float, ease: Easing = EaseInOut) {
        val from = actions.lastOrNull()?.stop ?: initial
        actions.add(Frame(from, newValue, ease, duration))
    }
}

fun animate(t: Float, initial: Float = 0f, builder: AnimBuilder.() -> Unit): Float {
    val state = AnimBuilder(initial, mutableListOf())
    state.builder()
    return state.get(t)
}

