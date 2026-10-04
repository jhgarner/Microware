package com.example.microware

import androidx.compose.ui.graphics.Color

// Copied from the Pico8 color palette, but can be replaced or extended with something else if needed.
enum class Palette(val code: Int, val color: Color) {
    Black(0, Color(0xFF000000)),
    Navy(1, Color(0xFF1D2B53)),
    Maroon(2, Color(0xFF7E2553)),
    Forest(3, Color(0xFF008751)),
    Rust(4, Color(0xFFAB5236)),
    DarkGray(5, Color(0xFF5F574F)),
    LightGray(6, Color(0xFFC2C3C7)),
    OffWhite(7, Color(0xFFFFF1E8)),
    Red(8, Color(0xFFFF004D)),
    Orange(9, Color(0xFFFFA300)),
    Yellow(10, Color(0xFFFFEC27)),
    Green(11, Color(0xFF00E436)),
    Cyan(12, Color(0xFF29ADFF)),
    Purple(13, Color(0xFF83769C)),
    Pink(14, Color(0xFFFF77A8)),
    Tan(15, Color(0xFFFFCCAA)),
    Clear(16, Color(0x00000000));

    companion object {
        fun fromCode(code: Int): Palette = entries.firstOrNull { it.code == code }
            ?: error("Unknown palette code: $code")
    }

}