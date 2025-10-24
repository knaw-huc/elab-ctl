package nl.knaw.huc.di.elaborate.elabctl

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver
import nl.knaw.huc.di.elaborate.elabctl.archiver.TEIBuilder.Companion.convertHorizontalSpace
import nl.knaw.huc.di.elaborate.elabctl.archiver.TEIBuilder.Companion.horizontalSpaceTag

class ArchiverTest {

    @Test
    fun `test bolland-cosijn`() {
//        Archiver.archive(listOf("./data/elab4-correspondentie-bolland-en-cosijn.war"))
    }

    @Test
    fun `converting nbsp to horizontal space`() {
        val oneNbsp = "one<nbsp/>space"
        val oneSpace = "one" + horizontalSpaceTag(1) + "space"
        assertEquals(oneSpace, oneNbsp.convertHorizontalSpace())

        val twoNbsp = "two<nbsp/><nbsp/>spaces"
        val twoSpace = "two" + horizontalSpaceTag(2) + "spaces"
        assertEquals(twoSpace, twoNbsp.convertHorizontalSpace())

        val threeNbsp = "three&nbsp;&nbsp;&nbsp;spaces"
        val threeSpace = "three" + horizontalSpaceTag(3) + "spaces"
        assertEquals(threeSpace, threeNbsp.convertHorizontalSpace())
    }

}