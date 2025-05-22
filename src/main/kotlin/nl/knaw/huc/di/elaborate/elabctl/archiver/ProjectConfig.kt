package nl.knaw.huc.di.elaborate.elabctl.archiver

data class ProjectConfig(
    val projectName: String,
    val personIds: Map<String, String> = emptyMap(),
    val divTypeForLayerName: Map<String, String> = emptyMap()
)
