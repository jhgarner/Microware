package com.example.microware.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.layout.insets

enum class HomeNavigation { Editor }

@Composable
fun Home(navigate: (HomeNavigation) -> Unit) {
    Column(
        modifier = Modifier.padding(
            start = insets.leftDp(),
            end = insets.rightDp(),
            top = insets.topDp(),
            bottom = insets.bottomDp(),
        )
            .fillMaxSize()
            .aspectRatio(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(
            modifier = Modifier.weight(0.5f),
            onClick = { navigate(HomeNavigation.Editor) },
        ) {
            drawCircle(Color.Cyan, radius = 20f)
        }
        Row(modifier = Modifier.weight(0.5f)) {
            IconButton(
                modifier = Modifier.weight(0.5f),
                onClick = { navigate(HomeNavigation.Editor) },
            ) {
                drawCircle(Color.Cyan, radius = 20f)
            }
            IconButton(
                modifier = Modifier.weight(0.5f),
                onClick = { navigate(HomeNavigation.Editor) },
            ) {
                drawCircle(Color.Cyan, radius = 20f)
            }
        }
    }
}