package nl.knaw.huc.di.elaborate.elabctl.archiver

import org.junit.jupiter.api.Test
import nl.knaw.huc.di.elaborate.elabctl.archiver.TEIBuilder.Companion.wrapLines

class TEIBuilderTest {

    @Test
    fun `test wrapLines`() {
        val s =
            "this is a very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, very, long text"
        val wrapped = s.wrapLines(80)
        println(wrapped)
        val lines = wrapped.split("\n")
        println(lines.map { it.length })
        assert(lines.all { it.length <= 80 })
    }

}