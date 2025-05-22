package nl.knaw.huc.di.elaborate.elabctl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import nl.knaw.huc.di.elaborate.elabctl.archiver.fixXhtml

class XMLToolsKtTest {

    @Test
    fun fixXhtml() {
        val badXml = "line1<br>line2"
        val fixedXml = badXml.fixXhtml()
        val expected = "line1<br/>line2"
        assertEquals(expected, fixedXml)
    }
}