package com.example.microware.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.microware.ui.icons.drawTrashHandleIcon
import com.example.microware.util.Identifiable
import com.example.microware.util.upserting
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

val TrashGap = 8.dp

data class Trash<T : Identifiable<T>>(
    val garbage: PersistentList<T> = persistentListOf(),
) {
    fun discard(t: T): Trash<T> = copy(garbage = garbage.upserting(t))
}

@Composable
fun TrashControls(foreground: Color) {
    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        drawTrashHandleIcon(foreground)
    }
}
