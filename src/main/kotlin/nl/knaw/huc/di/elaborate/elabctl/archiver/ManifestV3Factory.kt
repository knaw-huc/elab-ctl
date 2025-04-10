package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.net.URI
import info.freelibrary.iiif.presentation.v3.Annotation
import info.freelibrary.iiif.presentation.v3.AnnotationPage
import info.freelibrary.iiif.presentation.v3.Canvas
import info.freelibrary.iiif.presentation.v3.Manifest
import info.freelibrary.iiif.presentation.v3.PaintingAnnotation
import info.freelibrary.iiif.presentation.v3.properties.I18n
import info.freelibrary.iiif.presentation.v3.properties.Label
import info.freelibrary.iiif.presentation.v3.properties.Metadata
import info.freelibrary.iiif.presentation.v3.properties.Summary
import info.freelibrary.iiif.presentation.v3.properties.Value

object ManifestV3Factory {

    fun createFromImages(imagePaths: List<String>): String {
        val canvas = Canvas("url")
        return canvas.toString()
    }

    fun manifest(): String {
        val manifest = Manifest("url", Label("en", "Letters"))
        manifest.metadata = listOf(
            Metadata(Label("en", "Filename"), Value("en", "letters/RM01.json"))
        )
        manifest.summary = Summary(I18n("en", "Letters"))
        manifest.rights = URI("http://creativecommons.org/publicdomain/zero/1.0/")
        val canvas1 = Canvas("canvas-id", Label("en", "VGM001001681_01_n"))
            .setWidthHeight(2115, 1270)
        val page1 = AnnotationPage<PaintingAnnotation>("page-id")
        val annotation = PaintingAnnotation("annotation-id",canvas1)
        canvas1.paintingPages.add(page1.addAnnotations(annotation))
        manifest.canvases = listOf(
            canvas1
        )

        return manifest.toString()
    }

}