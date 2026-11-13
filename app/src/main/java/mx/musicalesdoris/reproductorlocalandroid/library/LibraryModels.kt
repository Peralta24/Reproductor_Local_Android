package mx.musicalesdoris.reproductorlocalandroid.library

enum class LibrarySection(val title: String, val heading: String) {
    SONGS("Canciones", "Tus canciones"),
    FAVORITES("Favoritos", "Tus favoritos"),
    PLAYLISTS("Listas", "Tus listas"),
    ALBUMS("Álbumes", "Tus álbumes"),
    ARTISTS("Artistas", "Tus artistas"),
    GENRES("Géneros", "Tus géneros"),
    FOLDERS("Carpetas", "Carpetas locales")
}

data class PlaylistSummary(
    val id: String,
    val title: String,
    val songCount: Int,
    val description: String? = null,
    val artworkPath: String? = null
)
data class AlbumSummary(
    val id: String,
    val title: String,
    val artist: String?,
    val songCount: Int,
    val artworkPath: String?,
    val songIds: List<String>
)
data class ArtistSummary(
    val id: String,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val songIds: List<String>
)
data class GenreSummary(val id: String, val name: String, val songCount: Int, val songIds: List<String>)
data class FolderSummary(val id: String, val name: String, val songCount: Int, val songIds: List<String>)
data class FolderGroupingState(val isLoading: Boolean = true, val folders: List<FolderSummary> = emptyList())
