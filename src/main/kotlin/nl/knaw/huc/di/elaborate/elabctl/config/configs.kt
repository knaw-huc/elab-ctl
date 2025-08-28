package nl.knaw.huc.di.elaborate.elabctl.config

import kotlinx.serialization.Serializable

@Serializable
data class ElabCtlConfig(
    val projectName: String,
    val editor: EditorConfig,
    val divRole: String
)

@Serializable
data class EditorConfig(
    val id: String,
    val name: String,
    val url: String,
)
