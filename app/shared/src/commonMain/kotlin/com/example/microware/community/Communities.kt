package com.example.microware.community

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import com.example.microware.ui.components.DrawerHandleState
import com.example.microware.ui.components.DrawerWithHandle
import com.example.microware.ui.components.Grid
import com.example.microware.ui.components.HandleEdge
import com.example.microware.ui.components.HandleSize
import com.example.microware.ui.components.HandleType
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.components.Scrim
import com.example.microware.ui.gestures.DragController
import com.example.microware.ui.gestures.Offer
import com.example.microware.ui.gestures.dragObserver
import com.example.microware.ui.layout.insets
import com.example.microware.updateCommunity
import com.example.microware.util.toDp
import io.ktor.http.HttpStatusCode
import kotlin.math.roundToInt

private sealed interface CommunityPanel {
    data object Join : CommunityPanel
    data class Edit(val draft: CommunityDraft) : CommunityPanel
}

@Composable
fun Communities() {
    var communities by remember { mutableStateOf(listOf<CommunityDraft>()) }
    var refresh by remember { mutableStateOf(0) }
    var panel by remember { mutableStateOf<CommunityPanel?>(null) }
    val dragController = remember { DragController<CommunityDraft>() }

    LaunchedEffect(refresh) {
        communities = getCommunityMetadata().map { it.toCommunityDraft() }
    }
    SystemBackHandler(enabled = panel != null) { panel = null }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().then(
            Modifier.dragObserver(dragController),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(
                start = insets.leftDp() + 32.dp,
                end = insets.rightDp() + 32.dp,
                top = insets.topDp() + 32.dp,
                bottom = insets.bottomDp(),
            ),
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
                            onClick = { panel = CommunityPanel.Edit(community) },
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

        Scrim()

        val handleType = if (dragController.offer != null || panel is CommunityPanel.Edit) {
            HandleType.Edit
        } else {
            HandleType.Create
        }
        val currentPanel = panel
        val handleState: DrawerHandleState<CommunityDraft, CommunityPanel> = when {
            dragController.offer != null -> DrawerHandleState.Dragging(
                dragTarget = dragController,
                onGiven = { offer -> panel = CommunityPanel.Edit(offer.item) },
            )

            currentPanel != null -> DrawerHandleState.Expanded(HandleEdge.Bottom, currentPanel)
            else -> DrawerHandleState.Waiting()
        }
        DrawerWithHandle(
            handleType = handleType,
            state = handleState,
            setExpanded = { edge ->
                panel = when {
                    edge == null -> null
                    panel == null -> CommunityPanel.Join
                    else -> panel
                }
            },
        ) { selectedPanel ->
            when (selectedPanel) {
                CommunityPanel.Join -> JoinCommunity(
                    onJoin = { id ->
                        val joined = joinCommunity(id) == HttpStatusCode.OK
                        refresh++
                        if (joined) panel = null
                        joined
                    },
                    onCreate = { panel = CommunityPanel.Edit(newCommunityDraft()) },
                )

                is CommunityPanel.Edit -> CommunityEditor(
                    draft = selectedPanel.draft,
                    onChange = { updated ->
                        panel = CommunityPanel.Edit(updated)
                    },
                    onCreate = { draft ->
                        val serverId = draft.serverId ?: createCommunity()
                        val status =
                            updateCommunity(serverId, DurableId(), draft.avatar, draft.banner)
                        if (status == HttpStatusCode.OK) panel = null
                        refresh++
                    },
                )
            }
        }

        dragController.offer?.let { CommunityDragGhost(it) }
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
