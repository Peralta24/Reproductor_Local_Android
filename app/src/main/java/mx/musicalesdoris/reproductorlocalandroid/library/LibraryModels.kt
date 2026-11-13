package mx.musicalesdoris.reproductorlocalandroid.library

/** Section navigation stays independent from theme selection. */
enum class LibrarySection(val title: String, val heading: String) {
    SONGS("Canciones", "Tus canciones"), FAVORITES("Favoritos", "Tus favoritos"),
    PLAYLISTS("Listas", "Tus listas"), ALBUMS("Álbumes", "Tus álbumes"),
    ARTISTS("Artistas", "Tus artistas"), GENRES("Géneros", "Tus géneros"),
    FOLDERS("Carpetas", "Tus carpetas")
}

data class PlaylistSummary(val id: String, val title: String, val songCount: Int,
                           val durationMs: Long? = null)
data class AlbumSummary(val id: String, val title: String, val artist: String,
                        val songCount: Int, val latestImportedAt: Long = 0L)
data class ArtistSummary(val id: String, val name: String, val albumCount: Int,
                         val songCount: Int = 0, val latestImportedAt: Long = 0L)
data class GenreSummary(val id: String, val name: String, val songCount: Int,
                        val latestImportedAt: Long = 0L)
data class FolderSummary(val id: String, val name: String, val path: String,
                         val songCount: Int, val latestImportedAt: Long = 0L)
