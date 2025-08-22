package nl.knaw.huc.di.elaborate.elabctl.archiver

import org.junit.jupiter.api.Test
import nl.knaw.huc.di.elaborate.elabctl.manifests.ManifestV3Factory

class ManifestFactoryTest {

//    @Test
    fun `test reading facsimiles zip`() {
        val facsimileDimensionsList =
            FacsimileDimensionsFactory.readFacsimileDimensionsFromZipFilePath("facsimiles.zip")
                .take(10)
                .toList()
//        logger.info { "size=" + list.size }
        facsimileDimensionsList
            .groupBy { it.fileName.substringBeforeLast('-') }
            .forEach { (entryName, facsimileDimensions) ->
                val manifestJson = ManifestV3Factory(
                    "https://manifests.editem.huygens.knaw.nl/projectname",
                    "https://iiif.editem.huygens.knaw.nl/projectname"
                ).forEntry(entryName, facsimileDimensions)
                println(manifestJson)
            }
    }

}