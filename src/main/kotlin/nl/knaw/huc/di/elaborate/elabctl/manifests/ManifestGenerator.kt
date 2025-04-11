package nl.knaw.huc.di.elaborate.elabctl.manifests

import kotlin.io.path.Path
import kotlin.io.path.writeText
import nl.knaw.huc.di.elaborate.elabctl.archiver.FacsimileDimensionsFactory
import nl.knaw.huc.di.elaborate.elabctl.archiver.ManifestV3Factory
import nl.knaw.huc.di.elaborate.elabctl.logger

object ManifestGenerator {

    fun generateFrom(zipPath: String) {
        val manifestFactory = ManifestV3Factory(
            "https://manifests.editem.huygens.knaw.nl/projectname",
            "https://iiif.editem.huygens.knaw.nl/projectname"
        )
        logger.info { "<= $zipPath" }
        FacsimileDimensionsFactory.readFacsimileDimensionsFromZipFilePath(zipPath)
            .groupBy { it.fileName.substringBeforeLast('-') }
            .forEach { (entryName, facsimileDimensions) ->
                val manifestJson = manifestFactory.forEntry(entryName, facsimileDimensions)
                val outPath = "out/$entryName-manifest.json"
                logger.info { "=> $outPath" }
                Path(outPath).writeText(manifestJson.toString())
            }
    }

}