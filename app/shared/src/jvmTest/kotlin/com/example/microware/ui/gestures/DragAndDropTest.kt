package com.example.microware.ui.gestures

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.example.microware.editor.world.WorldEditorContent
import com.example.microware.editor.world.rememberWorldEditorState
import com.example.microware.game.model.Obj
import com.example.microware.game.model.World
import com.example.microware.ui.layout.Insets
import com.example.microware.ui.layout.LocalInsets
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DragAndDropTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val dropped = mutableStateOf<Offer<String>?>(null)

    private fun setDraggableContent(covered: Boolean) {
        val controller = DragController<String>()
        composeRule.setContent {
            Box(
                Modifier.fillMaxSize()
                    .dragObserver(controller)
                    .dropTarget(controller, onGiven = { dropped.value = it }),
            ) {
                Box(Modifier.size(100.dp).testTag("source").dragSource(controller, Unit, "item"))
                if (covered) Box(Modifier.size(100.dp).observeWithoutConsuming())
            }
        }
    }

    private fun Modifier.observeWithoutConsuming() = pointerInput(Unit) {
        awaitPointerEventScope { while (true) awaitPointerEvent() }
    }

    private fun dragFrom(tag: String, fraction: Offset, to: Offset) {
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val start = bounds.topLeft + Offset(bounds.width * fraction.x, bounds.height * fraction.y)
        composeRule.onRoot().performTouchInput { swipe(start, to, durationMillis = 200) }
        composeRule.waitForIdle()
    }

    @Test
    fun dropRemembersWhereTheSourceWasGripped() {
        setDraggableContent(covered = false)

        dragFrom("source", Offset(.25f, .75f), to = Offset(300f, 300f))

        val offer = dropped.value!!
        assertEquals("item", offer.item)
        assertEquals(.25f, offer.grip.x, .01f)
        assertEquals(.75f, offer.grip.y, .01f)
    }

    @Test
    fun coveredSourceCannotBeDragged() {
        setDraggableContent(covered = true)

        dragFrom("source", Offset(.5f, .5f), to = Offset(300f, 300f))

        assertNull(dropped.value)
    }

    @Test
    fun spriteLandsUnderTheGripWhateverSizeItsSourceWas() {
        val world = mutableStateOf(World())
        val sprite = Obj(8, 8)
        composeRule.setContent {
            CompositionLocalProvider(LocalInsets provides Insets.new()) {
                val state = rememberWorldEditorState()
                Box(Modifier.size(600.dp, 800.dp).dragObserver(state.dragController)) {
                    WorldEditorContent(
                        world = world.value,
                        handler = { world.value = it.modifyWorld(world.value) },
                        state = state,
                    )
                    Box(
                        Modifier.size(64.dp).testTag("large")
                            .dragSource(state.dragController, Unit, sprite),
                    )
                    Box(
                        Modifier.align(Alignment.TopEnd).size(8.dp).testTag("small")
                            .dragSource(state.dragController, Unit, sprite),
                    )
                }
            }
        }

        val target = Offset(300f, 350f)
        dragFrom("large", Offset(.5f, .5f), to = target)
        dragFrom("small", Offset(.5f, .5f), to = target)

        val (fromLarge, fromSmall) = world.value.objects
        assertEquals(fromLarge.offset.x, fromSmall.offset.x, .01f)
        assertEquals(fromLarge.offset.y, fromSmall.offset.y, .01f)
    }
}
