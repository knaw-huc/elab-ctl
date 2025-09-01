package nl.knaw.huc.di.elaborate.elabctl.apparatus

import org.junit.jupiter.api.Test

class ApparatusGeneratorTest {
    @Test
    fun `generate bolland-cosijn apparatus`() {
        ApparatusGenerator().generate("./data/elab4-correspondentie-bolland-en-cosijn.war")
    }
}