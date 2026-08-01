package dev.voicejournal.ui.notedetail.editor.model

sealed interface SpanType {
    object Bold : SpanType
    object Italic : SpanType
    object Underline : SpanType
    object Strikethrough : SpanType
    object Code : SpanType
    data class Highlight(val colorArgb: Long = 0xFFFFEB3BL) : SpanType
    data class Link(val url: String) : SpanType
}

data class SpanRange(
    val type: SpanType,
    val start: Int,
    val end: Int
) {
    init {
        require(start <= end) { "SpanRange start ($start) must be <= end ($end)" }
    }

    val length: Int get() = end - start
    val isEmpty: Boolean get() = start == end

    fun intersects(rangeStart: Int, rangeEnd: Int): Boolean {
        return start < rangeEnd && end > rangeStart
    }

    fun contains(index: Int): Boolean {
        return index in start until end
    }
}
