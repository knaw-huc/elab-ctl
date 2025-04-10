package nl.knaw.huc.di.elaborate.elabctl.archiver

import java.awt.Dimension
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.FileCacheImageInputStream
import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.raise.either
import nl.knaw.huc.di.elaborate.elabctl.archiver.ManifestV3Factory.FacsimileDimensions

object FacsimileDimensionsFactory {
    fun readFacsimileDimensionsFromZipFilePath(zipFilePath: String): Sequence<FacsimileDimensions> =
        sequence {
            ZipFile(zipFilePath).use { zipFile ->
                zipFile
                    .entries()
                    .toList()
                    .filterNot { it.isDirectory }
                    .filter { it.name.startsWith("facsimiles/") }
                    .forEach { entry ->
                        zipFile.getInputStream(entry).use { inputStream ->
                            either {
                                val dimension = getImageDimension(inputStream, entry.name).bind()
                                FacsimileDimensions(
                                    fileName = entry.name.split("/").last(),
                                    width = dimension.width,
                                    height = dimension.height
                                )
                            }
                        }.fold(
                            { error -> throw error },
                            { facsimileDimensions -> yield(facsimileDimensions) }
                        )
                    }
            }
        }

    private fun getImageDimension(inputStream: InputStream, fileName: String): Either<Exception, Dimension> {
        val suffix = fileName.split(".").last()
        val iterator = ImageIO.getImageReadersBySuffix(suffix)
        while (iterator.hasNext()) {
            val reader: ImageReader = iterator.next()
            try {
                val dimension = FileCacheImageInputStream(inputStream, null).use { stream ->
                    reader.setInput(stream)
                    val width: Int = reader.getWidth(reader.getMinIndex())
                    val height: Int = reader.getHeight(reader.getMinIndex())
                    Dimension(width, height)
                }
                return Right(dimension)
            } catch (e: Exception) {
                return Left(e)
            } finally {
                reader.dispose()
            }
        }

        return Left(IOException("Not a known image file: $fileName"))
    }

}