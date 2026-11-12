package mx.musicalesdoris.reproductorlocalandroid

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface SongCatalogRepository {
    fun loadSongs(): List<Song>

    /** Reads, transforms, and persists the latest catalog under one repository lock. */
    fun updateSongs(transform: (List<Song>) -> List<Song>): List<Song>

    /** Preserves an unreadable catalog as a verified backup before starting an empty catalog. */
    fun recoverUnreadableCatalog(): File
}

class CatalogReadException(message: String, cause: Throwable) : IOException(message, cause)

class AtomicCatalogReplacementUnavailableException(cause: Throwable) :
    IOException("El sistema de archivos no permite reemplazar el catálogo de forma atómica", cause)

internal fun interface CatalogAtomicMover {
    fun move(temporaryFile: File, catalogFile: File)
}

internal fun interface CatalogBackupCopier {
    fun copyAndSync(source: File, destination: File)
}

/**
 * JSON persistence for the local catalog. Updates are serialized per canonical catalog path.
 * Writes use a unique temporary file in the same directory and replace the catalog only through
 * ATOMIC_MOVE. If the filesystem cannot provide that operation, the update is rejected and the
 * previous catalog remains the active file. A successful rename is the commit point; the temp
 * file is flushed before it, but this does not promise survival of sudden hardware power loss.
 */
class JsonSongCatalogRepository private constructor(
    private val songsDirectory: File,
    private val catalogFile: File,
    private val atomicMover: CatalogAtomicMover,
    private val backupCopier: CatalogBackupCopier
) : SongCatalogRepository {
    constructor(songsDirectory: File, catalogFile: File) : this(
        songsDirectory,
        catalogFile,
        CatalogAtomicMover { temporary, target ->
            try {
                Files.move(
                    temporary.toPath(), target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
                )
            } catch (exception: AtomicMoveNotSupportedException) {
                throw AtomicCatalogReplacementUnavailableException(exception)
            }
        },
        CatalogBackupCopier { source, destination -> copyAndSyncFile(source, destination) }
    )

    private val lock = CatalogLocks.forFile(catalogFile)

    override fun loadSongs(): List<Song> = lock.withLock { loadSongsLocked() }

    override fun updateSongs(transform: (List<Song>) -> List<Song>): List<Song> = lock.withLock {
        val current = loadSongsLocked()
        val updated = transform(current)
        writeSongsLocked(updated)
        updated
    }

    override fun recoverUnreadableCatalog(): File = lock.withLock {
        if (!catalogFile.isFile) throw IllegalStateException("No hay un catálogo existente que recuperar")
        try {
            loadSongsLocked()
            throw IllegalStateException("El catálogo se puede leer; no requiere recuperación")
        } catch (exception: CatalogReadException) {
            // Only a known read/parse failure permits this explicit recovery path.
        }

        val parent = catalogFile.parentFile ?: throw IOException("El catálogo no tiene directorio padre")
        if (!parent.isDirectory && !parent.mkdirs() && !parent.isDirectory) {
            throw IOException("No se pudo preparar el directorio de recuperación")
        }
        val temporaryBackup = File.createTempFile("${catalogFile.name}.corrupt-", ".tmp", parent)
        val backup = File(parent, temporaryBackup.name.removeSuffix(".tmp") + ".bak")
        try {
            backupCopier.copyAndSync(catalogFile, temporaryBackup)
            if (!sameBytes(catalogFile, temporaryBackup)) {
                throw IOException("La copia de recuperación no coincide con el catálogo original")
            }
            Files.move(temporaryBackup.toPath(), backup.toPath(), StandardCopyOption.ATOMIC_MOVE)
            writeSongsLocked(emptyList())
            backup
        } catch (exception: Exception) {
            // Keep the original and any verified backup. Remove incomplete backup temporaries.
            throw exception
        } finally {
            if (temporaryBackup.exists()) temporaryBackup.delete()
        }
    }

    private fun loadSongsLocked(): List<Song> {
        if (!catalogFile.exists()) return emptyList()
        try {
            val array = JSONArray(catalogFile.readText())
            return buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val song = Song(
                        id = item.getString("id"),
                        fileName = item.getString("fileName"),
                        title = item.getString("title"),
                        favorite = item.optBoolean("favorite", false),
                        duration = item.optionalDouble("duration"),
                        artist = item.optionalString("artist"),
                        albumArtist = item.optionalString("albumArtist"),
                        album = item.optionalString("album"),
                        trackNumber = item.optionalInt("trackNumber"),
                        artworkPath = item.optionalString("artworkPath"),
                        genre = item.optionalString("genre"),
                        metadataFingerprint = item.optionalString("metadataFingerprint"),
                        metadataRetryAfterMillis = item.optionalLong("metadataRetryAfterMillis"),
                        metadataRetryCount = item.optionalInt("metadataRetryCount") ?: 0
                    )
                    // Keep the JSON record even if the audio is temporarily unavailable.
                    add(song)
                }
            }
        } catch (exception: Exception) {
            throw CatalogReadException("No se pudo leer el catálogo JSON", exception)
        }
    }

    private fun writeSongsLocked(songs: List<Song>) {
        val parent = catalogFile.parentFile ?: throw IOException("El catálogo no tiene directorio padre")
        if (!parent.isDirectory && !parent.mkdirs() && !parent.isDirectory) {
            throw IOException("No se pudo crear el directorio del catálogo")
        }

        val array = JSONArray()
        songs.forEach { song ->
            val item = JSONObject()
                .put("id", song.id)
                .put("fileName", song.fileName)
                .put("title", song.title)
                .put("favorite", song.favorite)
            song.duration?.let { item.put("duration", it) }
            song.artist?.let { item.put("artist", it) }
            song.albumArtist?.let { item.put("albumArtist", it) }
            song.album?.let { item.put("album", it) }
            song.trackNumber?.let { item.put("trackNumber", it) }
            song.artworkPath?.let { item.put("artworkPath", it) }
            song.genre?.let { item.put("genre", it) }
            song.metadataFingerprint?.let { item.put("metadataFingerprint", it) }
            song.metadataRetryAfterMillis?.let { item.put("metadataRetryAfterMillis", it) }
            if (song.metadataRetryCount > 0) item.put("metadataRetryCount", song.metadataRetryCount)
            array.put(item)
        }

        val temporaryFile = File.createTempFile("${catalogFile.name}.", ".tmp", parent)
        try {
            FileOutputStream(temporaryFile).use { output ->
                output.write(array.toString().toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            try {
                atomicMover.move(temporaryFile, catalogFile)
            } catch (exception: AtomicCatalogReplacementUnavailableException) {
                throw exception
            } catch (exception: AtomicMoveNotSupportedException) {
                throw AtomicCatalogReplacementUnavailableException(exception)
            }
        } finally {
            if (temporaryFile.exists()) temporaryFile.delete()
        }
    }

    private fun sameBytes(left: File, right: File): Boolean =
        left.length() == right.length() && MessageDigest.isEqual(sha256(left), sha256(right))

    private fun sha256(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest()
    }

    private fun JSONObject.optionalString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeUnless { it == "null" }

    private fun JSONObject.optionalDouble(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key).takeIf { it.isFinite() }

    private fun JSONObject.optionalInt(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key)

    private fun JSONObject.optionalLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    companion object {
        internal fun withAtomicMover(
            songsDirectory: File,
            catalogFile: File,
            atomicMover: CatalogAtomicMover
        ): JsonSongCatalogRepository = JsonSongCatalogRepository(
            songsDirectory,
            catalogFile,
            atomicMover,
            CatalogBackupCopier { source, destination -> copyAndSyncFile(source, destination) }
        )

        internal fun withBackupCopier(
            songsDirectory: File,
            catalogFile: File,
            backupCopier: CatalogBackupCopier,
            atomicMover: CatalogAtomicMover = CatalogAtomicMover { temporary, target ->
                try {
                    Files.move(
                        temporary.toPath(), target.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (exception: AtomicMoveNotSupportedException) {
                    throw AtomicCatalogReplacementUnavailableException(exception)
                }
            }
        ): JsonSongCatalogRepository = JsonSongCatalogRepository(
            songsDirectory, catalogFile, atomicMover, backupCopier
        )
    }
}

private fun copyAndSyncFile(source: File, destination: File) {
    FileInputStream(source).use { input ->
        FileOutputStream(destination, false).use { output ->
            input.copyTo(output)
            output.fd.sync()
        }
    }
}

private object CatalogLocks {
    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    fun forFile(file: File): ReentrantLock {
        val key = file.canonicalPath.lowercase(Locale.ROOT)
        return locks.computeIfAbsent(key) { ReentrantLock() }
    }
}
