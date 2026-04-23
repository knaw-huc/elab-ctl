package nl.knaw.huc.di.elaborate.elabctl.manifests

import java.net.URI
import info.freelibrary.iiif.presentation.v3.AnnotationPage
import info.freelibrary.iiif.presentation.v3.Canvas
import info.freelibrary.iiif.presentation.v3.ImageContent
import info.freelibrary.iiif.presentation.v3.Manifest
import info.freelibrary.iiif.presentation.v3.PaintingAnnotation
import info.freelibrary.iiif.presentation.v3.properties.I18n
import info.freelibrary.iiif.presentation.v3.properties.Label
import info.freelibrary.iiif.presentation.v3.properties.Metadata
import info.freelibrary.iiif.presentation.v3.properties.Value
import info.freelibrary.iiif.presentation.v3.services.ImageService3
import nl.knaw.huc.di.elaborate.elabctl.archiver.EditionConfig
import nl.knaw.huc.di.elaborate.elabctl.archiver.FacsimileDimensionsFactory.FacsimileDimensions

class ManifestV3Factory(val manifestBaseUrl: String, val iiifBaseUrl: String) {

    data class PMetadata(
        val baseName: String,
        val iiifBaseUrl: String,
        val manifestUrl: URI
    )

    fun forEntry(
        entryName: String,
        facsimileDimensions: List<FacsimileDimensions>,
        config: EditionConfig
    ): Pair<Manifest, ManifestGenerator.LetterMetadata> {
        val manifestId = "$manifestBaseUrl/$entryName.json"
        val metadata = listOf(
            Metadata(Label("en", "Collection"), Value(I18n("en", config.title))),
            Metadata(Label("en", "Section"), Value(I18n("en", entryName))),
        )

        val pairs = facsimileDimensions.toCanvases(manifestId)
        val entryMetadata = ManifestGenerator.LetterMetadata(
            id = entryName,
            manifest = manifestId,
            pageMetadata = pairs.map { it.second }
                .associate {
                    it.baseName to ManifestGenerator.PageMetadata(
                        it.iiifBaseUrl,
                        it.manifestUrl.toString()
                    )
                }
        )

        return Pair(
            Manifest(manifestId, Label("en", entryName))
                .setRights(LICENSE)
//            .setBehaviors(ManifestBehavior.PAGED)
                .setMetadata(metadata)
                .setCanvases(pairs.map { it.first }),
            entryMetadata
        )
    }

    fun forProject(
        projectName: String,
        config: EditionConfig,
        groups: Map<String, List<FacsimileDimensions>>
    ): Manifest {
        val manifestId = "$manifestBaseUrl/$projectName.json"
        val projectTitle = config.title
        val facsimileDimensions = groups.values.flatten()
        val metadata = listOf(
            Metadata(Label("en", "Name"), Value(I18n("en", projectTitle)))
        )
        return Manifest(manifestId, Label("en", projectTitle))
            .setRights(LICENSE)
//            .setBehaviors(ManifestBehavior.PAGED)
            .setMetadata(metadata)
            .setCanvases(facsimileDimensions.toCanvases(manifestId).map { it.first })
    }

    private fun List<FacsimileDimensions>.toCanvases(manifestId: String): List<Pair<Canvas, PMetadata>> =
        sortedBy { it.fileName }
            .mapIndexed { i, facsimileDimensions ->
                val canvasTitle = facsimileDimensions.fileName
                val canvas =
                    Canvas("$manifestId#canvas-$i", Label("en", canvasTitle))
                        .setWidthHeight(facsimileDimensions.width, facsimileDimensions.height)
                val page = AnnotationPage<PaintingAnnotation>("$manifestId#page-$i")
                val imageUrl = "${iiifBaseUrl}${facsimileDimensions.fileName}"
                val annotation =
                    PaintingAnnotation("$manifestId#annotation-${facsimileDimensions.fileName}", canvas).apply {
                        setChoice(true).bodies.add(
                            ImageContent("$imageUrl/full/max/0/default.jpg")
                                .setWidthHeight(facsimileDimensions.width, facsimileDimensions.height)
                                .setServices(ImageService3(ImageService3.Profile.LEVEL_ONE, imageUrl))
                        )
                    }
                canvas.paintingPages.add(page.addAnnotations(annotation))
                val pageMetadata = PMetadata(
                    baseName = facsimileDimensions.fileName.substringBeforeLast("."),
                    iiifBaseUrl = imageUrl,
                    manifestUrl = canvas.id
                )
                Pair(canvas, pageMetadata)
            }

    companion object {
        const val LICENSE = "http://creativecommons.org/licenses/by-nc/4.0/"
    }
}