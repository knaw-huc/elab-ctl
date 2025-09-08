package nl.knaw.huc.di.elaborate.elabctl.config

import kotlinx.serialization.Serializable

enum class PageBreakEncoding {
    PILCROW,
    PAGE_BREAK_MARKER,
    NONE
}

@Serializable
data class ElabCtlConfig(
    val projectName: String,
    val editor: EditorConfig,
    val divRole: String,
    val pageBreakEncoding: PageBreakEncoding,
    val annoNumToRefTarget: String? = null,
    val letterDates: LetterDateConfig,
    val letterMetadata: LetterMetadataConfig
)

@Serializable
data class EditorConfig(
    val id: String,
    val name: String,
    val url: String,
)

@Serializable
data class LetterDateConfig(
    val earliestYear: Int,
    val latestYear: Int
)

@Serializable
data class LetterMetadataConfig(
    val sender: String,
    val senderPlace: String,
    val recipient: String,
    val recipientPlace: String? = null,
    val date: String,
    val language: String,
    val idno: String? = null,
    val settlement: String? = null,
    val institution: String? = null,
    val collection: String? = null,

)
