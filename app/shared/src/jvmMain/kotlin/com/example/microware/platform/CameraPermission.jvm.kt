package com.example.microware.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberCameraPermissionState(): CameraPermissionState = CameraPermissionState(
    isGranted = true,
    canRequestPermission = false,
    requestPermission = {},
)
