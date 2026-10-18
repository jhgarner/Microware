package com.example.microware.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.microware.core.AvatarHeight
import com.example.microware.core.AvatarWidth
import com.example.microware.core.BannerHeight
import com.example.microware.core.BannerWidth
import com.example.microware.core.Model
import com.example.microware.editor.sprite.EditableCanvas
import com.example.microware.editor.sprite.SpriteEditorTool
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.Sprite.Companion.load
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.icons.drawQrCode
import com.example.microware.ui.icons.drawPlayIcon
import com.example.microware.ui.icons.drawSaveIcon
import com.example.microware.util.Id
import com.example.microware.util.Identifiable
import com.example.microware.util.insetPadding
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

internal data class CommunityDraft(
    val serverId: Long?,
    val code: DurableId,
    val avatar: Avatar,
    val banner: Banner,
    override val id: Id,
) : Identifiable<CommunityDraft> {
    override val self: CommunityDraft get() = this

    override fun newId(id: Id) = copy(id = id)
}

internal fun Model.CommunityMetadata.toCommunityDraft() = CommunityDraft(
    serverId = id,
    code = DurableId(code),
    avatar = Avatar(icon.load()),
    banner = Banner(banner.load()),
    id = Identifiable.nextId(),
)

internal fun newCommunityDraft() = CommunityDraft(
    serverId = null,
    code = DurableId(),
    avatar = Avatar(Sprite.empty(AvatarWidth, AvatarHeight)),
    banner = Banner(Sprite.empty(BannerWidth, BannerHeight)),
    id = Identifiable.nextId(),
)

private sealed interface Tab {
    object Share : Tab
    object Experiences : Tab
    data class Artwork(val style: CommunityArtwork) : Tab
}

private enum class CommunityArtwork { Avatar, Banner }

@Composable
internal fun CommunityEditor(
    draft: CommunityDraft,
    onChange: (CommunityDraft) -> Unit,
    onCreate: suspend (CommunityDraft) -> Unit,
    onExperiencesTabChanged: (Boolean) -> Unit = {},
) {
    var tab by remember(draft.id) { mutableStateOf<Tab>(Tab.Share) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().insetPadding().height(72.dp),
        ) {
            IconButton(
                modifier = Modifier.size(64.dp),
                selected = tab == Tab.Share,
                onClick = {
                    tab = Tab.Share
                    onExperiencesTabChanged(false)
                },
            ) { drawQrCode() }
            IconButton(
                description = "Edit community experiences",
                modifier = Modifier.size(64.dp),
                selected = tab == Tab.Experiences,
                onClick = {
                    tab = Tab.Experiences
                    onExperiencesTabChanged(true)
                },
            ) { drawPlayIcon() }
            ArtworkButton(
                description = "Edit community avatar",
                sprite = draft.avatar.sprite,
                selected = tab == Tab.Artwork(CommunityArtwork.Avatar),
                onClick = {
                    tab = Tab.Artwork(CommunityArtwork.Avatar)
                    onExperiencesTabChanged(false)
                },
            )
            ArtworkButton(
                description = "Edit community banner",
                sprite = draft.banner.sprite,
                selected = tab == Tab.Artwork(CommunityArtwork.Banner),
                onClick = {
                    tab = Tab.Artwork(CommunityArtwork.Banner)
                    onExperiencesTabChanged(false)
                },
            )

            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                IconButton(
                    description = "Save community",
                    modifier = Modifier.size(64.dp),
                    onClick = { scope.launch { onCreate(draft) } },
                ) {
                    drawSaveIcon(it)
                }
            }
        }

        when (val tab = tab) {
            Tab.Share -> Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DurableIdQrCode(
                    durableId = draft.code,
                    modifier = Modifier.aspectRatio(1f).fillMaxWidth().padding(12.dp),
                )
                SelectionContainer {
                    BasicText(
                        durableIdToEmojis(draft.code),
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(),
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            is Tab.Artwork -> {
                val sprite = when (tab.style) {
                    CommunityArtwork.Avatar -> draft.avatar.sprite
                    CommunityArtwork.Banner -> draft.banner.sprite
                }
                EditableCanvas(
                    sprite = sprite,
                    commit = { edited ->
                        onChange(
                            when (tab.style) {
                                CommunityArtwork.Avatar -> draft.copy(avatar = Avatar(edited))
                                CommunityArtwork.Banner -> draft.copy(banner = Banner(edited))
                            },
                        )
                    },
                    resize = {},
                    modifier = Modifier.weight(1f),
                    editorKey = draft.id,
                    tools = listOf(SpriteEditorTool.Pen, SpriteEditorTool.Bucket),
                    showLogicTagPicker = false,
                    positionEditor = {},
                )
            }

            Tab.Experiences -> CommunityExperiences(draft.serverId)
        }
    }
}

@Composable
private fun ArtworkButton(
    description: String,
    sprite: Sprite,
    selected: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        description = description,
        modifier = Modifier.size(64.dp),
        selected = selected,
        onClick = onClick,
    ) {
        drawCommunityArtwork(sprite)
    }
}

private fun DrawScope.drawCommunityArtwork(sprite: Sprite) {
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
