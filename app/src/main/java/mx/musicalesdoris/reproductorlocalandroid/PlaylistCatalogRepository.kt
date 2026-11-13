package mx.musicalesdoris.reproductorlocalandroid

import mx.musicalesdoris.reproductorlocalandroid.library.Playlist
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface PlaylistCatalogRepository {
    fun load(): List<Playlist>
    fun recoverUnreadableCatalog(): File
    fun create(title: String, description: String? = null, artworkBytes: ByteArray? = null): Playlist
    fun rename(id: String, title: String): Playlist
    fun updateDetails(id: String, title: String, description: String?, artworkBytes: ByteArray?, removeArtwork: Boolean): Playlist
    fun delete(id: String): Boolean
    fun addSongs(id: String, songIds: List<String>): Playlist
    fun removeSong(id: String, songId: String): Playlist
}

class PlaylistCatalogReadException(message: String, cause: Throwable) : IOException(message, cause)

class AtomicPlaylistReplacementUnavailableException(cause: Throwable) :
    IOException("No se pudo guardar el catálogo de listas de forma atómica", cause)

internal fun interface PlaylistCatalogAtomicMover {
    fun move(temporaryFile: File, catalogFile: File)
}

/** Separate JSON catalog keeps the existing songs.json format untouched. */
class JsonPlaylistCatalogRepository internal constructor(
    private val catalogFile: File,
    private val atomicMover: PlaylistCatalogAtomicMover,
    private val clockMillis: () -> Long,
    private val artworkStore: PlaylistArtworkStore
) : PlaylistCatalogRepository {
    constructor(catalogFile: File) : this(
        catalogFile,
        PlaylistCatalogAtomicMover { temporary, target ->
            try {
                Files.move(
                    temporary.toPath(), target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
                )
            } catch (exception: AtomicMoveNotSupportedException) {
                throw AtomicPlaylistReplacementUnavailableException(exception)
            }
        },
        System::currentTimeMillis,
        PlaylistArtworkStore(catalogFile.parentFile ?: File("."))
    )

    private val lock = locks.computeIfAbsent(catalogFile.canonicalPath.lowercase()) { ReentrantLock() }

    override fun load(): List<Playlist> = lock.withLock { loadLocked() }

    override fun recoverUnreadableCatalog(): File = lock.withLock {
        try {
            loadLocked()
            throw IllegalStateException("El catálogo de listas se puede leer; no requiere recuperación")
        } catch (exception: PlaylistCatalogReadException) {
            // The only permitted recovery path starts after a confirmed read/parse failure.
        }
        if (!catalogFile.isFile) throw IOException("No hay un catálogo que necesite recuperación")
        val parent = catalogFile.parentFile ?: throw IOException("El catálogo no tiene directorio padre")
        val backup = File(parent, "${catalogFile.name}.corrupt-${UUID.randomUUID()}.bak")
        val temporaryBackup = File.createTempFile("playlist-backup-", ".tmp", parent)
        try {
            FileOutputStream(temporaryBackup).use { output ->
                catalogFile.inputStream().use { it.copyTo(output) }
                output.fd.sync()
            }
            if (!temporaryBackup.readBytes().contentEquals(catalogFile.readBytes())) {
                throw IOException("No se pudo verificar el respaldo del catálogo")
            }
            try {
                Files.move(temporaryBackup.toPath(), backup.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } catch (exception: java.nio.file.AtomicMoveNotSupportedException) {
                throw AtomicPlaylistReplacementUnavailableException(exception)
            }
            writeLocked(emptyList())
            backup
        } finally {
            temporaryBackup.delete()
        }
    }

    override fun create(title: String, description: String?, artworkBytes: ByteArray?): Playlist = withArtworkCleanup {
        update { current ->
            val now = clockMillis()
            val artworkPath = artworkBytes?.let(artworkStore::save)
            val playlist = Playlist(UUID.randomUUID().toString(), normalizeTitle(title),
                createdAtMillis = now, updatedAtMillis = now,
                description = normalizeDescription(description), artworkPath = artworkPath)
            (current + playlist) to playlist
        }
    }

    override fun rename(id: String, title: String): Playlist = update { current ->
        val normalized = normalizeTitle(title)
        val existing = current.firstOrNull { it.id == id }
            ?: throw NoSuchElementException("No se encontró la lista")
        val updated = existing.copy(title = normalized, updatedAtMillis = clockMillis())
        current.map { if (it.id == id) updated else it } to updated
    }

    override fun updateDetails(
        id: String,
        title: String,
        description: String?,
        artworkBytes: ByteArray?,
        removeArtwork: Boolean
    ): Playlist = withArtworkCleanup {
        update { current ->
            val existing = current.firstOrNull { it.id == id }
                ?: throw NoSuchElementException("No se encontró la lista")
            val nextArtwork = when {
                artworkBytes != null -> artworkStore.save(artworkBytes)
                removeArtwork -> null
                else -> existing.artworkPath
            }
            val updated = existing.copy(
                title = normalizeTitle(title),
                description = normalizeDescription(description),
                artworkPath = nextArtwork,
                updatedAtMillis = clockMillis()
            )
            current.map { if (it.id == id) updated else it } to updated
        }
    }

    override fun delete(id: String): Boolean = withArtworkCleanup {
        update { current ->
            val updated = current.filterNot { it.id == id }
            updated to (updated.size != current.size)
        }
    }

    override fun addSongs(id: String, songIds: List<String>): Playlist = update { current ->
        val existing = current.firstOrNull { it.id == id }
            ?: throw NoSuchElementException("No se encontró la lista")
        val additions = songIds.filter(String::isNotBlank).distinct().filterNot(existing.songIds.toHashSet()::contains)
        val updated = if (additions.isEmpty()) existing else existing.copy(
            songIds = existing.songIds + additions,
            updatedAtMillis = clockMillis()
        )
        current.map { if (it.id == id) updated else it } to updated
    }

    override fun removeSong(id: String, songId: String): Playlist = update { current ->
        val existing = current.firstOrNull { it.id == id }
            ?: throw NoSuchElementException("No se encontró la lista")
        val updated = if (songId !in existing.songIds) existing else existing.copy(
            songIds = existing.songIds.filterNot { it == songId },
            updatedAtMillis = clockMillis()
        )
        current.map { if (it.id == id) updated else it } to updated
    }

    private fun <T> update(transform: (List<Playlist>) -> Pair<List<Playlist>, T>): T = lock.withLock {
        val current = loadLocked()
        val (updated, result) = transform(current)
        if (updated != current) writeLocked(updated)
        result
    }

    private fun loadLocked(): List<Playlist> {
        if (!catalogFile.exists()) return emptyList()
        try {
            val array = JSONArray(catalogFile.readText())
            return buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val songIdsJson = item.optJSONArray("songIds") ?: JSONArray()
                    val songIds = buildList {
                        for (songIndex in 0 until songIdsJson.length()) {
                            add(songIdsJson.getString(songIndex))
                        }
                    }
                    val created = item.optLong("createdAtMillis", 0L)
                    add(Playlist(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        songIds = songIds,
                        createdAtMillis = created,
                        updatedAtMillis = item.optLong("updatedAtMillis", created),
                        description = item.optString("description").takeIf(String::isNotBlank),
                        artworkPath = item.optString("artworkPath").takeIf(String::isNotBlank)
                    ))
                }
            }
        } catch (exception: Exception) {
            throw PlaylistCatalogReadException("No se pudo leer el catálogo de listas", exception)
        }
    }

    private fun writeLocked(playlists: List<Playlist>) {
        val parent = catalogFile.parentFile ?: throw IOException("El catálogo de listas no tiene directorio padre")
        if (!parent.isDirectory && !parent.mkdirs() && !parent.isDirectory) {
            throw IOException("No se pudo crear el directorio de listas")
        }
        val json = JSONArray()
        playlists.forEach { playlist ->
            json.put(JSONObject()
                .put("id", playlist.id)
                .put("title", playlist.title)
                .put("songIds", JSONArray(playlist.songIds))
                .put("createdAtMillis", playlist.createdAtMillis)
                .put("updatedAtMillis", playlist.updatedAtMillis).apply {
                    playlist.description?.let { put("description", it) }
                    playlist.artworkPath?.let { put("artworkPath", it) }
                })
        }
        val temporary = File.createTempFile("${catalogFile.name}.", ".tmp", parent)
        try {
            FileOutputStream(temporary).use { output ->
                output.write(json.toString().toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            try {
                atomicMover.move(temporary, catalogFile)
            } catch (exception: AtomicPlaylistReplacementUnavailableException) {
                throw exception
            } catch (exception: AtomicMoveNotSupportedException) {
                throw AtomicPlaylistReplacementUnavailableException(exception)
            }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun normalizeTitle(title: String): String = title.trim().take(MAX_TITLE_LENGTH).also {
        require(it.isNotEmpty()) { "Escribe un nombre para la lista" }
    }

    private fun normalizeDescription(value: String?): String? = value?.trim()?.take(MAX_DESCRIPTION_LENGTH)?.takeIf(String::isNotEmpty)

    private inline fun <T> withArtworkCleanup(block: () -> T): T = lock.withLock {
        try {
            block().also { cleanupArtworkLocked() }
        } catch (exception: Exception) {
            cleanupArtworkLocked()
            throw exception
        }
    }

    private fun cleanupArtworkLocked() {
        val referenced = runCatching { loadLocked().mapNotNull(Playlist::artworkPath).toSet() }.getOrNull() ?: return
        artworkStore.deleteUnreferenced(referenced)
    }

    companion object {
        private const val MAX_TITLE_LENGTH = 60
        private const val MAX_DESCRIPTION_LENGTH = 500
        private val locks = ConcurrentHashMap<String, ReentrantLock>()

        internal fun withAtomicMover(
            catalogFile: File,
            atomicMover: PlaylistCatalogAtomicMover,
            clockMillis: () -> Long = System::currentTimeMillis,
            artworkStore: PlaylistArtworkStore = PlaylistArtworkStore(catalogFile.parentFile ?: File("."))
        ) = JsonPlaylistCatalogRepository(catalogFile, atomicMover, clockMillis, artworkStore)
    }
}
