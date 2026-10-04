package com.example.microware.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType

@Composable
actual fun rememberCameraPermissionState(): CameraPermissionState {
    val scope = rememberCoroutineScope()
    val initialStatus = remember {
        AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
    }
    var isGranted by remember {
        mutableStateOf(initialStatus == AVAuthorizationStatusAuthorized)
    }
    var canRequest by remember {
        mutableStateOf(initialStatus == AVAuthorizationStatusNotDetermined)
    }

    return CameraPermissionState(
        isGranted = isGranted,
        canRequestPermission = canRequest,
        requestPermission = {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                scope.launch {
                    isGranted = granted
                    canRequest = false
                }
            }
        },
    )
}
