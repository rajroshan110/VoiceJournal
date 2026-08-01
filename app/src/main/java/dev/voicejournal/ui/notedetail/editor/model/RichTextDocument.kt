package dev.voicejournal.ui.notedetail.editor.model

data class RichTextDocument(
    val text: String = "",
    val spans: List<SpanRange> = emptyList(),
    val paragraphs: List<ParagraphRange> = emptyList()
) {
    val length: Int get() = text.length
    val isEmpty: Boolean get() = text.isEmpty()

    companion object {
        val EMPTY = RichTextDocument()
    }
}
