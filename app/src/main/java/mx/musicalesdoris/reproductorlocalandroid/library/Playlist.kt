package mx.musicalesdoris.reproductorlocalandroid.library

import mx.musicalesdoris.reproductorlocalandroid.Song

/** A playlist stores stable song IDs instead of duplicating song records. */
data class Playlist(
    val id: String,
    val title: String,
    val songIds: List<String> = emptyList(),
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val description: String? = null,
    /** App-private relative path, never an absolute device path. */
    val artworkPath: String? = null
) {
    fun songsIn(catalog: List<Song>): List<Song> {
        val songsById = catalog.associateBy(Song::id)
        return songIds.mapNotNull(songsById::get)
    }
}

data class PlaylistCatalogState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val isReadable: Boolean = true,
    val error: String? = null,
    val needsRecovery: Boolean = false,
    val recoveryMessage: String? = null
)
