package com.example.microware.platform

import androidx.compose.runtime.Composable

data class CameraPermissionState(
    val isGranted: Boolean,
    val canRequestPermission: Boolean,
    val requestPermission: () -> Unit,
) {
    fun request() {
        if (!isGranted && canRequestPermission) {
            requestPermission()
        }
    }
}

@Composable
expect fun rememberCameraPermissionState(): CameraPermissionState
