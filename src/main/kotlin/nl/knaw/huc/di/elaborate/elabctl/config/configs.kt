package nl.knaw.huc.di.elaborate.elabctl.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PageBreakEncoding {
    @SerialName("pilcrow")
    PILCROW,

    @SerialName("pageBreakMarker")
    PAGE_BREAK_MARKER,

    @SerialName("none")
    NONE
}

@Serializable
enum class ProjectType {
    @SerialName("letters")
    LETTERS,

    @SerialName("manuscript")
    MANUSCRIPT
}

@Serializable
data class ElabCtlConfig(
    val projectName: String,
    val title: String,
    val type: ProjectType,
    val editor: EditorConfig,
    val divRole: String,
    val pageBreakEncoding: PageBreakEncoding,
    val annoNumToRefTarget: String? = null,
    val letterDates: LetterDateConfig? = null,
    val letterMetadata: LetterMetadataConfig? = null
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
