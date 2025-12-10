package nl.knaw.huc.di.elaborate.elabctl

import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream
import kotlin.io.path.walk
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import nl.knaw.huc.di.elaborate.elabctl.archiver.Archiver.json
import nl.knaw.huc.di.elaborate.elabctl.archiver.EditionConfig
import nl.knaw.huc.di.elaborate.elabctl.archiver.Entry
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

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `ordering anton-de-kom-sections`() {
        val json = Json { ignoreUnknownKeys = true }
        val path = ".local/adk/data/"
        val order = mutableListOf<String>()
        Path(path)
            .walk()
            .map { it.absolutePathString() }
            .filter { it.contains('3') }
            .sorted()
            .map { json.decodeFromStream<Entry>(Path(it).inputStream()) }
            .map {
                val firstFacsName = it.facsimiles.firstOrNull()?.title?.replaceFirst("p", "") ?: ""
                "$firstFacsName | ${it.id} | ${it.name}"
            }
            .sorted()
            .forEach {
                println(it)
                order.add(it.split("|")[1].trim())
            }
        val x = json.encodeToString(order)
        Path("adk-order.json").writeText(x)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `reorder entries for config json`() {
        val customOrder = json.decodeFromStream<List<String>>(File("data/adk-order.json").inputStream())

        val json = Json { ignoreUnknownKeys = true }
        val path = ".local/adk/data/config.json"
        val ist = Path(path).inputStream()
        val elabConfig: EditionConfig =
            json.decodeFromStream(ist)
        elabConfig.entries
        val newOrder = customOrder.map { filename -> elabConfig.entries.first { it.datafile == filename } }
        val x = json.encodeToString(newOrder)
        println(x)
    }

}