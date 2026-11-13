package mx.musicalesdoris.reproductorlocalandroid

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Stores normalized playlist artwork under an app-private, content-addressed relative path. */
class PlaylistArtworkStore(private val filesDirectory: File) {
    private val artworkDirectory = File(filesDirectory, DIRECTORY)

    @Throws(IOException::class)
    fun save(input: ByteArray): String {
        if (input.isEmpty() || input.size > MAX_INPUT_BYTES) throw IOException("La imagen está vacía o supera el límite permitido")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(input, 0, input.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("La imagen seleccionada no se puede decodificar")
        val options = BitmapFactory.Options().apply {
            var sample = 1
            while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_EDGE * 2) sample *= 2
            inSampleSize = sample
        }
        val decoded = BitmapFactory.decodeByteArray(input, 0, input.size, options)
            ?: throw IOException("La imagen seleccionada no se puede decodificar")
        val normalized = try {
            val scale = minOf(1f, MAX_EDGE.toFloat() / maxOf(decoded.width, decoded.height).coerceAtLeast(1))
            val bitmap = if (scale < 1f) Bitmap.createScaledBitmap(
                decoded, (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1), true
            ) else decoded
            try {
                ByteArrayOutputStream().use { output ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                        throw IOException("No se pudo preparar la portada")
                    }
                    output.toByteArray()
                }
            } finally {
                if (bitmap !== decoded) bitmap.recycle()
            }
        } finally {
            decoded.recycle()
        }
        val name = sha256(normalized) + ".jpg"
        if (!artworkDirectory.isDirectory && !artworkDirectory.mkdirs() && !artworkDirectory.isDirectory) {
            throw IOException("No se pudo crear el almacenamiento de portadas")
        }
        val destination = File(artworkDirectory, name)
        val expectedHash = name.removeSuffix(".jpg")
        if (!destination.isFile || !isDecodable(destination) ||
            runCatching { sha256(destination.readBytes()) }.getOrNull() != expectedHash) {
            val temporary = File.createTempFile("playlist-cover-", ".tmp", artworkDirectory)
            try {
                FileOutputStream(temporary).use { stream -> stream.write(normalized); stream.fd.sync() }
                try {
                    Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } catch (exception: java.nio.file.AtomicMoveNotSupportedException) {
                    throw IOException("El almacenamiento no permite guardar la portada de forma atómica", exception)
                }
            } finally {
                temporary.delete()
            }
        }
        return "$DIRECTORY/$name"
    }

    fun fileFor(relativePath: String?): File? {
        val file = pathFor(filesDirectory, relativePath) ?: return null
        return file.takeIf(File::isFile)
    }

    fun deleteUnreferenced(referencedPaths: Set<String>) {
        artworkDirectory.listFiles()?.forEach { file ->
            val relative = "$DIRECTORY/${file.name}"
            if (file.isFile && file.name.matches(Regex("[a-f0-9]{64}\\.jpg")) && relative !in referencedPaths) {
                file.delete()
            }
        }
    }

    private fun isDecodable(file: File): Boolean = runCatching {
        BitmapFactory.Options().also { it.inJustDecodeBounds = true }
            .let { options -> BitmapFactory.decodeFile(file.absolutePath, options); options.outWidth > 0 && options.outHeight > 0 }
    }.getOrDefault(false)

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        /** Validates only the app-relative path; file access and image decoding belong on IO. */
        fun pathFor(filesDirectory: File, relativePath: String?): File? {
            if (relativePath.isNullOrBlank()) return null
            val prefix = "$DIRECTORY/"
            if (!relativePath.startsWith(prefix)) return null
            val leaf = relativePath.removePrefix(prefix)
            if (!leaf.matches(Regex("[a-f0-9]{64}\\.jpg"))) return null
            return File(File(filesDirectory, DIRECTORY), leaf)
        }

        private const val DIRECTORY = "playlist-artwork"
        private const val MAX_EDGE = 1200
        private const val MAX_INPUT_BYTES = 20 * 1024 * 1024
        private const val JPEG_QUALITY = 84
    }
}
