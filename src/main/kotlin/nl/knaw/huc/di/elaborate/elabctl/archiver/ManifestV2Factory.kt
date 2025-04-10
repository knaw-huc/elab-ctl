package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale
import com.fasterxml.jackson.databind.ObjectWriter
import de.digitalcollections.iiif.model.GenericService
import de.digitalcollections.iiif.model.ImageContent
import de.digitalcollections.iiif.model.MetadataEntry
import de.digitalcollections.iiif.model.OtherContent
import de.digitalcollections.iiif.model.PropertyValue
import de.digitalcollections.iiif.model.enums.ViewingDirection
import de.digitalcollections.iiif.model.enums.ViewingHint
import de.digitalcollections.iiif.model.image.ImageApiProfile
import de.digitalcollections.iiif.model.image.ImageService
import de.digitalcollections.iiif.model.image.TileInfo
import de.digitalcollections.iiif.model.jackson.IiifObjectMapper
import de.digitalcollections.iiif.model.sharedcanvas.AnnotationList
import de.digitalcollections.iiif.model.sharedcanvas.Canvas
import de.digitalcollections.iiif.model.sharedcanvas.Collection
import de.digitalcollections.iiif.model.sharedcanvas.Layer
import de.digitalcollections.iiif.model.sharedcanvas.Manifest
import de.digitalcollections.iiif.model.sharedcanvas.Range
import de.digitalcollections.iiif.model.sharedcanvas.Sequence

object ManifestV2Factory {

    val WRITER: ObjectWriter = IiifObjectMapper().writerWithDefaultPrettyPrinter()

    fun createFromImages(imagePaths: List<String>): String {
        val canvas = Canvas("url").apply {
            addLabel("A Label")
            addDescription("")
        }
        return WRITER.writeValueAsString(canvas)
    }

    fun manifest(): String {
        val manifest =
            Manifest("http://example.org/iiif/book1/manifest", "Book 1").apply {
                addMetadata("Author", "Anne Author")
                addMetadata(MetadataEntry(PropertyValue("Published"), PropertyValue().apply {
                    addValue(Locale.ENGLISH, "Paris, circa 1400")
                    addValue(Locale.FRENCH, "Paris, environ 14eme siecle")
                }))
                addDescription("A longer description of this example book. It should give some real information.")
                navDate = OffsetDateTime.of(1856, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
                addLicense("https://creativecommons.org/publicdomain/zero/1.0/")
                addAttribution("Provided by Example Organization")

                addService(
                    GenericService(
                        "http://example.org/ns/jsonld/context.json",
                        "http://example.org/service/example",
                        "http://example.org/docs/example-service.html"
                    )
                )
                addSeeAlso(
                    OtherContent(
                        "http://example.org/library/catalog/book1.marc",
                        "application/marc",
                        "http://example.org/profiles/marc21"
                    )
                )
                addRendering(
                    OtherContent("http://example.org/iiif/book1.pdf").apply {
                        addLabel("Download as PDF")
                    }
                )
                addWithin(Collection("http://example.org/collections/books/"))
            }

        val seq = Sequence("http://example.org/iiif/book1/sequence/normal").apply {
            addLabel("Current Page Order")
            viewingDirection = ViewingDirection.LEFT_TO_RIGHT
            addViewingHint(ViewingHint.PAGED)
            addCanvas(Canvas("http://example.org/iiif/book1/canvas/p1", "p. 1").apply {
                this.width = 750
                this.height = 1000
                addImage(ImageContent("http://example.org/iiif/book1/res/page1.jpg").apply {
                    this.width = 1500
                    this.height = 2000
                    addService(
                        ImageService("http://example.org/images/book1-page1", ImageApiProfile.LEVEL_ONE)
                    )
                })
                addOtherContent(AnnotationList("http://example.org/iiif/book1/list/p1").apply {
                    addWithin(Layer("http://example.org/iiif/book1/layer/l1", "Example Layer"))
                })
            })

            addCanvas(Canvas("http://example.org/iiif/book1/canvas/p2", "p. 2").apply {
                this.width = 750
                this.height = 1000
                addImage(ImageContent("http://example.org/images/book1-page2/full/1500,2000/0/default.jpg").apply {
                    this.width = 1500
                    this.height = 2000
                    addService(ImageService("http://example.org/images/book1-page2", ImageApiProfile.LEVEL_ONE).apply {
                        this.width = 6000
                        this.height = 8000
                        addTile(TileInfo(512).apply {
                            addScaleFactor(1, 2, 4, 8, 16)
                        })
                    })
                })
                addOtherContent(AnnotationList("http://example.org/iiif/book1/list/p2").apply {
                    addWithin(Layer("http://example.org/iiif/book1/layer/l1"))
                })
            })

            addCanvas(Canvas("http://example.org/iiif/book1/canvas/p3", "p. 3").apply {
                this.width = 750
                this.height = 1000
                addImage(ImageContent("http://example.org/iiif/book1/res/page3.jpg").apply {
                    this.width = 1500
                    this.height = 2000
                    addService(
                        ImageService("http://example.org/images/book1-page3", ImageApiProfile.LEVEL_ONE)
                    )
                })
                addOtherContent(AnnotationList("http://example.org/iiif/book1/list/p3").apply {
                    addWithin(Layer("http://example.org/iiif/book1/layer/l1"))
                })
            })

        }

        manifest.addSequence(seq)

        manifest.addRange(
            Range("http://example.org/iiif/book1/range/r1", "Introduction").apply {
                addCanvas(
                    seq.canvases[0].identifier.toString(),
                    seq.canvases[1].identifier.toString(),
                    seq.canvases[2].identifier.toString() + "#xywh=0,0,750,300"
                )
            }
        )

        return WRITER.writeValueAsString(manifest)
    }

}