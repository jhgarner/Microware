package com.example.microware.community

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.microware.core.BannerHeight
import com.example.microware.core.BannerWidth
import com.example.microware.core.Model
import com.example.microware.editor.sprite.EditableCanvas
import com.example.microware.editor.sprite.SpriteEditorTool
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.Sprite.Companion.load
import com.example.microware.getExperienceMetadatas
import com.example.microware.saveExperienceMetadata
import com.example.microware.ui.components.DrawerHandleState
import com.example.microware.ui.components.DrawerWithHandle
import com.example.microware.ui.components.Grid
import com.example.microware.ui.components.HandleEdge
import com.example.microware.ui.components.HandleSize
import com.example.microware.ui.components.HandleType
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.gestures.DragController
import com.example.microware.ui.gestures.Offer
import com.example.microware.ui.gestures.dragObserver
import com.example.microware.ui.icons.drawSaveIcon
import com.example.microware.util.Id
import com.example.microware.util.Identifiable
import com.example.microware.util.insetPadding
import com.example.microware.util.toDp
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

private data class ExperienceDraft(
    val serverId: Long?,
    val communityId: Long,
    val banner: Banner,
    override val id: Id,
) : Identifiable<ExperienceDraft> {
    override val self: ExperienceDraft get() = this
    override fun newId(id: Id) = copy(id = id)
}

private fun Model.ExperienceMetadata.toDraft(communityId: Long) = ExperienceDraft(
    serverId = id,
    communityId = communityId,
    banner = Banner(banner.load()),
    id = Identifiable.nextId(),
)

private fun newExperienceDraft(communityId: Long) = ExperienceDraft(
    serverId = null,
    communityId = communityId,
    banner = Banner(Sprite.empty(BannerWidth, BannerHeight)),
    id = Identifiable.nextId(),
)

@Composable
internal fun CommunityExperiences(communityId: Long?) {
    var experiences by remember(communityId) { mutableStateOf(emptyList<ExperienceDraft>()) }
    var selectedDraft by remember(communityId) { mutableStateOf<ExperienceDraft?>(null) }
    var refresh by remember { mutableStateOf(0) }
    val dragController = remember { DragController<ExperienceDraft>() }
    LaunchedEffect(communityId, refresh) {
        experiences = communityId?.let { id ->
            runCatching { getExperienceMetadatas(id).map { it.toDraft(id) } }.getOrElse { listOf() }
        }.orEmpty()
    }

    BoxWithConstraints(Modifier.fillMaxSize().dragObserver(dragController)) {
        Column(Modifier.fillMaxSize().insetPadding()) {
            Box(Modifier.weight(1f)) {
                Grid(
                    items = experiences,
                    itemSize = DpSize(120.dp, 64.dp),
                    dragSource = dragController,
                    dragTarget = dragController,
                    onMove = { _, _ -> },
                    preview = { experience ->
                        IconButton(
                            description = "Edit experience",
                            aspectRatio = BannerWidth.toFloat() / BannerHeight,
                            onClick = { selectedDraft = experience },
                        ) {
                            drawExperienceBanner(experience.banner.sprite)
                        }
                    },
                )
            }
            Spacer(Modifier.height(HandleSize))
        }

        val offer = dragController.offer
        val handleState: DrawerHandleState<ExperienceDraft, ExperienceDraft>? = when {
            offer != null -> DrawerHandleState.Dragging(
                dragTarget = dragController,
                onGiven = { selectedDraft = it.item },
            )

            selectedDraft != null -> DrawerHandleState.Expanded(HandleEdge.Bottom, selectedDraft)
            communityId != null -> DrawerHandleState.Waiting(
                extraOnExpand = { selectedDraft = newExperienceDraft(communityId) },
            )

            else -> null
        }
        DrawerWithHandle(
            handleType = if (offer == null) HandleType.Create else HandleType.Edit,
            state = handleState,
            setExpanded = { if (it == null) selectedDraft = null },
            drawerContent = { draft ->
                ExperienceEditor(
                    draft = draft,
                    onChange = { selectedDraft = it },
                    onSave = { edited ->
                        saveExperienceMetadata(
                            id = edited.serverId,
                            communityId = edited.communityId,
                            banner = edited.banner,
                        )
                        selectedDraft = null
                        refresh++
                    },
                )
            },
        )
        offer?.let { currentOffer ->
            dragController.rootCoordinates.value?.let { root ->
                ExperienceDragGhost(
                    offer = currentOffer,
                    topLeft = root.windowToLocal(currentOffer.offset - currentOffer.localOffset),
                )
            }
        }
    }
}

@Composable
private fun ExperienceEditor(
    draft: ExperienceDraft,
    onChange: (ExperienceDraft) -> Unit,
    onSave: suspend (ExperienceDraft) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(64.dp)) {
            Spacer(Modifier.weight(1f))
            IconButton(
                description = "Save experience",
                modifier = Modifier.size(64.dp),
                onClick = { scope.launch { onSave(draft) } },
            ) {
                drawSaveIcon(it)
            }
        }
        EditableCanvas(
            sprite = draft.banner.sprite,
            commit = { onChange(draft.copy(banner = Banner(it))) },
            resize = {},
            modifier = Modifier.weight(1f),
            editorKey = draft.id,
            tools = listOf(SpriteEditorTool.Pen, SpriteEditorTool.Bucket),
            showLogicTagPicker = false,
            positionEditor = {},
        )
    }
}

private fun DrawScope.drawExperienceBanner(sprite: Sprite) {
    val scale = min(size.width / sprite.width, size.height / sprite.height)
    val width = (sprite.width * scale).roundToInt()
    val height = (sprite.height * scale).roundToInt()
    drawImage(
        image = sprite.bitmap,
        dstOffset = IntOffset(
            ((size.width - width) / 2f).roundToInt(),
            ((size.height - height) / 2f).roundToInt(),
        ),
        dstSize = IntSize(width, height),
        filterQuality = FilterQuality.None,
    )
}

@Composable
private fun ExperienceDragGhost(offer: Offer<ExperienceDraft>, topLeft: Offset) {
    Canvas(
        modifier = Modifier
            .offset {
                IntOffset(
                    topLeft.x.roundToInt(),
                    topLeft.y.roundToInt(),
                )
            }
            .size(width = offer.size.width.toDp(), height = offer.size.height.toDp())
            .zIndex(.5f),
    ) {
        drawExperienceBanner(offer.item.banner.sprite)
    }
}
