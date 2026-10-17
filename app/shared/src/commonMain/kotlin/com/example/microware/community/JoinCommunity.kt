package com.example.microware.community

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.icons.drawCreateCommunityIcon
import com.example.microware.ui.icons.drawJoinCommunityIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private enum class SubmittedType { Qr, Emoji }

@Composable
fun JoinCommunity(
    onJoin: suspend (DurableId) -> Boolean,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var invalid by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
    ) {
        var submittedType by remember { mutableStateOf<SubmittedType?>(null) }
        var submitting by remember { mutableStateOf(false) }
        var shouldAnimateText by remember { mutableStateOf(true) }
        DurableIdQrScanner(
            modifier = Modifier.weight(.5f),
            onScanned = {
                if (submitting) return@DurableIdQrScanner
                submitting = true
                submittedType = SubmittedType.Qr
                scope.launch {
                    invalid = !onJoin(it)
                    submitting = false
                }
            },
            onInteraction = { shouldAnimateText = false },
        )
        var emojiInput by remember { mutableStateOf("") }
        Column(
            modifier = Modifier.weight(.5f),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            var placeholder by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                while (true) {
                    val edits = mutableListOf(placeholder)
                    delay(1.seconds)
                    val len = Random.nextInt(3, 8)
                    repeat(len) {
                        val i = Random.nextInt(0, encodingAlphabet.size)
                        placeholder += encodingAlphabet[i]
                        edits.add(placeholder)
                        delay(40.milliseconds)
                    }
                    delay(1.seconds)
                    repeat(len + 1) {
                        placeholder = edits.removeLast()
                        delay(40.milliseconds)
                    }
                }
            }
            val offsetX = remember { Animatable(0f) }
            LaunchedEffect(invalid, submittedType) {
                if (!invalid || submittedType != SubmittedType.Emoji) {
                    offsetX.animateTo(0f)
                    return@LaunchedEffect
                }
                offsetX.animateTo(10f, animationSpec = tween(durationMillis = 40))
                offsetX.animateTo(-10f, animationSpec = tween(durationMillis = 80))
                offsetX.animateTo(0f, animationSpec = tween(durationMillis = 40))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    placeholder = { Text(if (shouldAnimateText) placeholder else "") },
                    modifier = Modifier.weight(1f)
                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        .onFocusChanged { if (it.isFocused) shouldAnimateText = false },
                    singleLine = true,
                    value = emojiInput,
                    onValueChange = { emojiInput = it },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Palette.OffWhite.color,
                        focusedContainerColor = Palette.OffWhite.color,
                        focusedIndicatorColor =
                            if (invalid && submittedType == SubmittedType.Emoji) Palette.Maroon.color
                            else Palette.Purple.color,
                        unfocusedIndicatorColor =
                            if (invalid && submittedType == SubmittedType.Emoji) Palette.Maroon.color
                            else Palette.Black.color,
                    ),
                )
                IconButton(
                    description = "Join community",
                    modifier = Modifier.size(60.dp),
                    onClick = {
                        if (submitting) return@IconButton
                        submitting = true
                        submittedType = SubmittedType.Emoji
                        val id = runCatching { durableIdFromEmojis(emojiInput) }
                            .onFailure { println(it) }
                            .getOrNull()
                        if (id == null) {
                            invalid = true
                            return@IconButton
                        }
                        scope.launch {
                            invalid = !onJoin(id)
                            submitting = false
                        }
                    },
                ) {
                    drawJoinCommunityIcon(it)
                }
            }
            IconButton(
                description = "Create a community",
                modifier = Modifier.size(60.dp),
                onClick = onCreate,
            ) {
                drawCreateCommunityIcon(it)
            }
        }
    }
}
