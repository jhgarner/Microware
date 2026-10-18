package com.example.microware.community

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.zIndex
import com.example.microware.createCommunity
import com.example.microware.getCommunityMetadata
import com.example.microware.joinCommunity
import com.example.microware.platform.SystemBackHandler
import com.example.microware.ui.components.ButtonHandle
import com.example.microware.ui.components.DrawerButtonState
import com.example.microware.ui.components.Grid
import com.example.microware.ui.components.HandlePresentation
import com.example.microware.ui.components.HandleSize
import com.example.microware.ui.components.HandleType
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.gestures.DragController
import com.example.microware.ui.gestures.Offer
import com.example.microware.ui.gestures.dragObserver
import com.example.microware.ui.gestures.dropTarget
import com.example.microware.updateCommunity
import com.example.microware.util.insetPadding
import com.example.microware.util.toDp
import io.ktor.http.HttpStatusCode
import kotlin.math.roundToInt

private sealed interface CommunityPanel {
    data object Join : CommunityPanel
    data class Edit(val draft: CommunityDraft, val experiences: Boolean = false) : CommunityPanel
}

@Composable
fun Communities() {
    var communities by remember { mutableStateOf(listOf<CommunityDraft>()) }
    var refresh by remember { mutableStateOf(0) }
    var panel by remember { mutableStateOf(listOf<CommunityPanel>()) }

    LaunchedEffect(refresh) {
        communities = runCatching { getCommunityMetadata() }.getOrElse { listOf() }
            .map { it.toCommunityDraft() }
    }
    SystemBackHandler(enabled = panel.isNotEmpty()) { panel = panel.dropLast(1) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val currentPanel = panel.lastOrNull()) {
            null -> {
                val dragController = remember { DragController<CommunityDraft>() }
                Column(
                    modifier = Modifier.dragObserver(dragController).insetPadding(),
                ) {
                    Box(Modifier.weight(1f)) {
                        Grid(
                            items = communities,
                            itemSize = DpSize(100.dp, 100.dp),
                            dragSource = dragController,
                            dragTarget = dragController,
                            onMove = { _, _ -> },
                            preview = { community ->
                                IconButton(
                                    description = "Edit community",
                                    onClick = {
                                        panel += listOf(CommunityPanel.Edit(community))
                                    },
                                ) {
                                    drawImage(
                                        image = community.avatar.sprite.bitmap,
                                        dstSize = size.toIntSize(),
                                        filterQuality = FilterQuality.None,
                                    )
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(HandleSize))
                }

                dragController.offer?.let { CommunityDragGhost(it) }

                val isDragging = dragController.offer != null
                var isHovered by remember { mutableStateOf(false) }
                ButtonHandle(
                    type = if (isDragging) HandleType.Edit else HandleType.Create,
                    highlight = isHovered,
                    state = if (isDragging) {
                        DrawerButtonState.Dragging
                    } else {
                        DrawerButtonState.Waiting {
                            panel += listOf(CommunityPanel.Join)
                        }
                    },
                    modifier = Modifier.dropTarget(
                        dragController = dragController,
                        onGiven = { panel += listOf(CommunityPanel.Edit(it.item)) },
                        onEnter = { isHovered = true },
                        onExit = { isHovered = false },
                        onEnd = { isHovered = false },
                    ),
                    presentation = HandlePresentation.FloatingAction,
                )
            }

            CommunityPanel.Join -> {
                JoinCommunity(
                    onJoin = { id ->
                        val joined = joinCommunity(id) == HttpStatusCode.OK
                        refresh++
                        if (joined) panel = listOf()
                        joined
                    },
                    onCreate = { panel += listOf(CommunityPanel.Edit(newCommunityDraft())) },
                )
                ButtonHandle(HandleType.Create)
            }

            is CommunityPanel.Edit -> {
                CommunityEditor(
                    draft = currentPanel.draft,
                    onChange = { updated ->
                        panel = panel.dropLast(1) + currentPanel.copy(draft = updated)
                    },
                    onCreate = { draft ->
                        val serverId = draft.serverId ?: createCommunity()
                        val status =
                            updateCommunity(serverId, DurableId(), draft.avatar, draft.banner)
                        if (status == HttpStatusCode.OK) panel = listOf()
                        refresh++
                    },
                    onExperiencesTabChanged = { showing ->
                        panel = panel.dropLast(1) + currentPanel.copy(experiences = showing)
                    },
                )
                if (!currentPanel.experiences) ButtonHandle(HandleType.Edit)
            }
        }
    }
}

@Composable
private fun CommunityDragGhost(offer: Offer<CommunityDraft>) {
    Canvas(
        modifier = Modifier
            .offset {
                IntOffset(
                    (offer.offset.x - offer.localOffset.x).roundToInt(),
                    (offer.offset.y - offer.localOffset.y).roundToInt(),
                )
            }
            .size(width = offer.size.width.toDp(), height = offer.size.height.toDp())
            .zIndex(.5f),
    ) {
        drawImage(
            image = offer.item.avatar.sprite.bitmap,
            dstSize = offer.size,
            filterQuality = FilterQuality.None,
        )
    }
}
