package com.example.microware.community

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.microware.ui.layout.insets

@Composable
fun JoinCommunity(onJoin: (DurableId) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        DurableIdQrScanner(
            modifier = Modifier.weight(.5f).padding(
                start = insets.leftDp() + 32.dp,
                end = insets.rightDp() + 32.dp,
                top = insets.topDp() + 32.dp,
                bottom = insets.bottomDp() + 32.dp,
            ),
            onScanned = onJoin,
        )
        var emojiInput by remember { mutableStateOf("") }
        TextField(
            modifier = Modifier.weight(.5f),
            value = emojiInput,
            onValueChange = { emojiInput = it },
        )
    }
}
