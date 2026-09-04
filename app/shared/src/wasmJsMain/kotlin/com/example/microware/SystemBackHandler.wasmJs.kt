package com.example.microware

import androidx.compose.runtime.Composable

@Composable
actual fun SystemBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
