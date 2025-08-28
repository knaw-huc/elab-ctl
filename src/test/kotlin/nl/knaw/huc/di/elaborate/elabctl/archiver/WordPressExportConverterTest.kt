package nl.knaw.huc.di.elaborate.elabctl.archiver

import org.junit.jupiter.api.Test
import nl.knaw.huc.di.elaborate.elabctl.config.EditorConfig
import nl.knaw.huc.di.elaborate.elabctl.config.ElabCtlConfig

class WordPressExportConverterTest {

    @Test
    fun `test WordPressExportConverter with correspondentie-bolland-en-cosijn`() {
        testWPExportForProject("correspondentie-bolland-en-cosijn")
    }

    @Test
    fun `test WordPressExportConverter with brieven-correspondenten-1900`() {
        testWPExportForProject("brieven-correspondenten-1900")
    }

    private fun testWPExportForProject(projectName: String) {
        val wpePath = "data/${projectName.replace("elab4-", "")}-wpe.xml"
        val outputDir = "out"
        val conversionConfig = ElabCtlConfig(projectName, EditorConfig("id", "name", "url"), "original")
        val errors = WordPressExportConverter(outputDir, conversionConfig).convert(wpePath)
        assert(errors.isEmpty())
    }

}