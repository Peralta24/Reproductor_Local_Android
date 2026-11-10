package mx.musicalesdoris.reproductorlocalandroid

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Song(val id: String, val fileName: String, val title: String, val favorite: Boolean = false)

class SongLibrary(private val context: Context) {
    private val directory = File(context.filesDir, "Songs").apply { mkdirs() }
    private val catalog = File(context.filesDir, "songs.json")
    val error = MutableStateFlow<String?>(null)
    private val _songs = MutableStateFlow(readSongs())
    val songs = _songs.asStateFlow()

    fun fileFor(song: Song) = File(directory, song.fileName)

    fun importSongs(uris: List<Uri>): List<Song> {
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
                val song = Song(UUID.randomUUID().toString(), fileName, name.substringBeforeLast('.', name))
                val updated = _songs.value + song
                save(updated)
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

    fun toggleFavorite(id: String) {
        val updated = _songs.value.map { if (it.id == id) it.copy(favorite = !it.favorite) else it }
        try { save(updated); _songs.value = updated }
        catch (exception: Exception) { error.value = "No se pudo guardar el favorito: ${exception.localizedMessage}" }
    }

    fun delete(song: Song): Boolean {
        val previous = _songs.value
        val updated = previous.filterNot { it.id == song.id }
        return try {
            save(updated)
            if (!fileFor(song).delete()) {
                save(previous)
                error("No se pudo eliminar el archivo")
            }
            _songs.value = updated
            true
        } catch (exception: Exception) {
            error.value = "No se pudo eliminar ${song.title}: ${exception.localizedMessage}"
            false
        }
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
                    val song = Song(item.getString("id"), item.getString("fileName"), item.getString("title"), item.optBoolean("favorite"))
                    if (fileFor(song).exists()) add(song)
                }
            }
        }
    } catch (exception: Exception) {
        error.value = "No se pudo leer la biblioteca: ${exception.localizedMessage}"
        emptyList()
    }

    private fun save(songs: List<Song>) {
        val array = JSONArray()
        songs.forEach { song ->
            array.put(JSONObject().put("id", song.id).put("fileName", song.fileName)
                .put("title", song.title).put("favorite", song.favorite))
        }
        val temporary = File(context.filesDir, "songs.json.tmp")
        temporary.writeText(array.toString())
        if (!temporary.renameTo(catalog)) {
            temporary.delete()
            error("No se pudo guardar la biblioteca")
        }
    }
}
