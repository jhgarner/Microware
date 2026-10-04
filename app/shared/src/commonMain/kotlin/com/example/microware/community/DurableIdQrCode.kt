package com.example.microware.community

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

private const val qrPayloadPrefix = "mw-id:v1:"
private const val base64UrlAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
private const val base64BitsPerCharacter = 6
private const val base64Mask = 0x3F

fun encodeDurableIdQrPayload(durableId: DurableId): String =
    qrPayloadPrefix + encodeBase64Url(durableId.bits)

fun decodeDurableIdQrPayload(payload: String): DurableId {
    require(payload.startsWith(qrPayloadPrefix)) { "Unsupported QR code" }
    return DurableId(decodeBase64Url(payload.removePrefix(qrPayloadPrefix)))
}

@Composable
fun DurableIdQrCode(
    durableId: DurableId,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberQrCodePainter(
            data = encodeDurableIdQrPayload(durableId),
            errorCorrectionLevel = QrErrorCorrectionLevel.Medium,
        ),
        contentDescription = "QR code for durable ID",
        modifier = modifier,
    )
}

private fun encodeBase64Url(bits: Long): String {
    val value = bits.toULong()
    val encodedLength = (Long.SIZE_BITS + base64BitsPerCharacter - 1) / base64BitsPerCharacter
    return buildString(encodedLength) {
        var remainingBits = Long.SIZE_BITS
        while (remainingBits > 0) {
            val bitOffset = remainingBits - base64BitsPerCharacter
            val characterValue = if (bitOffset >= 0) {
                ((value shr bitOffset) and base64Mask.toULong()).toInt()
            } else {
                ((value shl -bitOffset) and base64Mask.toULong()).toInt()
            }
            append(base64UrlAlphabet[characterValue])
            remainingBits -= base64BitsPerCharacter
        }
    }
}

private fun decodeBase64Url(encoded: String): Long {
    val expectedLength = (Long.SIZE_BITS + base64BitsPerCharacter - 1) / base64BitsPerCharacter
    require(encoded.length == expectedLength) { "Invalid durable ID QR code" }

    var buffer = 0
    var bufferedBits = 0
    var decodedBits = 0uL
    var decodedByteCount = 0

    for (character in encoded) {
        val value = base64UrlAlphabet.indexOf(character)
        require(value >= 0) { "Invalid durable ID QR code" }

        buffer = (buffer shl base64BitsPerCharacter) or value
        bufferedBits += base64BitsPerCharacter
        if (bufferedBits >= Byte.SIZE_BITS) {
            bufferedBits -= Byte.SIZE_BITS
            val byte = (buffer shr bufferedBits) and 0xFF
            decodedBits = (decodedBits shl Byte.SIZE_BITS) or byte.toUByte().toULong()
            decodedByteCount++
            buffer = buffer and ((1 shl bufferedBits) - 1)
        }
    }

    require(decodedByteCount == Long.SIZE_BYTES && buffer == 0) {
        "Invalid durable ID QR code"
    }
    return decodedBits.toLong()
}
