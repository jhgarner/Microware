package com.example.microware

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

data class Offer(val offset: Offset, val localOffset: Offset, val obj: Obj, val from: Any)

@Composable
fun Modifier.dropSource(
    settableOffer: MutableState<Offer?>,
    from: Any,
    obj: Obj,
    raceBefore: suspend context(PointerRacer) () -> Unit = { },
): Modifier {
    val layout = remember { mutableStateOf<LayoutCoordinates?>(null) }
    return onGloballyPositioned { layout.value = it }
        .pointerInput(obj.id) {
            racing {
                raceBefore()
                sequence {
                    val localToWindow = layout.value?.let { it::localToWindow } ?: { it }
                    followDrag { change ->
                        var offer by settableOffer
                        offer = offer?.copy(offset = localToWindow(change.position))
                            ?: Offer(localToWindow(change.position), change.position, obj, from)
                    }
                    settableOffer.value = null
                }
            }
        }
}

@Composable
fun Modifier.dropTarget(
    offered: Offer?,
    onStart: () -> Unit = {},
    onEnter: () -> Unit = {},
    onExit: () -> Unit = {},
    onGiven: (Offer) -> Unit = {},
    onEnd: () -> Unit = {},
): Modifier {
    var layout by remember { mutableStateOf<LayoutCoordinates?>(null) }
    layout?.apply {
        val rect = boundsInWindow()
        val lastOffer = rememberLast(offered)
        val wasContained = lastOffer?.let { rect.contains(it.offset) } ?: false
        val contained = offered?.let { rect.contains(it.offset) }
        if (lastOffer == null && offered != null) {
            onStart()
        } else {
            if (!wasContained && contained == true) {
                onEnter()
            } else if (wasContained && contained == null) {
                onGiven(lastOffer)
            } else if (wasContained && contained == false) {
                onExit()
            }
            if (lastOffer != null && offered == null) {
                onEnd()
            }
        }
    }

    return onGloballyPositioned { layout = it }
}
