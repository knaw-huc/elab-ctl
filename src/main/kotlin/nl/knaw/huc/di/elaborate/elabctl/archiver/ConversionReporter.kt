package nl.knaw.huc.di.elaborate.elabctl.archiver

import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter
import org.apache.commons.csv.CSVFormat
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.di.elaborate.elabctl.config.ElabCtlConfig

class ConversionReporter(val projectName: String, val conversionConfig: ElabCtlConfig) {
    val rows: MutableList<Map<String, String>> = mutableListOf()
    val fields: MutableSet<String> = mutableSetOf()
    val editionEntryBaseUrl = "https://$projectName.huygens.knaw.nl/edition/entry"
    val letterTeiBaseUrl = "https://gitlab.huc.knaw.nl/elaborate/$projectName/-/blob/main/tei/letters"
    val dateAttributeFactory = DateAttributeFactory(conversionConfig.letterDates)

    fun addEntry(entry: Entry, teiName: String) {
        val row = mutableMapOf<String, String>()
        entry.metadata.forEach {
            row[it.field] = it.value
            fields.add(it.field)
        }

        val field = "Entry"
        row[field] = "$editionEntryBaseUrl/${entry.id}"
        fields.add(field)

        val field1 = "TEI"
        row[field1] = "$letterTeiBaseUrl/${teiName}.xml"
        fields.add(field1)

        val date = row[conversionConfig.letterMetadata.date] ?: ""
        dateAttributeFactory.getDateAttributes(date).forEach { (field, value) ->
            row[field] = value
            fields.add(field)
        }

        val language = row[conversionConfig.letterMetadata.language] ?: ""
        val langField = "lang"
        row[langField] = language.asIsoLang()
        fields.add(langField)

        rows.add(row)
    }

    fun storeAsCsv() {
        val path = "out/${projectName}-report.csv"
        logger.info { "=> $path" }
        val exportedFields = mutableListOf<String>()
        val labels = mutableListOf<String>()
        exportedFields.add("Entry")
        labels.add("Entry")

        exportedFields.add("TEI")
        labels.add("TEI")

        exportedFields.add(conversionConfig.letterMetadata.sender)
        labels.add("sentPerson (${conversionConfig.letterMetadata.sender})")

        exportedFields.add(conversionConfig.letterMetadata.senderPlace)
        labels.add("sentPlaceName (${conversionConfig.letterMetadata.senderPlace})")

        exportedFields.add(conversionConfig.letterMetadata.date)
        labels.add("sentDate (${conversionConfig.letterMetadata.date})")

        exportedFields.add("when")
        labels.add("when")

        exportedFields.add("notBefore")
        labels.add("notBefore")

        exportedFields.add("notAfter")
        labels.add("notAfter")

        exportedFields.add(conversionConfig.letterMetadata.recipient)
        labels.add("receivedPerson (${conversionConfig.letterMetadata.recipient})")

        conversionConfig.letterMetadata.recipientPlace?.let {
            exportedFields.add(it)
            labels.add("receivedPlaceName (${conversionConfig.letterMetadata.recipientPlace})")
        }

        exportedFields.add(conversionConfig.letterMetadata.language)
        labels.add("language (${conversionConfig.letterMetadata.language})")

        exportedFields.add("lang")
        labels.add("lang")

        conversionConfig.letterMetadata.idno?.let {
            exportedFields.add(it)
            labels.add("idno ($it)")
        }

        conversionConfig.letterMetadata.settlement?.let {
            exportedFields.add(it)
            labels.add("settlement ($it)")
        }

        conversionConfig.letterMetadata.institution?.let {
            exportedFields.add(it)
            labels.add("institution ($it)")
        }

        conversionConfig.letterMetadata.collection?.let {
            exportedFields.add(it)
            labels.add("collection ($it)")
        }

        val remainingFields = fields - exportedFields.toSet()
        exportedFields.addAll(remainingFields)
        labels.addAll(remainingFields)

        Path(path).bufferedWriter().use { writer ->
            CSVFormat.DEFAULT.print(writer).apply {
                printRecord(labels)
                rows.forEach { row -> printRecord(exportedFields.map { field -> row[field] }) }
            }
        }
    }

}