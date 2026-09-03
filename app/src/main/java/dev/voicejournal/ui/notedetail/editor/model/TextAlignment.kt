package dev.voicejournal.ui.notedetail.editor.model

enum class TextAlignment {
    Start,
    Center,
    End
}

data class AlignmentRange(
    val alignment: TextAlignment,
    val start: Int,
    val end: Int
) {
    init {
        require(start <= end) { "AlignmentRange start ($start) must be <= end ($end)" }
    }

    val length: Int get() = end - start
    val isEmpty: Boolean get() = start == end
}
