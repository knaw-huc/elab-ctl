package nl.knaw.huc.di.elaborate.elabctl

import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml

object TEIBuilder {

    val HI_TAGS: Map<String, String> = mapOf(
        "strong" to "bold",
        "b" to "bold",
        "u" to "underline",
        "em" to "italic",
        "i" to "italic",
        "sub" to "subscript",
        "sup" to "superscript"
    )

    fun Entry.toTEI(teiName: String): String {
        val printOptions = PrintOptions(
            singleLineTextElements = true,
            indent = "  ",
            useSelfClosingTags = true
        )
        return xml("TEI") {
            xmlns = "http:www.tei-c.org/ns/1.0"
            "teiHeader" {
                "fileDesc" {
                    "titelStmt" {
                        "title" {
                            -name
                        }
                    }
                }
            }
            if (facsimiles.isNotEmpty()) {
                "facsimile" {
                    facsimiles.forEachIndexed { i, facs ->
                        "graphic" {
                            attribute("url", "$teiName-${(i + 1).toString().padStart(2, '0')}.jp2")
                        }
                    }
                }
            }
            "text" {
                "interpGrp" {
                    metadata
                        .filter { it.value.isNotEmpty() }
                        .forEach {
                            "interp" {
                                attribute("type", it.field)
                                attribute("value", it.value)
                            }
                        }
                }
                "body" {
                    parallelTexts
                        .filter { it.value.text.isNotEmpty() }
                        .forEach { (layerName, textLayer) ->
                            "div" {
                                attribute("type", layerName)
                                unsafeText(textLayer.text.cleanup())
                            }
                        }
                }
            }
        }.toString(printOptions = printOptions)
    }

    private fun String.cleanup(): String =
        this.replace("<br>", "\n")
            .replace("&", "&amp;")
            .replace("<br>", "<br/>\n")
            .replace("<body>", "<div>")
            .replace("</body>", "</div>\n")
            .replace("\n", "<lb/>\n")
            .replace("<i></em></i>", "</em>")
            .replace("<em style=\"font-style: italic;\">", "<em>")
            .replace("<em></i></em>", "</i>")
            .replace("<em></em>", "")
            .replace("<i></i>", "")
            .replace(" style=\"font-size:.*?>", ">")
            .replace("\n", " ")
            .replace("<lb/>", "<lb/>\n")
}