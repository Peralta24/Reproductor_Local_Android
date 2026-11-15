package mx.musicalesdoris.reproductorlocalandroid.library.components.artwork

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.albumKey

internal data class ArtworkIndexSnapshot(val catalog: List<Song>, val byAlbum: Map<String, String>)

/** Builds the same last-entry-wins map as the previous mapNotNull(...).toMap() implementation. */
internal suspend fun buildArtworkIndex(catalog: List<Song>): ArtworkIndexSnapshot {
    val byAlbum = LinkedHashMap<String, String>()
    for ((index, song) in catalog.withIndex()) {
        if (index % 128 == 0) currentCoroutineContext().ensureActive()
        val key = song.albumKey()
        val artwork = song.artworkFileName
        if (key != null && artwork != null) byAlbum[key] = artwork
    }
    currentCoroutineContext().ensureActive()
    return ArtworkIndexSnapshot(catalog, byAlbum)
}

/** Publishes only an index for the currently installed catalog and exposes completion to Compose. */
internal class ArtworkIndexCache {
    private val lock = Any()
    @Volatile private var current: ArtworkIndexSnapshot? = null
    private val _revision = MutableStateFlow(0L)
    val revision = _revision.asStateFlow()

    fun invalidateFor(catalog: List<Song>) {
        synchronized(lock) {
            if (current?.catalog !== catalog) current = null
        }
    }

    fun artworkName(song: Song, catalog: List<Song>): String? {
        song.artworkFileName?.let { return it }
        val index = current?.takeIf { it.catalog === catalog } ?: return null
        return song.albumKey()?.let(index.byAlbum::get)
    }

    fun publishIfCurrent(index: ArtworkIndexSnapshot, currentCatalog: () -> List<Song>): Boolean =
        synchronized(lock) {
            if (index.catalog !== currentCatalog()) return@synchronized false
            current = index
            _revision.value += 1
            true
        }
}
