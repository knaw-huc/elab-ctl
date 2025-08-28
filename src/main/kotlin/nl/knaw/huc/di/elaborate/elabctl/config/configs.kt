package nl.knaw.huc.di.elaborate.elabctl.config

import kotlinx.serialization.Serializable

enum class PageBreakEncoding {
    PILCROW,
    PAGE_BREAK_MARKER
}

@Serializable
data class ElabCtlConfig(
    val projectName: String,
    val editor: EditorConfig,
    val divRole: String,
    val pageBreakEncoding: PageBreakEncoding
)

@Serializable
data class EditorConfig(
    val id: String,
    val name: String,
    val url: String,
)
