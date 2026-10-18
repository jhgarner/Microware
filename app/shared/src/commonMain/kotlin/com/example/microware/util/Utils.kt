package com.example.microware.util

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.Ref
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.microware.core.Model
import com.example.microware.ui.layout.density
import com.example.microware.ui.layout.insets
import kotlin.reflect.KProperty

inline fun <reified Ex : Exception> untilThrows(cond: () -> Unit) {
    while (true) {
        try {
            cond()
        } catch (ex: Ex) {
            return
        }
    }
}

@Composable
fun <T> rememberLast(t: T): T? {
    val last = remember { Ref<T>() }
    SideEffect { last.value = t }
    return last.value
}

@Composable
inline fun Dp.toPx(): Float = with(density) { toPx() }

@Composable
inline fun Dp.roundToPx(): Int = with(density) { roundToPx() }

@Composable
inline fun Int.toDp(): Dp = with(density) { toDp() }

inline fun <T> MutableState<T>.modify(f: (T) -> T) {
    value = f(value)
}

fun Offset.toModel(): Model.Offset = Model.Offset(x, y)
fun Model.Offset.toUi(): Offset = Offset(x, y)

@Composable
fun Modifier.insetPadding(): Modifier = padding(
    start = insets.leftDp() + 32.dp,
    end = insets.rightDp() + 32.dp,
    top = insets.topDp() + 32.dp,
    bottom = insets.bottomDp(),
)

inline fun <T, P> T.runMaybe(p: P?, action: T.(P) -> T): T = run { p?.let { action(it) } ?: this }

data class Settable<T>(var value: T, val setter: (T) -> Unit) {
    operator fun <Owner> getValue(thisRef: Owner, property: KProperty<*>): T {
        return value
    }

    operator fun <Owner> setValue(thisRef: Owner, property: KProperty<*>, value: T) {
        setter(value)
        this.value = value
    }
}