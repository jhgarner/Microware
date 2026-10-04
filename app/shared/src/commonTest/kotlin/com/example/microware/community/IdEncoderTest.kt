package com.example.microware.community

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IdEncoderTest {
    @Test
    fun examples() {
        infix fun Long.toMatch(emojis: String) {
            assertEquals(durableIdToEmojis(DurableId(this)), emojis)
        }

        1L toMatch "😃❤️"
        511L toMatch "❤️😃"
        512L toMatch "😃😀❤️"
        (1_735_689_600_000L shl 16) toMatch "😂🦴🛕🍑🦘😀😀🥒"
        Long.MAX_VALUE toMatch "❤️❤️❤️❤️❤️❤️❤️🤣"
        Long.MIN_VALUE toMatch "😃😀😀😀😀😀😀😀❤️"
        (-1L) toMatch "😃❤️❤️❤️❤️❤️❤️❤️😂"
    }

    @Test
    fun roundTripsDurableIdsAcrossLongValues() {
        val ids = listOf(
            1L,
            511L,
            512L,
            1_735_689_600_000L shl 16,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            -1L,
        )

        ids.forEach { bits ->
            val id = DurableId(bits)
            assertEquals(id, durableIdFromEmojis(durableIdToEmojis(id)))
        }
    }

    @Test
    fun rejectsAnEmojiTypo() {
        val encoded = durableIdToEmojis(DurableId(1L))
        val typo = encoded.replaceFirst("😃", "😀")

        assertFailsWith<IllegalArgumentException> {
            durableIdFromEmojis(typo)
        }
    }

    @Test
    fun rejectsUnknownCharacters() {
        assertFailsWith<IllegalStateException> {
            durableIdFromEmojis("💯")
        }
    }
}
