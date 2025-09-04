package nl.knaw.huc.di.elaborate.elabctl.apparatus

import java.io.File
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.writeText
import arrow.core.toNonEmptyListOrNull
import com.google.common.collect.TreeMultimap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.logging.log4j.kotlin.logger
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.XmlVersion
import org.redundent.kotlin.xml.xml
import nl.knaw.huc.di.elaborate.elabctl.archiver.AnnotationData
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver.json
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver.loadEntry
import nl.knaw.huc.di.elaborate.elabctl.archiver.EditionConfig

class ApparatusGenerator {

    val printOptions = PrintOptions(
        singleLineTextElements = true,
        indent = "  ",
        useSelfClosingTags = true
    )
    val projectTitle = "Correspondentie Bolland en Cosijn"

    @OptIn(ExperimentalSerializationApi::class)
    fun generate(warPath: String) {
        val personMetadataFields = listOf("Afzender", "Ontvanger")
        val personNames = mutableSetOf<String>()
        val annoNumForBioText = TreeMultimap.create<String, String>()
        val annoNumForBiblText = TreeMultimap.create<String, String>()
        ZipFile(warPath).use { zip ->
            val elabConfigEntry = zip.getEntry("data/config.json")
            val elabConfig: EditionConfig = zip.getInputStream(elabConfigEntry).use { input ->
                json.decodeFromStream(input)
            }
            elabConfig.entries.forEach { entryDescription ->
                val entry = loadEntry(zip, entryDescription)
                val persons =
                    entry.metadata
                        .filter { personMetadataFields.contains(it.field) }
                        .map { it.value }
                personNames.addAll(persons)
                val annotationData = entry.parallelTexts
                    .flatMap { it.value.annotationData }
                    .map { fixAnnotationTypeName(it) }
                annotationData
                    .filter { it.type.name == "Persoon" }
                    .map { it.n.toString() to it.normalizeAnnotationText() }
                    .forEach { p ->
                        annoNumForBioText[p.second].add(p.first)
                    }
                annotationData
                    .filter { it.type.name == "Publicatie" }
                    .map { it.n.toString() to it.normalizeAnnotationText() }
                    .forEach { p ->
                        annoNumForBiblText[p.second].add(p.first)
                    }

            }
        }
        exportAnnoNumToRefTarget(annoNumForBioText, annoNumForBiblText)
        exportBioXml(annoNumForBioText)
        exportBiblioXml(annoNumForBiblText)
    }

    private fun exportAnnoNumToRefTarget(
        annoNumForBioText: TreeMultimap<String, String>,
        annoNumForBiblText: TreeMultimap<String, String>
    ) {
        val annoNumToRefTarget = mutableMapOf<String, String>()
        annoNumForBioText.asMap().entries.forEachIndexed { i, e ->
            val num = "%03d".format(i + 1)
            val refTarget = "bio.xml#pers$num"
            e.value.forEach {
                annoNumToRefTarget[it] = refTarget
            }
        }
        annoNumForBiblText.asMap().entries.forEachIndexed { i, e ->
            val num = "%03d".format(i + 1)
            val refTarget = "biblio.xml#bg$num"
            e.value.forEach {
                annoNumToRefTarget[it] = refTarget
            }
        }
        val folder = "data"
        File(folder).mkdirs()
        val path = "$folder/correspondentie-bolland-en-cosijn-annonum-to-ref-target.json"
        logger.info { "=> $path" }
        Path(path).writeText(Json.encodeToString(annoNumToRefTarget))
    }

    private fun exportBiblioXml(annoNumForBiblText: TreeMultimap<String, String>) {
        println("Publications")
        annoNumForBiblText.keySet().sorted().forEach {
            println("-  $it [ in ${annoNumForBiblText[it].sorted()}]")
        }
        val tei = buildBiblioXml(annoNumForBiblText)
        val folder = "build/zip/elab4-correspondentie-bolland-en-cosijn/apparatus"
        File(folder).mkdirs()
        val path = "$folder/bibliolist.xml"
        logger.info { "=> $path" }
        Path(path).writeText(tei)
    }

    private fun buildBiblioXml(annoNumForBiblText: TreeMultimap<String, String>): String {
        val biblXmlNodes = annoNumForBiblText.keySet().mapIndexed { i, biblText ->
            val biblNum = "%03d".format(i + 1)
            xml("bibl") {
                attribute("xml:id", "bg$biblNum")
                comment(biblText)
                "label" {
                    -biblText.asBiblLabelContent()
                }
                unsafeText(biblText.asBiblText())
            }
        }

        return xml("TEI") {
            globalProcessingInstruction("editem", Pair("template", "bibliolist"))
            globalProcessingInstruction(
                "xml-model",
                Pair("href", "https://xmlschema.huygens.knaw.nl/editem-bibliolist.rng"),
                Pair("type", "application/xml"),
                Pair("schematypens", "http://relaxng.org/ns/structure/1.0"),
            )
            globalProcessingInstruction(
                "xml-model",
                Pair("href", "https://xmlschema.huygens.knaw.nl/editem-bibliolist.rng"),
                Pair("type", "application/xml"),
                Pair("schematypens", "http://purl.oclc.org/dsdl/schematron"),
            )
            version = XmlVersion.V10
            encoding = "UTF-8"
            xmlns = "http://www.tei-c.org/ns/1.0"
            "teiHeader" {
                "fileDesc" {
                    "titleStmt" {
                        "title" {
                            attribute("xml:lang", "nl")
                            -projectTitle
                        }
                    }
                }
            }

            "text" {
                "body" {
                    "head" {
                        -"$projectTitle: Bibliografie"
                    }
                    "listBibl" {
                        addElements(biblXmlNodes)
                    }
                }
            }

        }.toString(printOptions = printOptions)
    }

    private fun String.asBiblText(): String {
        val level = when {
            this.startsWith("<em>") -> "j"
            PAGE_RANGE_REGEX.containsMatchIn(this) -> "j"
            this.startsWith("Uit:") -> "a"
            this.split(" ").first().endsWith(",") -> "m"
            this.split(" ").first().endsWith(".") -> "m"
            else -> "?"
        }
        return this
            .replace("<em></em>", "")
            .replace("<sup>", "<hi rend=\"super\">")
            .replace("</sup>", "</hi>")
            .replace("<em>", """<title level="$level">""")
            .replace("</em>", "</title>")
    }

    private fun exportBioXml(annoNumForBioText: TreeMultimap<String, String>) {
        println("Persons")
        annoNumForBioText.keySet().sorted().forEach {
            println("-  $it [ in ${annoNumForBioText[it].sorted()}]")
        }
        val tei = buildBioXml(annoNumForBioText)
        val folder = "build/zip/elab4-correspondentie-bolland-en-cosijn/apparatus"
        File(folder).mkdirs()
        val path = "$folder/bio.xml"
        logger.info { "=> $path" }
        Path(path).writeText(tei)
    }

    private fun asPerson(bioText: String): ApparatusPerson {
        val parts = bioText.split(", ", limit = 2)
        val firstPart = parts[0]
        var forename = ""
        var surname = ""
        var birth: String? = null
        var death: String? = null
        var nameParts = mutableListOf("")
        if (firstPart.contains("(")) {
            val (namePart, years) = firstPart.split("(", ")")
            nameParts = namePart.trim().split(" ").toMutableList()
            val yearParts = years.split("-", "–")
            birth = yearParts[0]
            death = yearParts[1]
        } else {
            nameParts = firstPart.trim().split(" ").toMutableList()
        }

        surname = nameParts.removeLast()
        forename = nameParts
            .takeWhile { it[0].isUpperCase() }
            .joinToString(" ")
        val nameLink = nameParts
            .filter { it[0].isLowerCase() }
            .toNonEmptyListOrNull()
            ?.joinToString(" ")

        val source = if (SOURCE_REGEX.containsMatchIn(bioText)) {
            SOURCE_REGEX.find(bioText)?.groups[1]?.value
        } else {
            null
        }
        val note = if (parts.size == 2) {
            SOURCE_REGEX.replace(parts[1], "").trim('.').trim()
        } else {
            null
        }
        return ApparatusPerson(forename, nameLink, surname, source, birth, death, note)
    }

    private fun buildBioXml(annoNumForBioText: TreeMultimap<String, String>): String {
        val personXmlNodes = annoNumForBioText.keySet().mapIndexed { i, bioText ->
            val persNum = "%03d".format(i)
            val person = asPerson(bioText)
            xml("person") {
                attribute("xml:id", "pers$persNum")
                person.source?.let { source ->
                    attribute("source", source)
                }
                comment(bioText)
                "persName" {
                    attribute("full", "yes")
                    "foreName" { -person.forename }
                    text(" ")
                    person.nameLink?.let {
                        text(" ")
                        "nameLink" { -it }
                    }
                    "surname" { -person.surname }
                }
                person.birth?.let {
                    "birth" { attribute("when", it) }
                }
                person.death?.let {
                    "death" { attribute("when", it) }
                }
                person.note?.let {
                    "note" {
                        attribute("type", "shortDesc")
                        -it
                    }
                }
            }
        }
        return xml("TEI") {
            globalProcessingInstruction("editem", Pair("template", "biolist"))
            globalProcessingInstruction(
                "xml-model",
                Pair("href", "https://xmlschema.huygens.knaw.nl/editem-biolist.rng"),
                Pair("type", "application/xml"),
                Pair("schematypens", "http://relaxng.org/ns/structure/1.0"),
            )
            globalProcessingInstruction(
                "xml-model",
                Pair("href", "https://xmlschema.huygens.knaw.nl/editem-biolist.rng"),
                Pair("type", "application/xml"),
                Pair("schematypens", "http://purl.oclc.org/dsdl/schematron"),
            )
            version = XmlVersion.V10
            encoding = "UTF-8"
            xmlns = "http://www.tei-c.org/ns/1.0"
            "teiHeader" {
                "fileDesc" {
                    "titleStmt" {
                        "title" {
                            attribute("xml:lang", "nl")
                            -projectTitle
                        }
                    }
                    "publicationStmt" {
                        "publisher" {
                            "name" {
                                attribute("ref", "https://huygens.knaw.nl/en")
                                -"Huygens Institute for the History and Culture of the Netherlands (KNAW)"
                            }
                        }
                        "pubPlace" {}
                        "date" {
                            attribute("when", "2016-08-02")
                            -"2016-08-02"
                        }
                    }
                    "sourceDesc" {
                        "p" { -"N.A." }

                    }
                }
            }

            "text" {
                "body" {
                    "listPerson" {
                        addElements(personXmlNodes)
                    }
                }
            }

        }.toString(printOptions = printOptions)
    }

    private fun AnnotationData.normalizeAnnotationText(): String {
        val normalized = text.replace("<br/>", "")
            .replace("&nbsp;", " ")
            .replace("<em> ", " <em>")
            .replace(" </em>", "</em> ")
            .replace("<em></em>", "")
            .replace("<u></u>", "")
            .replace(".</em>", "</em>.")
            .replace("'</em>", "</em>'")
            .replace("'Zum <em>Beowulf</em>'.", "\"Zum <em>Beowulf</em>.\"")
            .replace("(1857-1913,", "(1857-1913),")
            .trim()
        return if (!normalized.endsWith(".")) {
            "$normalized."
        } else {
            normalized
        }
    }

    private fun fixAnnotationTypeName(data: AnnotationData): AnnotationData =
        if (data.text == "<em>De Gids.</em>"
            || data.text.startsWith("Brugmann, Karl. <em>")
            || data.text.startsWith("Paul, Hermann")
            || data.text.startsWith("Ellis, Alexander John.")
        ) {
            data.copy(type = data.type.copy(name = "Publication"))
        } else {
            data
        }

    data class ApparatusPerson(
        val forename: String,
        val nameLink: String?,
        val surname: String,
        val source: String?,
        val birth: String?,
        val death: String?,
        val note: String?
    )

    companion object {
        val TAG_REGEX = Regex("<[^>]+>")
        val PAGE_RANGE_REGEX = Regex("\\d+-\\d+")
        val SOURCE_REGEX = Regex("Zie (http.*)\\.$")

        private fun String.asBiblLabelContent(): String =
            TAG_REGEX.replace(this, "")
    }
}

