package com.example.microware.game.sprite

import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
internal data class StoredSprite(
    val width: Int,
    val height: Int,
    val encoding: Int,
    val pixels: String,
)

@OptIn(ExperimentalEncodingApi::class)
internal object SpriteCodec {
    private const val CurrentEncoding = 1

    fun encode(sprite: Sprite): StoredSprite = StoredSprite(
        width = sprite.width,
        height = sprite.height,
        encoding = CurrentEncoding,
        pixels = Base64.encode(sprite.packedBytes()),
    )

    fun decode(stored: StoredSprite): Sprite {
        require(stored.encoding == CurrentEncoding) {
            "Unsupported sprite encoding: ${stored.encoding}"
        }
        val bytes = Base64.decode(stored.pixels)
        return Sprite.fromPackedBytes(stored.width, stored.height, bytes)
    }
}
