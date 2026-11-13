package mx.musicalesdoris.reproductorlocalandroid

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.musicalesdoris.reproductorlocalandroid.library.artwork.AndroidEmbeddedArtworkNormalizer
import mx.musicalesdoris.reproductorlocalandroid.library.artwork.EmbeddedArtworkNormalizer
import mx.musicalesdoris.reproductorlocalandroid.library.artwork.FileSongArtworkStore
import mx.musicalesdoris.reproductorlocalandroid.library.artwork.NoopEmbeddedArtworkNormalizer
import mx.musicalesdoris.reproductorlocalandroid.library.artwork.SongArtworkStore
import mx.musicalesdoris.reproductorlocalandroid.library.metadata.AndroidSongMetadataExtractor
import mx.musicalesdoris.reproductorlocalandroid.library.metadata.AndroidLogMetadataDiagnosticsReporter
import mx.musicalesdoris.reproductorlocalandroid.library.metadata.ExtractedAudioMetadata
import mx.musicalesdoris.reproductorlocalandroid.library.metadata.ArtworkStorageSummary
import mx.musicalesdoris.reproductorlocalandroid.library.metadata.MetadataDiagnosticsReporter
import mx.musicalesdoris.reproductorlocalandroid.library.metadata.NoopMetadataDiagnosticsReporter
import mx.musicalesdoris.reproductorlocalandroid.library.metadata.NoopSongMetadataExtractor
import mx.musicalesdoris.reproductorlocalandroid.library.metadata.SongMetadataExtractor
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class Song(
    val id: String,
    val fileName: String,
    val title: String,
    val favorite: Boolean = false,
    val duration: Double? = null,
    val artist: String? = null,
    val albumArtist: String? = null,
    val album: String? = null,
    val trackNumber: Int? = null,
    val artworkPath: String? = null,
    val genre: String? = null,
    /** File fingerprint for the last extraction attempt; retry fields indicate incomplete attempts. */
    val metadataFingerprint: String? = null,
    val metadataRetryAfterMillis: Long? = null,
    val metadataRetryCount: Int = 0
)

data class SongImportSource(val displayName: String, val copyTo: (File) -> Unit)

interface SongImporter {
    fun createSource(uri: Uri): SongImportSource
}

internal interface SongFileOperations {
    fun isFile(file: File): Boolean
    /** Returns false only when the file did not exist; throws when an existing file cannot be deleted. */
    fun deleteIfExists(file: File): Boolean
}

private object DefaultSongFileOperations : SongFileOperations {
    override fun isFile(file: File): Boolean = file.isFile

    override fun deleteIfExists(file: File): Boolean {
        if (!file.exists()) return false
        if (!file.delete()) throw IOException("No se pudo eliminar ${file.name}")
        return true
    }
}

class SongLibrary internal constructor(
    filesDirectory: File,
    private val catalogRepository: SongCatalogRepository,
    private val songImporter: SongImporter? = null,
    private val songFileOperations: SongFileOperations = DefaultSongFileOperations,
    private val metadataExtractor: SongMetadataExtractor = NoopSongMetadataExtractor,
    private val artworkNormalizer: EmbeddedArtworkNormalizer = NoopEmbeddedArtworkNormalizer,
    private val artworkStore: SongArtworkStore = FileSongArtworkStore(File(filesDirectory, "AlbumArtwork")),
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val metadataDiagnostics: MetadataDiagnosticsReporter = NoopMetadataDiagnosticsReporter,
    private val loadOnConstruction: Boolean = true
) {
    constructor(context: Context) : this(
        filesDirectory = context.filesDir,
        catalogRepository = JsonSongCatalogRepository(
            songsDirectory = File(context.filesDir, "Songs"),
            catalogFile = File(context.filesDir, "songs.json")
        ),
        songImporter = AndroidSongImporter(context),
        metadataExtractor = AndroidSongMetadataExtractor(diagnostics = AndroidLogMetadataDiagnosticsReporter),
        artworkNormalizer = AndroidEmbeddedArtworkNormalizer(),
        artworkStore = FileSongArtworkStore(File(context.filesDir, "AlbumArtwork")),
        metadataDiagnostics = AndroidLogMetadataDiagnosticsReporter,
        loadOnConstruction = false
    )

    private val directory = File(filesDirectory, "Songs")
    private val operationLock = ReentrantLock()

    val error = MutableStateFlow<String?>(null)
    private val _catalogNeedsRecovery = MutableStateFlow(false)
    val catalogNeedsRecovery = _catalogNeedsRecovery.asStateFlow()
    private val _recoveryMessage = MutableStateFlow<String?>(null)
    val recoveryMessage = _recoveryMessage.asStateFlow()
    private val _catalogLoading = MutableStateFlow(!loadOnConstruction)
    val catalogLoading = _catalogLoading.asStateFlow()
    private val _catalogReadable = MutableStateFlow(true)
    private var catalogLoaded = false
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs = _songs.asStateFlow()
    private val _availabilityBySongId = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val availabilityBySongId = _availabilityBySongId.asStateFlow()

    init {
        if (loadOnConstruction) operationLock.withLock { loadCatalogLocked() }
    }

    /** Loads persisted state once. Production callers run this on Dispatchers.IO. */
    fun initializeCatalog(): Boolean = operationLock.withLock { loadCatalogLocked() }

    fun fileFor(song: Song) = File(directory, song.fileName)

    /** Returns the last availability snapshot. Filesystem inspection is performed only by IO callers. */
    fun isAvailable(song: Song): Boolean = _availabilityBySongId.value[song.id] == true

    /** Refreshes one record from disk; callers must invoke this from Dispatchers.IO. */
    fun refreshAvailability(songId: String): Boolean = operationLock.withLock {
        val song = _songs.value.firstOrNull { it.id == songId } ?: return false
        val available = runCatching { songFileOperations.isFile(fileFor(song)) }.getOrDefault(false)
        _availabilityBySongId.value = _availabilityBySongId.value + (songId to available)
        available
    }

    fun artworkFileFor(song: Song): File? = artworkStore.fileFor(song.artworkPath)

    fun artworkFileForPath(relativePath: String?): File? = artworkStore.fileFor(relativePath)

    fun importSongs(uris: List<Uri>): List<Song> = operationLock.withLock {
        if (!ensureCatalogWritable()) return emptyList()
        val importer = songImporter
        if (importer == null) {
            error.value = "No hay un importador de archivos configurado"
            return emptyList()
        }

        val added = mutableListOf<Song>()
        val failures = mutableListOf<String>()
        val metadataWarnings = mutableListOf<String>()
        uris.forEach { uri ->
            val fallbackName = uri.lastPathSegment ?: "Audio"
            try {
                importSourceLocked(importer.createSource(uri), added, failures, metadataWarnings)
            } catch (exception: Exception) {
                failures += fallbackName
                reportMutationFailure(exception)
            }
        }
        finishImport(added, failures, metadataWarnings)
    }

    internal fun importSources(sources: List<SongImportSource>): List<Song> = operationLock.withLock {
        if (!ensureCatalogWritable()) return emptyList()
        val added = mutableListOf<Song>()
        val failures = mutableListOf<String>()
        val metadataWarnings = mutableListOf<String>()
        sources.forEach { source -> importSourceLocked(source, added, failures, metadataWarnings) }
        finishImport(added, failures, metadataWarnings)
    }

    private fun importSourceLocked(
        source: SongImportSource,
        added: MutableList<Song>,
        failures: MutableList<String>,
        metadataWarnings: MutableList<String>
    ) {
        if (!ensureCatalogWritable()) {
            failures += source.displayName
            return
        }
        var destination: File? = null
        var storedArtworkPath: String? = null
        try {
            if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
                throw IOException("No se pudo crear el almacenamiento de canciones")
            }
            val name = source.displayName
            val extension = name.substringAfterLast('.', "").takeIf { it != name }?.let { ".$it" } ?: ""
            val fileName = UUID.randomUUID().toString() + extension
            destination = File(directory, fileName)
            source.copyTo(destination)
            val initialSong = Song(
                id = UUID.randomUUID().toString(),
                fileName = fileName,
                title = name.substringBeforeLast('.', name)
            )
            val fingerprint = metadataExtractor.fingerprint(destination)
            var song = initialSong
            if (fingerprint != null) {
                try {
                    val metadata = metadataExtractor.extract(destination)
                    storedArtworkPath = storeArtwork(metadata.embeddedArtwork, metadataWarnings, source.displayName)
                    val enriched = initialSong.withMetadata(metadata, fingerprint, storedArtworkPath)
                    song = enriched.withMetadataAttempt(fingerprint, incomplete = metadataIsIncomplete(enriched))
                } catch (_: Exception) {
                    metadataDiagnostics.onExtractionFailure()
                    song = initialSong.withMetadataAttempt(fingerprint, incomplete = true)
                    metadataWarnings += source.displayName
                }
            }
            val updated = catalogRepository.updateSongs { current -> current + song }
            _availabilityBySongId.value = _availabilityBySongId.value + (song.id to true)
            _songs.value = updated
            added += song
        } catch (exception: Exception) {
            try {
                val latest = catalogRepository.loadSongs()
                artworkStore.deleteIfUnreferenced(storedArtworkPath, latest)
            } catch (_: Exception) {
                // Keep a possible orphan rather than risking deletion during catalog failure.
            }
            val cleanupFailed = try {
                destination?.let(songFileOperations::deleteIfExists)
                false
            } catch (_: Exception) {
                true
            }
            failures += if (cleanupFailed) {
                "${source.displayName} (no se pudo limpiar el archivo parcial)"
            } else {
                source.displayName
            }
            reportMutationFailure(exception)
        }
    }

    private fun finishImport(
        added: List<Song>,
        failures: List<String>,
        metadataWarnings: List<String>
    ): List<Song> {
        if (_catalogNeedsRecovery.value) {
            // Preserve the recovery guidance set by the failed catalog read.
        } else if (failures.isNotEmpty()) {
            error.value = failures.joinToString(prefix = "No se pudieron importar: ")
        } else if (metadataWarnings.isNotEmpty()) {
            error.value = "Se importó el audio, pero no se pudieron leer todos los metadatos de: " +
                metadataWarnings.distinct().joinToString()
        } else {
            error.value = null
        }
        return added
    }

    private fun storeArtwork(
        embeddedArtwork: ByteArray?,
        warnings: MutableList<String>,
        displayName: String
    ): String? {
        if (embeddedArtwork == null) {
            logArtworkDiagnostic(embedded = false, saved = false, pathExists = false, readable = false)
            return null
        }
        return try {
            val normalized = artworkNormalizer.normalize(embeddedArtwork)
            if (normalized == null) {
                warnings += displayName
                logArtworkDiagnostic(embedded = true, saved = false, pathExists = false, readable = false)
                null
            } else {
                val path = artworkStore.save(normalized)
                val fileExists = artworkStore.fileFor(path)?.isFile == true
                val readable = fileExists && artworkStore.isReadable(path)
                logArtworkDiagnostic(embedded = true, saved = true, pathExists = fileExists, readable = readable)
                if (!readable) {
                    artworkStore.deleteIfUnreferenced(path, _songs.value)
                    warnings += displayName
                    null
                } else path
            }
        } catch (_: Exception) {
            warnings += displayName
            logArtworkDiagnostic(embedded = true, saved = false, pathExists = false, readable = false)
            null
        }
    }

    private fun logArtworkDiagnostic(embedded: Boolean, saved: Boolean, pathExists: Boolean, readable: Boolean) {
        metadataDiagnostics.onArtwork(ArtworkStorageSummary(embedded, saved, pathExists, readable))
    }

    /** Rechecks incomplete entries on a bounded backoff; complete unchanged entries are skipped. */
    fun scanMissingMetadata() = operationLock.withLock {
        if (!ensureCatalogWritable()) return
        val current = try {
            catalogRepository.loadSongs().also(::publishLoadedSongs)
        } catch (exception: Exception) {
            reportMutationFailure(exception)
            return
        }
        val warnings = mutableListOf<String>()
        val now = clockMillis()
        for (song in current) {
            val audioFile = fileFor(song)
            if (!songFileOperations.isFile(audioFile)) continue
            val fingerprint = try {
                metadataExtractor.fingerprint(audioFile)
            } catch (_: Exception) {
                null
            } ?: continue
            val incomplete = metadataIsIncomplete(song)
            if (song.metadataFingerprint == fingerprint && !incomplete) continue
            if (song.metadataFingerprint == fingerprint &&
                song.metadataRetryAfterMillis?.let { now < it } == true) continue

            val metadata = try {
                metadataExtractor.extract(audioFile)
            } catch (_: Exception) {
                metadataDiagnostics.onExtractionFailure()
                warnings += song.title
                persistMetadataUpdate(song.id, fingerprint) { latest ->
                    latest.withMetadataAttempt(fingerprint, incomplete = true)
                }
                continue
            }

            val artworkPath = storeArtwork(metadata.embeddedArtwork, warnings, song.title)
            val updated = persistMetadataUpdate(song.id, fingerprint) { latest ->
                val enriched = latest.withMetadata(
                    metadata = metadata,
                    fingerprint = fingerprint,
                    artworkPath = artworkPath
                )
                enriched.withMetadataAttempt(fingerprint, incomplete = metadataIsIncomplete(enriched))
            }
            if (updated) {
                try {
                    artworkStore.deleteIfUnreferenced(song.artworkPath, _songs.value)
                } catch (_: Exception) {
                    warnings += "carátula de ${song.title}"
                }
            } else if (artworkPath != null) {
                try {
                    artworkStore.deleteIfUnreferenced(artworkPath, _songs.value)
                } catch (_: Exception) {
                    warnings += "carátula de ${song.title}"
                }
            }
        }
        if (warnings.isNotEmpty()) {
            error.value = "No se pudieron completar algunos metadatos o limpiar carátulas: ${warnings.distinct().joinToString()}"
        }
    }

    private fun persistMetadataUpdate(
        songId: String,
        fingerprint: String,
        transform: (Song) -> Song
    ): Boolean = try {
        val updated = catalogRepository.updateSongs { current ->
            current.map { song ->
                if (song.id == songId && metadataExtractor.fingerprint(fileFor(song)) == fingerprint) {
                    transform(song)
                } else {
                    song
                }
            }
        }
        _songs.value = updated
        updated.any { it.id == songId && it.metadataFingerprint == fingerprint }
    } catch (exception: Exception) {
        reportMutationFailure(exception)
        false
    }

    private fun Song.withMetadata(
        metadata: ExtractedAudioMetadata,
        fingerprint: String,
        artworkPath: String?
    ): Song = copy(
        title = metadata.title ?: title,
        artist = metadata.artist ?: artist,
        albumArtist = metadata.albumArtist ?: albumArtist,
        album = metadata.album ?: album,
        duration = metadata.durationSeconds ?: duration,
        trackNumber = metadata.trackNumber ?: trackNumber,
        genre = metadata.genre ?: genre,
        artworkPath = artworkPath ?: this.artworkPath?.takeIf(artworkStore::isReadable),
        metadataFingerprint = fingerprint
    )

    private fun Song.withMetadataAttempt(fingerprint: String, incomplete: Boolean = false): Song {
        if (!incomplete) return copy(
            metadataFingerprint = fingerprint,
            metadataRetryAfterMillis = null,
            metadataRetryCount = 0
        )
        val retryCount = (metadataRetryCount + 1).coerceAtMost(MAX_METADATA_RETRIES)
        val delay = (INITIAL_METADATA_RETRY_DELAY_MS * (1L shl (retryCount - 1)))
            .coerceAtMost(MAX_METADATA_RETRY_DELAY_MS)
        return copy(
            metadataFingerprint = fingerprint,
            metadataRetryAfterMillis = clockMillis() + delay,
            metadataRetryCount = retryCount
        )
    }

    private fun metadataIsIncomplete(song: Song): Boolean =
        (song.artist.isNullOrBlank() && song.albumArtist.isNullOrBlank()) ||
            song.album.isNullOrBlank() || !artworkStore.isReadable(song.artworkPath)

    fun toggleFavorite(id: String) = operationLock.withLock {
        if (!ensureCatalogWritable()) return
        try {
            val updated = catalogRepository.updateSongs { current ->
                current.map { if (it.id == id) it.copy(favorite = !it.favorite) else it }
            }
            _songs.value = updated
            error.value = null
        } catch (exception: Exception) {
            reportMutationFailure(exception)
        }
    }

    fun delete(song: Song): Boolean = operationLock.withLock {
        if (!ensureCatalogWritable()) return false

        var removedSong: Song? = null
        var audioDeleted = false
        val updated = try {
            catalogRepository.updateSongs { current ->
                val existingSong = current.firstOrNull { it.id == song.id }
                    ?: return@updateSongs current
                removedSong = existingSong

                // Delete the audio before committing the catalog removal. If this fails,
                // updateSongs never writes. If the later JSON write fails, the old record
                // remains durable and is now explicitly represented as unavailable.
                audioDeleted = songFileOperations.deleteIfExists(fileFor(existingSong))
                current.filterNot { it.id == existingSong.id }
            }
        } catch (exception: Exception) {
            if (audioDeleted) {
                try {
                    publishLoadedSongs(catalogRepository.loadSongs())
                } catch (readException: Exception) {
                    reportMutationFailure(readException)
                }
                removedSong?.let { removed ->
                    _availabilityBySongId.value = _availabilityBySongId.value + (removed.id to false)
                }
                if (!_catalogNeedsRecovery.value) {
                    error.value = "Se eliminó el archivo de audio, pero no se pudo guardar la eliminación del registro. " +
                        "La canción permanece en la biblioteca como no disponible; vuelve a intentar eliminarla. " +
                        (exception.localizedMessage ?: "Error de persistencia")
                }
            } else {
                reportMutationFailure(exception)
            }
            return false
        }

        val existingSong = removedSong
        if (existingSong == null) {
            _songs.value = updated
            return false
        }

        _availabilityBySongId.value = _availabilityBySongId.value - existingSong.id
        _songs.value = updated
        error.value = null
        try {
            artworkStore.deleteIfUnreferenced(existingSong.artworkPath, updated)
        } catch (_: Exception) {
            error.value = "La canción se eliminó, pero no se pudo limpiar su carátula"
        }
        true
    }

    fun recoverUnreadableCatalog(): Boolean = operationLock.withLock {
        if (!_catalogNeedsRecovery.value) {
            error.value = "El catálogo no requiere recuperación"
            return false
        }
        try {
            val backup = catalogRepository.recoverUnreadableCatalog()
            _availabilityBySongId.value = emptyMap()
            _songs.value = emptyList()
            _catalogReadable.value = true
            catalogLoaded = true
            _catalogLoading.value = false
            _catalogNeedsRecovery.value = false
            error.value = null
            _recoveryMessage.value = "Se creó una biblioteca nueva. El catálogo original se conservó en ${backup.name}."
            true
        } catch (exception: Exception) {
            error.value = "No se pudo recuperar el catálogo; el archivo original se conserva: ${exception.localizedMessage}"
            false
        }
    }

    fun clearMessages() {
        if (_catalogNeedsRecovery.value) return
        error.value = null
        _recoveryMessage.value = null
    }

    private fun ensureCatalogWritable(): Boolean {
        if (!catalogLoaded && !loadCatalogLocked()) return false
        if (!_catalogReadable.value) {
            error.value = error.value ?: "No se pudo leer la biblioteca; no se permitirán cambios hasta poder leer el catálogo"
            return false
        }
        if (!_catalogNeedsRecovery.value) return true
        error.value = "El catálogo no se puede modificar hasta completar una recuperación explícita"
        return false
    }

    private fun reportMutationFailure(exception: Exception) {
        if (exception is CatalogReadException) {
            _catalogNeedsRecovery.value = true
            error.value = unreadableCatalogMessage()
        } else {
            error.value = exception.localizedMessage ?: "No se pudo guardar la biblioteca"
        }
    }

    private fun loadCatalogLocked(): Boolean {
        if (catalogLoaded) return _catalogReadable.value
        _catalogLoading.value = true
        return try {
            publishLoadedSongs(catalogRepository.loadSongs())
            _catalogReadable.value = true
            _catalogNeedsRecovery.value = false
            true
        } catch (exception: Exception) {
            _songs.value = emptyList()
            _catalogReadable.value = false
            if (exception is CatalogReadException) _catalogNeedsRecovery.value = true
            error.value = if (exception is CatalogReadException) {
                unreadableCatalogMessage()
            } else {
                "No se pudo leer la biblioteca: ${exception.localizedMessage}"
            }
            false
        } finally {
            catalogLoaded = true
            _catalogLoading.value = false
        }
    }

    private fun publishLoadedSongs(loaded: List<Song>) {
        val availability = loaded.associate { song ->
            song.id to runCatching { songFileOperations.isFile(fileFor(song)) }.getOrDefault(false)
        }
        _availabilityBySongId.value = availability
        _songs.value = loaded
    }

    private fun unreadableCatalogMessage(): String =
        "El catálogo no se puede leer y los cambios están bloqueados para protegerlo. " +
            "Si eliges recuperar, se guardará una copia exacta y se iniciará un catálogo vacío; " +
            "los archivos de audio se conservarán."

    private companion object {
        const val INITIAL_METADATA_RETRY_DELAY_MS = 6 * 60 * 60 * 1000L
        const val MAX_METADATA_RETRY_DELAY_MS = 7 * 24 * 60 * 60 * 1000L
        const val MAX_METADATA_RETRIES = 20
    }
}
