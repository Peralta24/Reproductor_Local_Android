package mx.musicalesdoris.reproductorlocalandroid.library

import java.util.Locale
import java.io.File
import mx.musicalesdoris.reproductorlocalandroid.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Pure grouping rules shared by album and artist library sections. */
internal object LibraryGrouping {
    fun genres(songs: List<Song>): List<GenreSummary> = songs
        .mapNotNull { song -> song.genre.displayName()?.let { name -> name.normalizedName()?.let { it to Pair(name, song) } } }
        .groupBy({ it.first }, { it.second })
        .map { (id, entries) ->
            val members = entries.map { it.second }
            GenreSummary(id, entries.first().first, members.size, members.map(Song::id))
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    /** Uses only the actual private import destination; original SAF paths are not retained. */
    fun folders(songs: List<Song>, fileFor: (Song) -> File): List<FolderSummary> = songs
        .mapNotNull { song -> runCatching {
            val file = fileFor(song)
            if (!file.isFile) return@runCatching null
            val directory = file.parentFile ?: return@runCatching null
            directory.canonicalPath to Pair(directory.name.ifBlank { "Almacenamiento local" }, song)
        }.getOrNull() }
        .groupBy({ it.first }, { it.second })
        .map { (id, entries) ->
            val members = entries.map { it.second }
            FolderSummary(id, entries.first().first, members.size, members.map(Song::id))
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    fun albums(songs: List<Song>): List<AlbumSummary> = songs
        .filter { !it.album.normalizedName().isNullOrEmpty() }
        .groupBy { song ->
            val album = requireNotNull(song.album.normalizedName())
            val artist = song.albumGroupingCredit().normalizedName().orEmpty()
            "$album|$artist"
        }
        .map { (key, group) ->
            val representative = group.first()
            AlbumSummary(
                id = key,
                title = requireNotNull(representative.album.displayName()),
                artist = group.firstNotNullOfOrNull { it.albumGroupingCredit() },
                songCount = group.size,
                artworkPath = group.firstNotNullOfOrNull { it.artworkPath?.takeIf(String::isNotBlank) },
                songIds = group.map { it.id }
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

    fun artists(songs: List<Song>): List<ArtistSummary> = songs
        .filter { !it.artistGroupingCredit().normalizedName().isNullOrEmpty() }
        .groupBy { requireNotNull(it.artistGroupingCredit().normalizedName()) }
        .map { (key, group) ->
            ArtistSummary(
                id = key,
                name = requireNotNull(group.firstNotNullOfOrNull { it.artistGroupingCredit() }),
                albumCount = group.mapNotNull { song ->
                    song.album.normalizedName()?.let { album ->
                        "$album|${song.albumGroupingCredit().normalizedName().orEmpty()}"
                    }
                }.distinct().size,
                songCount = group.size,
                songIds = group.map { it.id }
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    private fun String?.normalizedName(): String? = this
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.lowercase(Locale.ROOT)
        ?.takeIf(String::isNotEmpty)

    private fun String?.displayName(): String? = this
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.takeIf(String::isNotEmpty)

    private fun Song.albumGroupingCredit(): String? = albumArtist.displayName() ?: artist.displayName()

    private fun Song.artistGroupingCredit(): String? = artist.displayName() ?: albumArtist.displayName()
}

/** Performs filesystem-backed folder grouping away from the UI dispatcher. */
internal suspend fun groupFoldersOnIo(songs: List<Song>, fileFor: (Song) -> File): List<FolderSummary> =
    withContext(Dispatchers.IO) { LibraryGrouping.folders(songs, fileFor) }
