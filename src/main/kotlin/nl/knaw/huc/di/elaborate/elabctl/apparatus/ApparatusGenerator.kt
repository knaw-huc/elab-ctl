package nl.knaw.huc.di.elaborate.elabctl.apparatus

import java.util.zip.ZipFile
import kotlinx.serialization.json.decodeFromStream
import nl.knaw.huc.di.elaborate.elabctl.archiver.AnnotationData
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver.json
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver.loadEntry
import nl.knaw.huc.di.elaborate.elabctl.archiver.EditionConfig

class ApparatusGenerator {
    fun generate(warPath: String) {
        val personMetadataFields = listOf("Afzender", "Ontvanger")
        val personNames = mutableSetOf<String>()
        val bibl = mutableSetOf<String>()
        ZipFile(warPath).use { zip ->
            val elabConfigEntry = zip.getEntry("data/config.json")
            val elabConfig: EditionConfig = zip.getInputStream(elabConfigEntry).use { input ->
                json.decodeFromStream(input)
            }
            val entries = elabConfig.entries
            entries.forEach {
                val entry = loadEntry(zip, it)
                val persons =
                    entry.metadata
                        .filter { personMetadataFields.contains(it.field) }
                        .map { it.value }
                personNames.addAll(persons)
                val annotatedPersons = entry.parallelTexts
                    .flatMap { it.value.annotationData }
                    .filter { it.type.name == "Persoon" }
                    .map { it.normalizeAnnotationText() }
                personNames.addAll(annotatedPersons)
                val annotatedBibl = entry.parallelTexts
                    .flatMap { it.value.annotationData }
                    .filter { it.type.name == "Publicatie" }
                    .map { it.normalizeAnnotationText() }
                bibl.addAll(annotatedBibl)
                entry.facsimiles.forEach {
                }

            }
        }
        println("Persons")
        personNames.sorted().forEach {
            println("-  $it")
        }
        println("Publications")
        bibl.sorted().forEach {
            println("-  $it")
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
            .trim()
        return if (!normalized.endsWith(".")) {
            "$normalized."
        } else {
            normalized
        }
    }

}