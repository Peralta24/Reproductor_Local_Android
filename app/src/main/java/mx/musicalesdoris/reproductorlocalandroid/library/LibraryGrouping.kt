package mx.musicalesdoris.reproductorlocalandroid.library

import java.util.Locale
import mx.musicalesdoris.reproductorlocalandroid.Song

/** Pure grouping rules shared by album and artist library sections. */
internal object LibraryGrouping {
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
