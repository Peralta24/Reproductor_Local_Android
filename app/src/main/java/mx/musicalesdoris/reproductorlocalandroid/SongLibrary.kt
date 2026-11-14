package mx.musicalesdoris.reproductorlocalandroid

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.content.ContentUris
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.text.Normalizer
import java.util.UUID
import mx.musicalesdoris.reproductorlocalandroid.library.AlbumSummary
import mx.musicalesdoris.reproductorlocalandroid.library.ArtistSummary
import mx.musicalesdoris.reproductorlocalandroid.library.GenreSummary
import mx.musicalesdoris.reproductorlocalandroid.library.FolderSummary
import mx.musicalesdoris.reproductorlocalandroid.library.PlaylistSummary
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.calculateArtworkSampleSize

data class Song(
    val id: String, val fileName: String, val title: String, val favorite: Boolean = false,
    val favoritedAt: Long? = null, val importedAt: Long = System.currentTimeMillis(),
    val durationMs: Long? = null, val album: String? = null, val artist: String? = null,
    val genre: String? = null, val track: Int? = null, val artworkFileName: String? = null,
    val sourceFolder: String? = null, val metadataScanned: Boolean = false,
    val contentUri: String? = null, val sizeBytes: Long = 0L,
    val mediaAvailable: Boolean = true, val importSourceIdentity: String? = null,
    val privateAudioFingerprint: String? = null, val relinkDismissed: Boolean = false,
    val albumArtist: String? = null, val mediaAlbumId: Long? = null,
    val mediaStoreVolume: String? = null, val albumArtistScanned: Boolean = false
) {
    val subtitle: String get() = listOfNotNull(artist, album).filter { it.isNotBlank() }
        .joinToString(" · ").ifBlank { "Archivo local" }
}

data class Playlist(
    val id: String = UUID.randomUUID().toString(), val title: String,
    val description: String = "", val songIds: List<String> = emptyList(),
    val coverFileName: String? = null,
    val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis()
)

data class PendingSongRelink(val librarySong: Song, val importedSong: Song)

class SongLibrary(private val context: Context) {
    private val hiddenMediaIds = context.getSharedPreferences("library", Context.MODE_PRIVATE)
    private val directory = File(context.filesDir, "Songs").apply { mkdirs() }
    private val artworkDirectory = File(context.filesDir, "AlbumArtwork").apply { mkdirs() }
    private val playlistCoverDirectory = File(context.filesDir, "PlaylistCovers").apply { mkdirs() }
    private val favoriteArtworkDirectory = File(context.filesDir, "FavoriteArtwork").apply { mkdirs() }
    private val favoriteArtworkPreferences = context.getSharedPreferences("favorite_artwork", Context.MODE_PRIVATE)
    private val catalog = File(context.filesDir, "songs.json")
    private val playlistCatalog = File(context.filesDir, "playlists.json")
    private val pendingRelinkCatalog = File(context.filesDir, "pending-relinks.json")
    private var catalogReadFailed = false
    private var playlistReadFailed = false
    private val metadataScanMutex = Mutex()
    private val deviceScanMutex = Mutex()
    @Volatile private var lastDeviceScanAtMs = 0L
    val error = MutableStateFlow<String?>(null)
    private val _songs = MutableStateFlow(readSongs())
    val songs = _songs.asStateFlow()
    private data class ArtworkIndex(val songs: List<Song>, val byAlbum: Map<String, String>)
    @Volatile private var artworkIndex: ArtworkIndex? = null
    private val _playlists = MutableStateFlow(readPlaylists())
    val playlists = _playlists.asStateFlow()
    private val _pendingRelinks = MutableStateFlow(readPendingRelinks())
    val pendingRelinks = _pendingRelinks.asStateFlow()
    private val _favoriteArtwork = MutableStateFlow(readFavoriteArtworkFile())
    val favoriteArtwork = _favoriteArtwork.asStateFlow()
    init {
        CoroutineScope(Dispatchers.IO).launch {
            enrichMissingMetadata()
        }
    }
    private suspend fun enrichMissingMetadata() {
        metadataScanMutex.lock()
        try {
            val original = _songs.value.filter { !it.metadataScanned || !it.albumArtistScanned }
            if (original.isEmpty()) return
            val updated = HashMap<String, Song>(original.size)
            for (song in original) {
                updated[song.id] = enrich(song)
                delay(15)
            }
            synchronized(this) {
                val merged = _songs.value.map { current ->
                    updated[current.id]?.takeIf { !current.metadataScanned || !current.albumArtistScanned } ?: current
                }
                saveSongs(merged)
                _songs.value = merged
            }
        } catch (exception: Exception) {
            error.value = "No se pudieron actualizar los metadatos: ${exception.localizedMessage}"
        } finally {
            metadataScanMutex.unlock()
        }
    }
    private fun enrich(song: Song): Song {
        val existingArtwork = song.artworkFileName?.let { File(artworkDirectory, it) }
            ?.takeIf(File::isFile)?.name
        val needsMetadata = song.album == null || song.artist == null || song.genre == null ||
            song.durationMs == null || song.track == null || !song.albumArtistScanned
        if (!needsMetadata && existingArtwork != null) return song.copy(metadataScanned = true, albumArtistScanned = true)
        val retriever = MediaMetadataRetriever()
        return try {
            if (song.contentUri != null) retriever.setDataSource(context, Uri.parse(song.contentUri))
            else retriever.setDataSource(fileFor(song).absolutePath)
            fun field(key: Int, current: String?) = current ?: retriever.extractMetadata(key)?.trim()?.takeIf(String::isNotEmpty)
            fun numericField(key: Int, current: Long?) = current ?: retriever.extractMetadata(key)?.toLongOrNull()
            val artName = existingArtwork ?: retriever.embeddedPicture?.let { bytes ->
                val name = "${song.id}.jpg"
                File(artworkDirectory, name).writeBytes(bytes)
                name
            }
            val track = song.track ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.toIntOrNull()
            song.copy(album = field(MediaMetadataRetriever.METADATA_KEY_ALBUM, song.album),
                artist = field(MediaMetadataRetriever.METADATA_KEY_ARTIST, song.artist),
                albumArtist = field(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, song.albumArtist),
                genre = field(MediaMetadataRetriever.METADATA_KEY_GENRE, song.genre),
                durationMs = numericField(MediaMetadataRetriever.METADATA_KEY_DURATION, song.durationMs),
                track = track,
                artworkFileName = existingArtwork ?: artName, metadataScanned = true, albumArtistScanned = true)
        } catch (_: Exception) { song.copy(metadataScanned = true, albumArtistScanned = true) }
        finally { retriever.release() }
    }

    private fun sha256For(uri: Uri): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        (context.contentResolver.openInputStream(uri) ?: error("No se pudo leer el origen")).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        } ?: return null
        digest.digest().toHexString()
    }.getOrNull()

    fun fileFor(song: Song) = File(directory, song.fileName)
    fun folderNameFor(song: Song): String = songFolderName(song, fileFor(song).parentFile?.name)
    fun uriFor(song: Song): Uri = song.contentUri?.let(Uri::parse) ?: Uri.fromFile(fileFor(song))
    fun sizeFor(song: Song): Long = if (song.contentUri != null) song.sizeBytes else fileFor(song).length()

    /** Reconciles the system's audio index without copying the device's music. */
    fun scanDeviceMusic() {
        if (!deviceScanMutex.tryLock()) return
        try {
            val scanStartedAt = SystemClock.elapsedRealtime()
            if (isDeviceScanCoolingDown(lastDeviceScanAtMs, scanStartedAt, 5_000L)) return
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val folderColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
            val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DATE_ADDED,
                folderColumn, MediaStore.Audio.Media.TRACK, MediaStore.Audio.AudioColumns.ALBUM_ID).let { columns ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        columns + MediaStore.Audio.AudioColumns.ALBUM_ARTIST + MediaStore.MediaColumns.VOLUME_NAME
                    else columns
                }
            val found = mutableListOf<Song>()
            val oldByUri = _songs.value.mapNotNull { song -> song.contentUri?.let { it to song } }.toMap()
            val importedBySourceIdentity = _songs.value
                .filter { it.contentUri == null && it.privateAudioFingerprint != null }
                .mapNotNull { song -> song.importSourceIdentity?.let { it to song } }.toMap()
            val hidden = hiddenMediaIds.getStringSet("hidden", emptySet()).orEmpty()
            context.contentResolver.query(collection, projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { cursor ->
                fun string(column: String) = cursor.getString(cursor.getColumnIndexOrThrow(column))
                fun number(column: String) = cursor.getLong(cursor.getColumnIndexOrThrow(column))
                while (cursor.moveToNext()) {
                    val mediaId = number(MediaStore.Audio.Media._ID)
                    val uri = ContentUris.withAppendedId(collection, mediaId).toString()
                    val volume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME)) ?: "external"
                    else "external"
                    val name = string(MediaStore.Audio.Media.DISPLAY_NAME) ?: continue
                    val dateAdded = number(MediaStore.Audio.Media.DATE_ADDED) * 1000
                    val sizeBytes = number(MediaStore.Audio.Media.SIZE)
                    val sourceIdentity = mediaStoreIdentity(uri, name, sizeBytes, dateAdded)
                    if ("media:$mediaId" in hidden || "mediaSourceKey:$sourceIdentity" in hidden) continue
                    val matchingImport = importedBySourceIdentity[sourceIdentity]
                    val previous = oldByUri[uri]
                    // Hash only a legacy imported/MediaStore pair that already exists in the catalog.
                    if (matchingImport != null && previous == null) continue
                    val verifiedImportFingerprint = matchingImport?.privateAudioFingerprint?.takeIf { fingerprint ->
                        sha256For(Uri.parse(uri)) == fingerprint
                    }
                    val old = previous?.takeIf {
                        it.fileName == name && it.sizeBytes == sizeBytes && it.importedAt == dateAdded
                    }
                    val title = string(MediaStore.Audio.Media.TITLE)?.takeIf { it.isNotBlank() }
                        ?: name.substringBeforeLast('.', name)
                    val albumArtist = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ARTIST))
                            ?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: old?.albumArtist
                    else old?.albumArtist
                    found += Song(id = old?.id ?: if (previous != null) UUID.randomUUID().toString() else "media:$volume:$mediaId", fileName = name,
                        title = title, favorite = old?.favorite ?: false,
                        favoritedAt = old?.favoritedAt,
                        importedAt = old?.importedAt ?: dateAdded,
                        durationMs = number(MediaStore.Audio.Media.DURATION).takeIf { it > 0 },
                        album = string(MediaStore.Audio.Media.ALBUM)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: old?.album,
                        artist = string(MediaStore.Audio.Media.ARTIST)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: old?.artist,
                        genre = old?.genre,
                        track = number(MediaStore.Audio.Media.TRACK).toInt().takeIf { it > 0 },
                        artworkFileName = old?.artworkFileName,
                        sourceFolder = string(folderColumn)?.let { path ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) path.trimEnd('/')
                            else path.substringBeforeLast('/', "")
                        }?.takeIf { it.isNotBlank() },
                        metadataScanned = old?.metadataScanned ?: false,
                        contentUri = uri, sizeBytes = sizeBytes, mediaAvailable = true,
                        albumArtist = albumArtist,
                        mediaAlbumId = number(MediaStore.Audio.AudioColumns.ALBUM_ID).takeIf { it >= 0 } ?: old?.mediaAlbumId,
                        mediaStoreVolume = volume,
                        albumArtistScanned = albumArtist != null || old?.albumArtistScanned == true,
                        importSourceIdentity = verifiedImportFingerprint?.let { sourceIdentity },
                        privateAudioFingerprint = verifiedImportFingerprint)
                }
            } ?: return
            val updated = synchronized(this) {
                val current = _songs.value
                val currentHidden = hiddenMediaIds.getStringSet("hidden", emptySet()).orEmpty()
                val stillVisible = found.filterNot { candidate ->
                    val identity = mediaStoreIdentity(candidate.contentUri.orEmpty(), candidate.fileName,
                        candidate.sizeBytes, candidate.importedAt)
                    val mediaId = candidate.contentUri?.substringAfterLast('/')
                    "mediaSourceKey:$identity" in currentHidden || "media:$mediaId" in currentHidden
                }
                val reconciled = reconcileMediaStoreSnapshot(current, stillVisible)
                val deduplicated = consolidateExactDuplicateSongs(reconciled)
                val next = deduplicated.songs
                val nextPlaylists = remapPlaylistSongIds(_playlists.value, deduplicated.idRemap)
                next.takeIf { it != current }?.also { nextSongs ->
                    try {
                        saveSongs(nextSongs)
                        if (nextPlaylists != _playlists.value) savePlaylists(nextPlaylists)
                    } catch (failure: Exception) {
                        runCatching { saveSongs(current) }
                        runCatching { savePlaylists(_playlists.value) }
                        throw failure
                    }
                    _songs.value = nextSongs
                    _playlists.value = nextPlaylists
                } ?: current
            }
            lastDeviceScanAtMs = SystemClock.elapsedRealtime()
            if (updated.any { !it.metadataScanned || !it.albumArtistScanned }) {
                CoroutineScope(Dispatchers.IO).launch { enrichMissingMetadata() }
            }
        } catch (exception: Exception) {
            error.value = "No se pudo leer la música del dispositivo: ${exception.localizedMessage}"
        } finally {
            deviceScanMutex.unlock()
        }
    }

    @Synchronized fun markDeviceMediaUnavailable() {
        try {
            val current = _songs.value
            val updated = markMediaStoreSnapshotUnavailable(current)
            if (updated != current) {
                saveSongs(updated)
                _songs.value = updated
            }
        } catch (exception: Exception) {
            error.value = "No se pudo actualizar la disponibilidad de la biblioteca: ${exception.localizedMessage}"
        }
    }
    fun albums(): List<AlbumSummary> = _songs.value.filter { it.albumKey() != null }
        .groupBy { it.albumKey()!! }.map { (key, members) ->
            AlbumSummary(key, members.first().album ?: "", albumDisplayArtist(members),
                members.size, members.maxOf { it.importedAt })
        }
    fun artists(): List<ArtistSummary> = _songs.value.filter { it.artist.key() != null }
        .groupBy { it.artist.key()!! }.map { (key, members) ->
            ArtistSummary(key, members.first().artist ?: "", members.mapNotNull { it.albumKey() }.distinct().size,
                members.size, members.maxOf { it.importedAt })
        }
    fun genres(): List<GenreSummary> = _songs.value.filter { it.genre.key() != null }
        .groupBy { it.genre.key()!! }.map { (key, members) ->
            GenreSummary(key, members.first().genre ?: "", members.size, members.maxOf { it.importedAt })
        }
    fun folders(): List<FolderSummary> = _songs.value.groupBy { it.sourceFolder ?: "Local" }
        .map { (path, members) ->
            FolderSummary(path, path.substringAfterLast('/').ifBlank { "Local" }, path,
                members.size, members.maxOf { it.importedAt })
        }
    fun playlistSummary(playlist: Playlist): PlaylistSummary {
        val members = songsIn(playlist)
        return PlaylistSummary(playlist.id, playlist.title, members.size,
            if (members.all { it.durationMs != null }) members.sumOf { it.durationMs ?: 0L } else null)
    }
    fun artworkFor(song: Song): File? {
        val catalog = _songs.value
        val index = artworkIndex?.takeIf { it.songs === catalog } ?: ArtworkIndex(catalog,
            catalog.mapNotNull { candidate ->
                val key = candidate.albumKey()
                val name = candidate.artworkFileName
                if (key != null && name != null) key to name else null
            }.toMap()).also { artworkIndex = it }
        val name = song.artworkFileName ?: song.albumKey()?.let(index.byAlbum::get)
        return name?.let { File(artworkDirectory, it) }?.takeIf(File::exists)
    }
    fun coverFor(playlist: Playlist): File? = playlist.coverFileName?.let { File(playlistCoverDirectory, it) }?.takeIf(File::exists)
    private fun readFavoriteArtworkFile(): File? = favoriteArtworkPreferences.getString("fileName", null)
        ?.takeIf { it.isNotBlank() && File(it).name == it }
        ?.let { File(favoriteArtworkDirectory, it).takeIf(File::isFile) }

    @Synchronized fun setFavoriteArtwork(uri: Uri): Boolean {
        val previous = favoriteArtworkPreferences.getString("fileName", null)
        val fileName = "favorites-${UUID.randomUUID()}.jpg"
        val destination = File(favoriteArtworkDirectory, fileName)
        val temporary = File(favoriteArtworkDirectory, "$fileName.tmp")
        var bitmap: Bitmap? = null
        var destinationCreated = false
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val readable = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
                true
            } ?: false
            if (!readable || bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                bounds.outWidth > 60_000 || bounds.outHeight > 60_000) {
                error("La imagen no es válida o no se puede leer.")
            }
            val sampleSize = calculateArtworkSampleSize(bounds.outWidth, bounds.outHeight, 1200)
            bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            } ?: error("No se pudo decodificar la imagen.")
            FileOutputStream(temporary).use { output ->
                if (!bitmap!!.compress(Bitmap.CompressFormat.JPEG, 88, output)) error("No se pudo guardar la imagen.")
                output.fd.sync()
            }
            if (!temporary.renameTo(destination)) error("No se pudo preparar la nueva portada.")
            destinationCreated = true
            if (!favoriteArtworkPreferences.edit().putString("fileName", fileName).commit()) {
                error("No se pudo guardar la referencia de la portada.")
            }
            _favoriteArtwork.value = destination
            previous?.takeIf { it != fileName && File(it).name == it }
                ?.let { File(favoriteArtworkDirectory, it).delete() }
            error.value = null
            true
        } catch (exception: Exception) {
            temporary.delete()
            if (destinationCreated && favoriteArtworkPreferences.getString("fileName", null) != fileName) {
                destination.delete()
            }
            error.value = "No se pudo cambiar la portada de Favoritos: ${exception.localizedMessage ?: "error de almacenamiento"}"
            false
        } finally {
            bitmap?.recycle()
        }
    }

    @Synchronized fun removeFavoriteArtwork(): Boolean {
        val previous = favoriteArtworkPreferences.getString("fileName", null)
        if (!favoriteArtworkPreferences.edit().remove("fileName").commit()) {
            error.value = "No se pudo restaurar la portada automática de Favoritos."
            return false
        }
        _favoriteArtwork.value = null
        previous?.takeIf { File(it).name == it }?.let { File(favoriteArtworkDirectory, it).delete() }
        error.value = null
        return true
    }
    fun songsIn(playlist: Playlist): List<Song> = playlist.songIds.mapNotNull { id -> _songs.value.firstOrNull { it.id == id } }
    fun songsInAlbum(key: String): List<Song> = _songs.value.filter { it.albumKey() == key }
        .sortedWith(compareBy<Song> { it.track ?: Int.MAX_VALUE }.thenBy { it.importedAt })
    fun songsByArtist(key: String): List<Song> = _songs.value.filter { it.artist.key() == key }
    fun songsInGenre(key: String): List<Song> = _songs.value.filter { it.genre.key() == key }
    fun songsInFolder(path: String): List<Song> = _songs.value.filter { (it.sourceFolder ?: "Local").equals(path, true) }

    @Synchronized fun importSongs(uris: List<Uri>): List<Song> {
        val added = mutableListOf<Song>()
        val failures = mutableListOf<String>()
        for (uri in uris) {
            val name = displayName(uri)
            val sourceIdentity = mediaStoreIdentityFor(uri)
            if (sourceIdentity != null && _songs.value.any { it.matchesMediaStoreIdentity(sourceIdentity) }) continue
            val extension = name.substringAfterLast('.', "").takeIf { it != name }?.let { ".$it" } ?: ""
            val fileName = UUID.randomUUID().toString() + extension
            val destination = File(directory, fileName)
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    DigestInputStream(input, digest).use { digestInput ->
                        destination.outputStream().use { output -> digestInput.copyTo(output) }
                    }
                } ?: error("No se pudo abrir el archivo")
                val fingerprint = digest.digest().toHexString()
                val matchingReference = _songs.value.firstOrNull { existing ->
                    existing.contentUri == null && existing.privateAudioFingerprint == fingerprint &&
                        !existing.mediaAvailable && !fileFor(existing).isFile && !existing.relinkDismissed
                }
                val alreadyImported = _songs.value.any { existing ->
                    existing.contentUri == null && existing.privateAudioFingerprint == fingerprint &&
                        existing.mediaAvailable && fileFor(existing).isFile
                } || _pendingRelinks.value.any { it.importedSong.privateAudioFingerprint == fingerprint }
                if (alreadyImported) {
                    destination.delete()
                    continue
                }
                val song = readMetadata(destination, name, uri, sourceIdentity, fingerprint)
                val updated = _songs.value + song
                if (matchingReference != null) {
                    savePendingRelinks(_pendingRelinks.value + PendingSongRelink(matchingReference, song))
                } else {
                    saveSongs(updated)
                    _songs.value = updated
                }
                added += song
            } catch (exception: Exception) {
                destination.delete()
                failures += name
            }
        }
        error.value = failures.takeIf { it.isNotEmpty() }?.joinToString(prefix = "No se pudieron importar: ")
        return added
    }

    private fun readMetadata(file: File, name: String, uri: Uri, sourceIdentity: String?,
                             fingerprint: String): Song {
        val retriever = MediaMetadataRetriever()
        val id = UUID.randomUUID().toString()
        var title = name.substringBeforeLast('.', name)
        var album: String? = null
        var artist: String? = null
        var albumArtist: String? = null
        var genre: String? = null
        var duration: Long? = null
        var track: Int? = null
        var artwork: String? = null
        try {
            retriever.setDataSource(file.absolutePath)
            fun field(key: Int) = retriever.extractMetadata(key)?.trim()?.takeIf(String::isNotEmpty)
            title = field(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: title
            album = field(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            artist = field(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            albumArtist = field(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            genre = field(MediaMetadataRetriever.METADATA_KEY_GENRE)
            duration = field(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            track = field(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.substringBefore('/')?.toIntOrNull()
            retriever.embeddedPicture?.let { bytes ->
                artwork = "$id.jpg"
                File(artworkDirectory, artwork!!).writeBytes(bytes)
            }
        } catch (_: Exception) {
            // The audio file may still be playable without readable metadata.
        } finally { retriever.release() }
        return Song(id, file.name, title, importedAt = System.currentTimeMillis(), durationMs = duration,
            album = album, artist = artist, genre = genre, track = track, artworkFileName = artwork,
            sourceFolder = uri.path?.substringBeforeLast('/')?.takeIf { it.isNotBlank() }, metadataScanned = true,
            importSourceIdentity = sourceIdentity, privateAudioFingerprint = fingerprint,
            albumArtist = albumArtist, albumArtistScanned = true)
    }

    @Synchronized fun resolvePendingRelink(librarySongId: String, importedSongId: String, relink: Boolean) {
        val pending = _pendingRelinks.value.firstOrNull {
            it.librarySong.id == librarySongId && it.importedSong.id == importedSongId
        } ?: return
        try {
            if (relink) {
                val existing = _songs.value.firstOrNull { it.id == librarySongId }
                val imported = pending.importedSong.takeIf { it.id == importedSongId && fileFor(it).isFile }
                if (existing == null || imported == null || existing.mediaAvailable || fileFor(existing).isFile ||
                    existing.privateAudioFingerprint == null || existing.privateAudioFingerprint != imported.privateAudioFingerprint) {
                    error("La referencia ya no coincide con el archivo importado")
                }
                val updated = _songs.value.filterNot { it.id == importedSongId }.map { song ->
                    if (song.id == librarySongId) confirmedSongRelink(song, imported)
                    else song
                }
                saveSongs(updated)
                _songs.value = updated
            } else {
                val updated = _songs.value.map { song ->
                    if (song.id == librarySongId) song.copy(relinkDismissed = true) else song
                } + pending.importedSong.copy(relinkDismissed = true)
                saveSongs(updated)
                _songs.value = updated
            }
            savePendingRelinks(_pendingRelinks.value - pending)
        } catch (exception: Exception) {
            error.value = "No se pudo actualizar la referencia de la canción: ${exception.localizedMessage}"
        }
    }

    private fun readPendingRelinks(): List<PendingSongRelink> = try {
        if (!pendingRelinkCatalog.isFile) emptyList() else {
            val array = JSONArray(pendingRelinkCatalog.readText())
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val missing = _songs.value.firstOrNull { it.id == item.getString("librarySongId") } ?: continue
                    val imported = item.getJSONObject("importedSong").toSong()
                    if (!missing.mediaAvailable && !fileFor(missing).isFile && fileFor(imported).isFile) {
                        add(PendingSongRelink(missing, imported))
                    }
                }
            }
        }
    } catch (exception: Exception) {
        error.value = "No se pudieron leer las coincidencias de biblioteca: ${exception.localizedMessage}"
        emptyList()
    }

    private fun savePendingRelinks(pending: List<PendingSongRelink>) {
        val array = JSONArray()
        pending.forEach { item ->
            array.put(JSONObject().put("librarySongId", item.librarySong.id)
                .put("importedSong", item.importedSong.toJsonObject()))
        }
        atomicWrite(pendingRelinkCatalog, array.toString())
        _pendingRelinks.value = pending
    }

    private fun mediaStoreIdentityFor(uri: Uri): String? {
        val documentId = runCatching {
            if (DocumentsContract.isDocumentUri(context, uri)) DocumentsContract.getDocumentId(uri) else null
        }.getOrNull()
        val canonical = canonicalMediaStoreAudioUri(uri.toString(), documentId) ?: return null
        val target = Uri.parse(canonical)
        return runCatching {
            context.contentResolver.query(target, arrayOf(MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DATE_ADDED), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val name = cursor.getString(0) ?: return@use null
                mediaStoreIdentity(canonical, name, cursor.getLong(1), cursor.getLong(2) * 1000)
            }
        }.getOrNull()
    }

    @Synchronized fun toggleFavorite(id: String) {
        val updated = _songs.value.map {
            if (it.id == id) it.copy(favorite = !it.favorite,
                favoritedAt = if (it.favorite) null else System.currentTimeMillis()) else it
        }
        try { saveSongs(updated); _songs.value = updated }
        catch (exception: Exception) { error.value = "No se pudo guardar el favorito: ${exception.localizedMessage}" }
    }

    @Synchronized fun delete(song: Song): Boolean {
        val previous = _songs.value
        val updated = previous.filterNot { it.id == song.id }
        val previousPlaylists = _playlists.value
        val updatedPlaylists = previousPlaylists.map { it.copy(songIds = it.songIds - song.id) }
        return try {
            saveSongs(updated)
            savePlaylists(updatedPlaylists)
            if (song.contentUri == null && !fileFor(song).delete()) error("No se pudo eliminar el archivo")
            song.artworkFileName?.let { File(artworkDirectory, it).delete() }
            _songs.value = updated
            _playlists.value = updatedPlaylists
            if (song.contentUri != null) {
                val identity = mediaStoreIdentity(song.contentUri, song.fileName, song.sizeBytes, song.importedAt)
                val hidden = hiddenMediaIds.getStringSet("hidden", emptySet()).orEmpty() + "mediaSourceKey:$identity"
                hiddenMediaIds.edit().putStringSet("hidden", hidden).apply()
            }
            true
        } catch (exception: Exception) {
            try { saveSongs(previous); savePlaylists(previousPlaylists) } catch (_: Exception) {}
            error.value = "No se pudo eliminar ${song.title}: ${exception.localizedMessage}"
            false
        }
    }

    @Synchronized fun createPlaylist(title: String, description: String = ""): Playlist? {
        val clean = title.trim().take(60)
        if (clean.isEmpty()) return null
        val playlist = Playlist(title = clean, description = description.trim())
        updatePlaylists(_playlists.value + playlist)
        return playlist
    }
    @Synchronized fun renamePlaylist(id: String, title: String, description: String) {
        val clean = title.trim().take(60)
        if (clean.isEmpty()) return
        updatePlaylists(_playlists.value.map { if (it.id == id) it.copy(title = clean,
            description = description.trim(), updatedAt = System.currentTimeMillis()) else it })
    }
    @Synchronized fun deletePlaylist(id: String) {
        val old = _playlists.value.firstOrNull { it.id == id }
        updatePlaylists(_playlists.value.filterNot { it.id == id })
        old?.coverFileName?.let { File(playlistCoverDirectory, it).delete() }
    }
    @Synchronized fun removePlaylistCover(id: String) {
        val playlist = _playlists.value.firstOrNull { it.id == id } ?: return
        updatePlaylists(_playlists.value.map { if (it.id == id) it.copy(coverFileName = null,
            updatedAt = System.currentTimeMillis()) else it })
        playlist.coverFileName?.let { File(playlistCoverDirectory, it).delete() }
    }
    @Synchronized fun setPlaylistCover(id: String, uri: Uri): Boolean {
        val old = _playlists.value.firstOrNull { it.id == id } ?: return false
        val destination = File(playlistCoverDirectory, "$id-${UUID.randomUUID()}.jpg")
        var bitmap: Bitmap? = null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val opened = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
                true
            } ?: false
            if (!opened) error("No se pudo abrir la imagen")
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outWidth > 60000 || bounds.outHeight > 60000)
                error("La imagen no es válida o excede el tamaño permitido")
            var sample = 1
            while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 1200) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("No se pudo leer la imagen")
            destination.outputStream().use { output ->
                if (!bitmap!!.compress(Bitmap.CompressFormat.JPEG, 88, output)) error("No se pudo codificar la imagen")
            }
            val updated = _playlists.value.map {
                if (it.id == id) it.copy(coverFileName = destination.name, updatedAt = System.currentTimeMillis()) else it
            }
            savePlaylists(updated)
            _playlists.value = updated
            old.coverFileName?.let { File(playlistCoverDirectory, it).delete() }
            error.value = null
            true
        } catch (exception: Exception) {
            destination.delete()
            error.value = "No se pudo guardar la portada: ${exception.localizedMessage}"
            false
        } finally { bitmap?.recycle() }
    }
    @Synchronized fun setPlaylistSongs(id: String, songIds: List<String>) {
        val valid = songIds.distinct().filter { selected -> _songs.value.any { it.id == selected } }
        updatePlaylists(_playlists.value.map { if (it.id == id) it.copy(songIds = valid,
            updatedAt = System.currentTimeMillis()) else it })
    }
    private fun updatePlaylists(updated: List<Playlist>) {
        try { savePlaylists(updated); _playlists.value = updated }
        catch (exception: Exception) { error.value = "No se pudo guardar la lista: ${exception.localizedMessage}" }
    }

    private fun displayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0) ?: "Audio"
        }
        return uri.lastPathSegment ?: "Audio"
    }

    private fun readSongs(): List<Song> = try {
        if (!catalog.exists()) emptyList() else {
            val array = JSONArray(catalog.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val song = array.getJSONObject(i).toSong()
                    val privateFileExists = fileFor(song).isFile
                    add(song.copy(
                        mediaAvailable = restoredMediaAvailability(song, privateFileExists),
                        importSourceIdentity = song.importSourceIdentity.takeIf { song.contentUri == null && privateFileExists }
                    ))
                }
            }
        }
    } catch (exception: Exception) {
        catalogReadFailed = true
        error.value = "No se pudo leer la biblioteca: ${exception.localizedMessage}"
        emptyList()
    }

    private fun readPlaylists(): List<Playlist> = try {
        if (!playlistCatalog.exists()) emptyList() else {
            val array = JSONArray(playlistCatalog.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val ids = item.optJSONArray("songIds") ?: JSONArray()
                    add(Playlist(item.getString("id"), item.getString("title"), item.optString("description"),
                        (0 until ids.length()).map { ids.getString(it) }, item.stringOrNull("coverFileName"),
                        item.optLong("createdAt"), item.optLong("updatedAt")))
                }
            }
        }
    } catch (exception: Exception) {
        playlistReadFailed = true
        error.value = "No se pudieron leer las listas: ${exception.localizedMessage}"
        emptyList()
    }

    private fun saveSongs(songs: List<Song>) {
        if (catalogReadFailed) error("El catálogo original no se pudo leer")
        val array = JSONArray()
        songs.forEach { song -> array.put(song.toJsonObject()) }
        atomicWrite(catalog, array.toString())
    }
    private fun savePlaylists(playlists: List<Playlist>) {
        if (playlistReadFailed) error("Las listas originales no se pudieron leer")
        val array = JSONArray()
        playlists.forEach { playlist ->
            array.put(JSONObject().put("id", playlist.id).put("title", playlist.title)
                .put("description", playlist.description).put("songIds", JSONArray(playlist.songIds))
                .put("coverFileName", playlist.coverFileName)
                .put("createdAt", playlist.createdAt).put("updatedAt", playlist.updatedAt))
        }
        atomicWrite(playlistCatalog, array.toString())
    }
    private fun atomicWrite(file: File, text: String) {
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(text)
        if (!temporary.renameTo(file)) { temporary.delete(); error("No se pudo guardar ${file.name}") }
    }
}

internal fun songFolderName(song: Song, privateFolderName: String?): String =
    if (song.contentUri != null) {
        song.sourceFolder?.trimEnd('/')?.substringAfterLast('/')?.takeIf(String::isNotBlank)
            ?: "Carpeta local"
    } else {
        privateFolderName?.takeIf(String::isNotBlank) ?: "Carpeta local"
    }

internal fun reconcileMediaStoreSnapshot(existing: List<Song>, visible: List<Song>): List<Song> {
    val refreshed = visible.associateBy { it.id }.toMutableMap()
    return existing.map { previous ->
        if (previous.contentUri == null) previous
        else refreshed.remove(previous.id)?.let { current -> current.copy(
            favorite = previous.favorite,
            favoritedAt = previous.favoritedAt,
            importedAt = previous.importedAt,
            genre = current.genre ?: previous.genre,
            artworkFileName = current.artworkFileName ?: previous.artworkFileName,
            metadataScanned = previous.metadataScanned,
            mediaAvailable = true
        ) } ?: previous.copy(mediaAvailable = false)
    } + refreshed.values
}

internal data class SongDeduplication(val songs: List<Song>, val idRemap: Map<String, String>)

/** Consolidates only records with an exact, stable source identity; titles and metadata are never keys. */
internal fun consolidateExactDuplicateSongs(songs: List<Song>): SongDeduplication {
    val groups = linkedMapOf<String, MutableList<Song>>()
    songs.forEach { song -> exactSongSourceKey(song)?.let { key -> groups.getOrPut(key) { mutableListOf() }.add(song) } }
    val remap = mutableMapOf<String, String>()
    val consumed = mutableSetOf<String>()
    val output = mutableListOf<Song>()
    songs.forEach { song ->
        if (song.id in consumed) return@forEach
        val key = exactSongSourceKey(song)
        val matches = key?.let { groups[it] }.orEmpty()
        if (matches.size < 2) {
            output += song
            return@forEach
        }
        val canonical = matches.first()
        val preferred = matches.firstOrNull { it.mediaAvailable } ?: canonical
        matches.forEach { duplicate ->
            consumed += duplicate.id
            if (duplicate.id != canonical.id) remap[duplicate.id] = canonical.id
        }
        output += canonical.copy(
            fileName = preferred.fileName,
            title = preferred.title,
            album = matches.firstNotNullOfOrNull { it.album?.takeIf(String::isNotBlank) },
            artist = matches.firstNotNullOfOrNull { it.artist?.takeIf(String::isNotBlank) },
            albumArtist = matches.firstNotNullOfOrNull { it.albumArtist?.takeIf(String::isNotBlank) },
            mediaAlbumId = matches.firstNotNullOfOrNull { it.mediaAlbumId },
            mediaStoreVolume = matches.firstNotNullOfOrNull { it.mediaStoreVolume },
            genre = matches.firstNotNullOfOrNull { it.genre?.takeIf(String::isNotBlank) },
            durationMs = matches.firstNotNullOfOrNull { it.durationMs },
            track = matches.firstNotNullOfOrNull { it.track },
            artworkFileName = matches.firstNotNullOfOrNull { it.artworkFileName },
            favorite = matches.any { it.favorite },
            favoritedAt = matches.mapNotNull { it.favoritedAt }.maxOrNull(),
            mediaAvailable = matches.any { it.mediaAvailable },
            metadataScanned = matches.all { it.metadataScanned },
            albumArtistScanned = matches.all { it.albumArtistScanned }
        )
    }
    return SongDeduplication(output, remap)
}

private fun exactSongSourceKey(song: Song): String? = when {
    song.importSourceIdentity != null && song.privateAudioFingerprint != null ->
        "import\u001f${song.importSourceIdentity}\u001f${song.privateAudioFingerprint}"
    song.contentUri != null -> "media\u001f${song.mediaStoreVolume ?: "external_primary"}\u001f${song.contentUri}"
    else -> null
}

internal fun remapPlaylistSongIds(playlists: List<Playlist>, idRemap: Map<String, String>): List<Playlist> =
    if (idRemap.isEmpty()) playlists else playlists.map { playlist ->
        playlist.copy(songIds = playlist.songIds.map { id -> idRemap[id] ?: id })
    }

internal fun albumDisplayArtist(members: List<Song>): String =
    members.firstNotNullOfOrNull { it.albumArtist?.takeIf(String::isNotBlank) }
        ?: members.mapNotNull { it.artist?.takeIf(String::isNotBlank) }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        ?: "Artista desconocido"

internal fun markMediaStoreSnapshotUnavailable(songs: List<Song>): List<Song> =
    songs.map { if (it.contentUri != null) it.copy(mediaAvailable = false) else it }

internal fun restoredMediaAvailability(song: Song, privateAudioExists: Boolean): Boolean =
    song.contentUri == null && privateAudioExists

internal fun canonicalMediaStoreAudioUri(uri: String, documentId: String? = null): String? {
    val rawId = if (documentId != null) {
        val type = documentId.substringBefore(':')
        if (!type.startsWith("audio")) return null
        documentId.substringAfter(':', "")
    } else {
        if (!uri.startsWith("content://media/") || "/audio/media/" !in uri) return null
        uri.substringAfterLast('/')
    }
    val id = rawId.toLongOrNull()?.takeIf { it >= 0 } ?: return null
    return "content://media/external/audio/media/$id"
}

internal fun mediaStoreIdentity(uri: String, displayName: String, sizeBytes: Long, dateAddedMs: Long): String =
    "$uri|$displayName|$sizeBytes|$dateAddedMs"

internal fun Song.matchesMediaStoreIdentity(identity: String): Boolean =
    importSourceIdentity == identity || contentUri?.let {
        mediaStoreIdentity(it, fileName, sizeBytes, importedAt) == identity
    } == true

internal fun isDeviceScanCoolingDown(lastScanAtMs: Long, nowMs: Long, cooldownMs: Long): Boolean =
    lastScanAtMs != 0L && nowMs - lastScanAtMs < cooldownMs

internal fun confirmedSongRelink(reference: Song, imported: Song): Song = imported.copy(
    id = reference.id,
    favorite = reference.favorite,
    favoritedAt = reference.favoritedAt,
    importedAt = reference.importedAt,
    privateAudioFingerprint = reference.privateAudioFingerprint,
    relinkDismissed = false
)

private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte ->
    (byte.toInt() and 0xff).toString(16).padStart(2, '0')
}

fun Song.albumKey(): String? {
    val albumName = album.key() ?: return null
    val mediaId = mediaAlbumId?.takeIf { it >= 0 }
    if (mediaId != null) return "media-album\u001f${mediaStoreVolume ?: "external_primary"}\u001f$mediaId"
    return "$albumName\u001f${(albumArtist ?: artist).key().orEmpty()}"
}
fun String?.key(): String? = this?.trim()?.takeIf(String::isNotEmpty)?.let {
    Normalizer.normalize(it, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()
}
private fun JSONObject.stringOrNull(key: String): String? = if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
private fun JSONObject.longOrNull(key: String): Long? = if (!has(key) || isNull(key)) null else optLong(key)
private fun JSONObject.intOrNull(key: String): Int? = if (!has(key) || isNull(key)) null else optInt(key)
private fun JSONObject.toSong(): Song = Song(
    id = getString("id"), fileName = getString("fileName"), title = getString("title"),
    favorite = optBoolean("favorite"), favoritedAt = longOrNull("favoritedAt"),
    importedAt = optLong("importedAt", 0).takeIf { it > 0 } ?: System.currentTimeMillis(),
    durationMs = longOrNull("durationMs"), album = stringOrNull("album"), artist = stringOrNull("artist"),
    genre = stringOrNull("genre"), track = intOrNull("track"), artworkFileName = stringOrNull("artworkFileName"),
    sourceFolder = stringOrNull("sourceFolder"), metadataScanned = optBoolean("metadataScanned"),
    contentUri = stringOrNull("contentUri"), sizeBytes = optLong("sizeBytes"),
    mediaAvailable = optBoolean("mediaAvailable", true), importSourceIdentity = stringOrNull("importSourceIdentity"),
    privateAudioFingerprint = stringOrNull("privateAudioFingerprint"), relinkDismissed = optBoolean("relinkDismissed"),
    albumArtist = stringOrNull("albumArtist"), mediaAlbumId = longOrNull("mediaAlbumId"),
    mediaStoreVolume = stringOrNull("mediaStoreVolume"), albumArtistScanned = optBoolean("albumArtistScanned")
)
private fun Song.toJsonObject(): JSONObject = JSONObject().put("id", id).put("fileName", fileName).put("title", title)
    .put("favorite", favorite).put("favoritedAt", favoritedAt).put("importedAt", importedAt)
    .put("durationMs", durationMs).put("album", album).put("artist", artist).put("genre", genre)
    .put("track", track).put("artworkFileName", artworkFileName).put("sourceFolder", sourceFolder)
    .put("metadataScanned", metadataScanned).put("contentUri", contentUri).put("sizeBytes", sizeBytes)
    .put("mediaAvailable", mediaAvailable).put("importSourceIdentity", importSourceIdentity)
    .put("privateAudioFingerprint", privateAudioFingerprint).put("relinkDismissed", relinkDismissed)
    .put("albumArtist", albumArtist).put("mediaAlbumId", mediaAlbumId)
    .put("mediaStoreVolume", mediaStoreVolume).put("albumArtistScanned", albumArtistScanned)
