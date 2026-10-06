package com.example.microware.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.platform.rememberCameraPermissionState
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.icons.drawScanner
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView

@Composable
fun DurableIdQrScanner(
    onScanned: (DurableId) -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraPermission = rememberCameraPermissionState()
    var cameraOpen by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val minSize = minOf(maxWidth, maxHeight)
            if (cameraOpen && cameraPermission.isGranted) {
                ScannerView(
                    codeTypes = listOf(BarcodeFormat.FORMAT_QR_CODE),
                    modifier = Modifier.size(minSize)
                        .clipToBounds()
                        .border(border = BorderStroke(2.dp, Palette.Black.color)),
                ) { result ->
                    if (result is BarcodeResult.OnSuccess) {
                        val durableId = runCatching {
                            decodeDurableIdQrPayload(result.barcode.data)
                        }.getOrNull()
                        if (durableId != null) {
                            onScanned(durableId)
                        }
                    }
                }
            } else {
                IconButton(
                    onClick = { onInteraction(); cameraPermission.request(); cameraOpen = true },
                    modifier = Modifier.size(minSize),
                ) {
                    drawScanner()
                }
            }
        }
    }
}
