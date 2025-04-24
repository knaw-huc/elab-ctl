package nl.knaw.huc.di.elaborate.elabctl.manifests

import info.freelibrary.iiif.presentation.v3.AnnotationPage
import info.freelibrary.iiif.presentation.v3.Canvas
import info.freelibrary.iiif.presentation.v3.ImageContent
import info.freelibrary.iiif.presentation.v3.Manifest
import info.freelibrary.iiif.presentation.v3.PaintingAnnotation
import info.freelibrary.iiif.presentation.v3.properties.I18n
import info.freelibrary.iiif.presentation.v3.properties.Label
import info.freelibrary.iiif.presentation.v3.properties.Metadata
import info.freelibrary.iiif.presentation.v3.properties.Value
import info.freelibrary.iiif.presentation.v3.properties.behaviors.ManifestBehavior
import info.freelibrary.iiif.presentation.v3.services.ImageService3
import nl.knaw.huc.di.elaborate.elabctl.archiver.FacsimileDimensionsFactory

class ManifestV3Factory(val manifestBaseUrl: String, val iiifBaseUrl: String) {

    fun forEntry(
        entryName: String,
        facsimileDimensions: List<FacsimileDimensionsFactory.FacsimileDimensions>
    ): Manifest {
        val manifestId = "$manifestBaseUrl/$entryName-manifest.json"
        val manifest = Manifest(manifestId, Label("en", "Pages"))
            .setRights("http://creativecommons.org/licenses/by/4.0/")
            .setBehaviors(ManifestBehavior.PAGED)

        manifest.metadata = listOf(
            Metadata(Label("en", "EntryName"), Value(I18n("en", entryName)))
        )

        val canvases = facsimileDimensions.sortedBy { it.fileName }
            .mapIndexed { i, facsimileDimensions ->
                val imageBaseName = facsimileDimensions.fileName.substringBeforeLast(".")
                val canvas =
                    Canvas("$manifestId#canvas-$i", Label("en", imageBaseName))
                        .setWidthHeight(facsimileDimensions.width, facsimileDimensions.height)
                val page = AnnotationPage<PaintingAnnotation>("$manifestId#page-$i")
                val imageUrl = "$iiifBaseUrl${facsimileDimensions.fileName}"
                val annotation =
                    PaintingAnnotation("$manifestId#annotation-${facsimileDimensions.fileName}", canvas).apply {
                        setChoice(true).bodies.add(
                            ImageContent("$imageUrl/full/max/0/default.jpg")
                                .setWidthHeight(facsimileDimensions.width, facsimileDimensions.height)
                                .setServices(ImageService3(ImageService3.Profile.LEVEL_ONE, imageUrl))
                        )
                    }
                canvas.paintingPages.add(page.addAnnotations(annotation))
                canvas
            }

        manifest.canvases = canvases

        return manifest
    }
}