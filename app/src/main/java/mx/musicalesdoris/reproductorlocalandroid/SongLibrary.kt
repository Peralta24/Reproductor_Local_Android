package mx.musicalesdoris.reproductorlocalandroid

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Normalizer
import java.util.UUID
import mx.musicalesdoris.reproductorlocalandroid.library.AlbumSummary
import mx.musicalesdoris.reproductorlocalandroid.library.ArtistSummary
import mx.musicalesdoris.reproductorlocalandroid.library.GenreSummary
import mx.musicalesdoris.reproductorlocalandroid.library.FolderSummary
import mx.musicalesdoris.reproductorlocalandroid.library.PlaylistSummary

data class Song(
    val id: String, val fileName: String, val title: String, val favorite: Boolean = false,
    val favoritedAt: Long? = null, val importedAt: Long = System.currentTimeMillis(),
    val durationMs: Long? = null, val album: String? = null, val artist: String? = null,
    val genre: String? = null, val track: Int? = null, val artworkFileName: String? = null,
    val sourceFolder: String? = null, val metadataScanned: Boolean = false
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

class SongLibrary(private val context: Context) {
    private val directory = File(context.filesDir, "Songs").apply { mkdirs() }
    private val artworkDirectory = File(context.filesDir, "AlbumArtwork").apply { mkdirs() }
    private val playlistCoverDirectory = File(context.filesDir, "PlaylistCovers").apply { mkdirs() }
    private val catalog = File(context.filesDir, "songs.json")
    private val playlistCatalog = File(context.filesDir, "playlists.json")
    private var catalogReadFailed = false
    private var playlistReadFailed = false
    val error = MutableStateFlow<String?>(null)
    private val _songs = MutableStateFlow(readSongs())
    val songs = _songs.asStateFlow()
    private val _playlists = MutableStateFlow(readPlaylists())
    val playlists = _playlists.asStateFlow()
    init {
        CoroutineScope(Dispatchers.IO).launch {
            val original = _songs.value
            if (original.none { !it.metadataScanned }) return@launch
            val updated = original.map { song ->
                if (song.metadataScanned) song else enrich(song)
            }
            try {
                synchronized(this@SongLibrary) {
                    val byId = updated.associateBy { it.id }
                    val merged = _songs.value.map { current ->
                        byId[current.id]?.takeIf { !current.metadataScanned } ?: current
                    }
                    saveSongs(merged)
                    _songs.value = merged
                }
            }
            catch (exception: Exception) { error.value = "No se pudieron actualizar los metadatos: ${exception.localizedMessage}" }
        }
    }
    private fun enrich(song: Song): Song {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(fileFor(song).absolutePath)
            fun field(key: Int) = retriever.extractMetadata(key)?.trim()?.takeIf(String::isNotEmpty)
            val artName = retriever.embeddedPicture?.let { bytes ->
                val name = "${song.id}.jpg"
                File(artworkDirectory, name).writeBytes(bytes)
                name
            }
            song.copy(album = song.album ?: field(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                artist = song.artist ?: field(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                genre = song.genre ?: field(MediaMetadataRetriever.METADATA_KEY_GENRE),
                durationMs = song.durationMs ?: field(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                track = song.track ?: field(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.substringBefore('/')?.toIntOrNull(),
                artworkFileName = song.artworkFileName ?: artName, metadataScanned = true)
        } catch (_: Exception) { song.copy(metadataScanned = true) }
        finally { retriever.release() }
    }

    fun fileFor(song: Song) = File(directory, song.fileName)
    fun albums(): List<AlbumSummary> = _songs.value.filter { it.albumKey() != null }
        .groupBy { it.albumKey()!! }.map { (key, members) ->
            AlbumSummary(key, members.first().album ?: "", members.first().artist ?: "Artista desconocido",
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
        val name = song.artworkFileName ?: _songs.value.firstOrNull {
            it.albumKey() != null && it.albumKey() == song.albumKey() && it.artworkFileName != null
        }?.artworkFileName
        return name?.let { File(artworkDirectory, it) }?.takeIf(File::exists)
    }
    fun coverFor(playlist: Playlist): File? = playlist.coverFileName?.let { File(playlistCoverDirectory, it) }?.takeIf(File::exists)
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
            val extension = name.substringAfterLast('.', "").takeIf { it != name }?.let { ".$it" } ?: ""
            val fileName = UUID.randomUUID().toString() + extension
            val destination = File(directory, fileName)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                } ?: error("No se pudo abrir el archivo")
                val song = readMetadata(destination, name, uri)
                val updated = _songs.value + song
                saveSongs(updated)
                _songs.value = updated
                added += song
            } catch (exception: Exception) {
                destination.delete()
                failures += name
            }
        }
        error.value = failures.takeIf { it.isNotEmpty() }?.joinToString(prefix = "No se pudieron importar: ")
        return added
    }

    private fun readMetadata(file: File, name: String, uri: Uri): Song {
        val retriever = MediaMetadataRetriever()
        val id = UUID.randomUUID().toString()
        var title = name.substringBeforeLast('.', name)
        var album: String? = null
        var artist: String? = null
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
            sourceFolder = uri.path?.substringBeforeLast('/')?.takeIf { it.isNotBlank() }, metadataScanned = true)
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
            if (!fileFor(song).delete()) error("No se pudo eliminar el archivo")
            song.artworkFileName?.let { File(artworkDirectory, it).delete() }
            _songs.value = updated
            _playlists.value = updatedPlaylists
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
    @Synchronized fun setPlaylistCover(id: String, uri: Uri) {
        val old = _playlists.value.firstOrNull { it.id == id } ?: return
        val destination = File(playlistCoverDirectory, "$id.jpg")
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: error("No se pudo abrir la imagen")
            val sample = (maxOf(bounds.outWidth, bounds.outHeight) / 1200).coerceAtLeast(1)
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("No se pudo leer la imagen")
            destination.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output) }
            bitmap.recycle()
            updatePlaylists(_playlists.value.map {
                if (it.id == id) it.copy(coverFileName = destination.name, updatedAt = System.currentTimeMillis()) else it
            })
            if (old.coverFileName != destination.name) old.coverFileName?.let { File(playlistCoverDirectory, it).delete() }
        } catch (exception: Exception) { error.value = "No se pudo guardar la portada: ${exception.localizedMessage}" }
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
                    val item = array.getJSONObject(i)
                    val song = Song(item.getString("id"), item.getString("fileName"), item.getString("title"),
                        item.optBoolean("favorite"), item.longOrNull("favoritedAt"),
                        item.optLong("importedAt", 0).takeIf { it > 0 } ?: System.currentTimeMillis(),
                        item.longOrNull("durationMs"), item.stringOrNull("album"), item.stringOrNull("artist"),
                        item.stringOrNull("genre"), item.intOrNull("track"), item.stringOrNull("artworkFileName"),
                        item.stringOrNull("sourceFolder"), item.optBoolean("metadataScanned"))
                    if (fileFor(song).exists()) add(song)
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
        songs.forEach { song ->
            array.put(JSONObject().put("id", song.id).put("fileName", song.fileName).put("title", song.title)
                .put("favorite", song.favorite).put("favoritedAt", song.favoritedAt).put("importedAt", song.importedAt)
                .put("durationMs", song.durationMs).put("album", song.album).put("artist", song.artist)
                .put("genre", song.genre).put("track", song.track).put("artworkFileName", song.artworkFileName)
                .put("sourceFolder", song.sourceFolder).put("metadataScanned", song.metadataScanned))
        }
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

fun Song.albumKey(): String? = album.key()?.let { "$it\u001f${artist.key().orEmpty()}" }
fun String?.key(): String? = this?.trim()?.takeIf(String::isNotEmpty)?.let {
    Normalizer.normalize(it, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()
}
private fun JSONObject.stringOrNull(key: String): String? = if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
private fun JSONObject.longOrNull(key: String): Long? = if (!has(key) || isNull(key)) null else optLong(key)
private fun JSONObject.intOrNull(key: String): Int? = if (!has(key) || isNull(key)) null else optInt(key)
