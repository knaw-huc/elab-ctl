package nl.knaw.huc.di.elaborate.elabctl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EditionConfig(
    val baseURL: String,
    val textFont: String,
    val entryMetadataFields: ArrayList<String> = arrayListOf(),
    val thumbnails: MutableMap<String, List<String>>? = mutableMapOf(),
    val annotationIndex: String,
    val id: Int,
    val entryTermPlural: String,
    val title: String,
    val levels: ArrayList<String> = arrayListOf(),
    val textLayers: ArrayList<String> = arrayListOf(),
    val entries: ArrayList<EntryDescription> = arrayListOf(),
    val publicationDate: String,
    val entryTermSingular: String,
    val metadata: Map<String, String>? = mapOf()
)

@Serializable
data class EntryDescription(
    val datafile: String,
    val name: String,
    val shortName: String
)

@Serializable
data class Entry(
    val id: Int,
    val name: String,
    @SerialName("paralleltexts") val parallelTexts: Map<String, TextLayer> = mapOf(),
    val shortName: String,
    val facsimiles: ArrayList<Facsimile> = arrayListOf(),
    val metadata: ArrayList<Metadata> = arrayListOf()
)

@Serializable
data class Facsimile(
    val title: String,
    val thumbnail: String,
    val zoom: String
)

@Serializable
data class Metadata(
    val field: String,
    val value: String
)

@Serializable
data class TextLayer(
    val text: String,
    val annotationData: ArrayList<AnnotationData> = arrayListOf()
)

@Serializable
data class AnnotationData(
    val annotatedText: String,
    val type: AnnotationType,
    val text: String,
    val n: Long
)

@Serializable
data class AnnotationType(
    val id: Int,
    val name: String,
    val description: String,
    val metadata: Map<String, String>,
)