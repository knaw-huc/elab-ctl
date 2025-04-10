package nl.knaw.huc.di.elaborate.elabctl.archiver

import org.junit.jupiter.api.Test

class ManifestFactoryTest {

    @Test
    fun `test ManifestV2Factory`() {
        val json = ManifestV2Factory.createFromImages(listOf())
        println(json)
    }

    @Test
    fun `test ManifestFactory manifest`() {
        val json = ManifestV2Factory.manifest()
        println(json)
    }

    @Test
    fun `test reading facsimiles zip`() {
        val facsimileDimensionsList =
            FacsimileDimensionsFactory.readFacsimileDimensionsFromZipFilePath("facsimiles.zip")
                .take(100)
                .toList()
//        logger.info { "size=" + list.size }
        facsimileDimensionsList
            .groupBy { it.fileName.substringBeforeLast('-') }
            .forEach { (entryName, facsimileDimensions) ->
                val manifestJson = ManifestV3Factory.forEntry(entryName, facsimileDimensions.sortedBy { it.fileName })
                println(manifestJson)
            }
    }

}