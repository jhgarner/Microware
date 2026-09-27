package com.example.microware.game.model

import com.example.microware.game.sprite.Palette

private val logicTagColors = Palette.entries.filter { it != Palette.Clear }
private const val logicTagShapeCount = 4
private val logicTagCount = logicTagColors.size * logicTagShapeCount

data class LogicTag(val tag: Int) {
    init {
        require(tag in 0..<logicTagCount) { "Invalid tag $tag" }
    }

    companion object {
        val all: List<LogicTag> = (0..<logicTagCount).map(::LogicTag)
    }
}
