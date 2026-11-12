package mx.musicalesdoris.reproductorlocalandroid.library.artwork

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import mx.musicalesdoris.reproductorlocalandroid.Song

internal fun interface EmbeddedArtworkNormalizer {
    /** Returns a bounded JPEG or null when the embedded bytes do not contain a valid image. */
    fun normalize(encodedImage: ByteArray): ByteArray?
}

internal interface SongArtworkStore {
    fun save(normalizedJpeg: ByteArray): String
    fun fileFor(relativePath: String?): File?
    fun isReadable(relativePath: String?): Boolean
    fun deleteIfUnreferenced(relativePath: String?, songs: List<Song>)
}

internal fun interface ArtworkFileValidator {
    fun canDecode(file: File): Boolean
}

private object AndroidBitmapFileValidator : ArtworkFileValidator {
    override fun canDecode(file: File): Boolean {
        if (!file.isFile) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 256) sample *= 2
        val bitmap = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return false
        bitmap.recycle()
        return true
    }
}

internal class AndroidEmbeddedArtworkNormalizer : EmbeddedArtworkNormalizer {
    override fun normalize(encodedImage: ByteArray): ByteArray? {
        if (encodedImage.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(encodedImage, 0, encodedImage.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (maxOf(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > MAX_SIDE * 2) {
            if (sampleSize > (1 shl 28)) break
            sampleSize *= 2
        }
        val decoded = BitmapFactory.decodeByteArray(
            encodedImage,
            0,
            encodedImage.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: return null

        val maxSide = maxOf(decoded.width, decoded.height)
        val normalized = if (maxSide > MAX_SIDE) {
            val scale = MAX_SIDE.toFloat() / maxSide
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }

        return try {
            ByteArrayOutputStream().use { output ->
                if (!normalized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) null
                else output.toByteArray()
            }
        } finally {
            if (normalized !== decoded) normalized.recycle()
            decoded.recycle()
        }
    }

    private companion object {
        const val MAX_SIDE = 1200
        const val JPEG_QUALITY = 82
    }
}

internal object NoopEmbeddedArtworkNormalizer : EmbeddedArtworkNormalizer {
    override fun normalize(encodedImage: ByteArray): ByteArray? = null
}

internal class FileSongArtworkStore(
    private val artworkDirectory: File,
    private val fileValidator: ArtworkFileValidator = AndroidBitmapFileValidator
) : SongArtworkStore {
    override fun save(normalizedJpeg: ByteArray): String {
        require(normalizedJpeg.isNotEmpty()) { "La carátula está vacía" }
        val hash = sha256(normalizedJpeg)
        val relativePath = "AlbumArtwork/$hash.jpg"
        val target = fileFor(relativePath) ?: throw IOException("Ruta de carátula no válida")
        // A content-addressed path may already exist but contain a truncated or corrupt file
        // from an interrupted earlier write. Reuse only a decodable image; otherwise repair it.
        if (fileValidator.canDecode(target)) return relativePath
        if (!artworkDirectory.isDirectory && !artworkDirectory.mkdirs() && !artworkDirectory.isDirectory) {
            throw IOException("No se pudo crear el directorio de carátulas")
        }

        val temporary = File.createTempFile("$hash-", ".tmp", artworkDirectory)
        try {
            FileOutputStream(temporary).use { output ->
                output.write(normalizedJpeg)
                output.fd.sync()
            }
            try {
                Files.move(
                    temporary.toPath(), target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                // Do not replace a valid existing file if the filesystem cannot atomically swap.
                if (fileValidator.canDecode(target)) return relativePath
                throw IOException("No se pudo reparar la carátula de forma atómica")
            }
            return relativePath
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    override fun fileFor(relativePath: String?): File? {
        val prefix = "AlbumArtwork/"
        if (relativePath == null || !relativePath.startsWith(prefix)) return null
        val fileName = relativePath.removePrefix(prefix)
        if (!fileName.matches(Regex("[a-f0-9]{64}\\.jpg"))) return null
        return File(artworkDirectory, fileName)
    }

    override fun isReadable(relativePath: String?): Boolean = try {
        fileFor(relativePath)?.let(fileValidator::canDecode) ?: false
    } catch (_: Exception) {
        false
    }

    override fun deleteIfUnreferenced(relativePath: String?, songs: List<Song>) {
        if (relativePath == null || songs.any { it.artworkPath == relativePath }) return
        fileFor(relativePath)?.let { file ->
            if (file.exists() && !file.delete()) throw IOException("No se pudo eliminar la carátula ${file.name}")
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte) }
}
