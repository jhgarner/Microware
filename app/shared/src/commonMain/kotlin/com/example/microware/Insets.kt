package com.example.microware

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

data class Insets(val window: WindowInsets, val density: Density) {
    companion object {
        @Composable
        fun new(): Insets =
            Insets(WindowInsets.safeContent.exclude(WindowInsets.ime), LocalDensity.current)
    }

    fun top() = window.getTop(density)
    fun topDp() = (top() / density.density).dp
    fun bottom() = window.getBottom(density)
    fun bottomDp() = (bottom() / density.density).dp
    fun left() = window.getLeft(density, LayoutDirection.Ltr)
    fun leftDp() = (left() / density.density).dp
    fun right() = window.getRight(density, LayoutDirection.Ltr)
    fun rightDp() = (right() / density.density).dp
}

val LocalInsets: ProvidableCompositionLocal<Insets> =
    staticCompositionLocalOf { throw RuntimeException("No Insets set") }

val insets @Composable inline get() = LocalInsets.current
val density @Composable inline get() = LocalDensity.current
