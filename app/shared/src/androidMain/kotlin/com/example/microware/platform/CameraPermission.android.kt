package com.example.microware.platform

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        isGranted = it
    }

    return CameraPermissionState(
        isGranted = isGranted,
        canRequestPermission = !isGranted,
        requestPermission = { launcher.launch(Manifest.permission.CAMERA) },
    )
}
