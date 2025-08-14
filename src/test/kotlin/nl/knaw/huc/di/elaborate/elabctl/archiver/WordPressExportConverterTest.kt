package nl.knaw.huc.di.elaborate.elabctl.archiver

import org.junit.jupiter.api.Test

class WordPressExportConverterTest {

    @Test
    fun `test WordPressExportConverter`() {
        val projectName = "correspondentie-bolland-en-cosijn"
        val wpePath = "data/${projectName.replace("elab4-", "")}-wpe.xml"
        val outputDir = "/tmp"
        val errors = WordPressExportConverter(outputDir).convert(wpePath)
        assert(errors.isEmpty())
    }

}