package com.example.microware

import androidx.compose.runtime.Composable

@Composable
expect fun SystemBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
)
