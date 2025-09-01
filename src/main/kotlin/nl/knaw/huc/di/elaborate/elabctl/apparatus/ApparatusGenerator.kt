package nl.knaw.huc.di.elaborate.elabctl.apparatus

import java.util.zip.ZipFile
import com.google.common.collect.TreeMultimap
import kotlinx.serialization.json.decodeFromStream
import nl.knaw.huc.di.elaborate.elabctl.archiver.AnnotationData
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver.json
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver.loadEntry
import nl.knaw.huc.di.elaborate.elabctl.archiver.EditionConfig

class ApparatusGenerator {
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
            elabConfig.entries.forEach {
                val entry = loadEntry(zip, it)
                val persons =
                    entry.metadata
                        .filter { personMetadataFields.contains(it.field) }
                        .map { it.value }
                personNames.addAll(persons)
                entry.parallelTexts
                    .flatMap { it.value.annotationData }
                    .map { fixAnnotationTypeName(it) }
                    .filter { it.type.name == "Persoon" }
                    .map { it.n.toString() to it.normalizeAnnotationText() }
                    .forEach { p ->
                        annoNumForBioText[p.second].add(p.first)
                    }
                entry.parallelTexts
                    .flatMap { it.value.annotationData }
                    .map { fixAnnotationTypeName(it) }
                    .filter { it.type.name == "Publicatie" }
                    .map { it.n.toString() to it.normalizeAnnotationText() }
                    .forEach { p ->
                        annoNumForBiblText[p.second].add(p.first)
                    }

                entry.facsimiles.forEach {
                }

            }
        }
        println("Persons")
        annoNumForBioText.keySet().sorted().forEach {
            println("-  $it [ in ${annoNumForBioText[it].sorted()}]")
        }
        println("Publications")
        annoNumForBiblText.keySet().sorted().forEach {
            println("-  $it [ in ${annoNumForBiblText[it].sorted()}]")
        }
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
        if (data.text == "<em>De Gids.</em>") {
            data.copy(type = data.type.copy(name = "Publication"))
        } else {
            data
        }
}

