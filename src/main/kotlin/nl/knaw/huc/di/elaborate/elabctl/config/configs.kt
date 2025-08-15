package nl.knaw.huc.di.elaborate.elabctl.config

import kotlinx.serialization.Serializable

@Serializable
data class ElabCtlConfig(
    val editor: EditorConfig
)

@Serializable
data class EditorConfig(
    val id: String,
    val name: String,
    val url: String,
)
