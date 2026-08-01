package dev.voicejournal.ui.notedetail.editor.model

sealed interface ParagraphType {
    data class Heading(val level: Int = 1) : ParagraphType
    object BulletList : ParagraphType
    data class NumberedList(val number: Int = 1) : ParagraphType
    object Quote : ParagraphType
}

data class ParagraphRange(
    val type: ParagraphType,
    val start: Int,
    val end: Int
) {
    init {
        require(start <= end) { "ParagraphRange start ($start) must be <= end ($end)" }
    }

    val length: Int get() = end - start
    val isEmpty: Boolean get() = start == end
}
