package nl.knaw.huc.di.elaborate.elabctl.apparatus

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import nl.knaw.huc.di.elaborate.elabctl.apparatus.ApparatusGenerator.Companion.TAG_REGEX

class ApparatusGeneratorTest {
    @Test
    fun `generate bolland-cosijn apparatus`() {
        ApparatusGenerator().generate("./data/elab4-correspondentie-bolland-en-cosijn.war")
    }

    @Test
    fun `test TAG_REGEX`() {
        val inString = "bla <tag> bla </tag> bla"
        val expectation = "bla  bla  bla"
        assertEquals(expectation, TAG_REGEX.replace(inString, ""))
    }
}