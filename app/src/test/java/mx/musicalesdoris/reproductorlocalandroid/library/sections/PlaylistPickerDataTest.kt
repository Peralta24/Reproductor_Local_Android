package mx.musicalesdoris.reproductorlocalandroid.library.sections

import mx.musicalesdoris.reproductorlocalandroid.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PlaylistPickerDataTest {
    private fun song(id: String, artist: String? = null, album: String? = null) =
        Song(id = id, fileName = "$id.mp3", title = id, artist = artist, album = album)

    @Test
    fun groupingPreservesSongsAndUsesExistingUnknownLabels() {
        val known = song("a", artist = "Zed", album = "First")
        val unknown = song("b")
        val songs = listOf(known, unknown)

        val artists = playlistPickerGroups(songs, "Artistas") { "unused" }
        val albums = playlistPickerGroups(songs, "Álbumes") { "unused" }

        assertEquals(listOf(known), artists.getValue("Zed"))
        assertEquals(listOf(unknown), artists.getValue("Artista desconocido"))
        assertEquals(listOf(known), albums.getValue("First"))
        assertEquals(listOf(unknown), albums.getValue("Álbum desconocido"))
        assertEquals(listOf("Artista desconocido", "Zed"), sortedPlaylistPickerGroupNames(artists))
        assertEquals(emptyMap<String, List<Song>>(), playlistPickerGroups(songs, null) { "unused" })
    }

    @Test
    fun catalogReplacementProducesFreshGroupsWithoutStaleSongs() {
        val oldSong = song("old", artist = "Artist")
        val replacement = song("new", artist = "Artist")

        val oldGroups = playlistPickerGroups(listOf(oldSong), "Artistas") { "unused" }
        val newGroups = playlistPickerGroups(listOf(replacement), "Artistas") { "unused" }

        assertSame(oldSong, oldGroups.getValue("Artist").single())
        assertEquals(listOf(replacement), newGroups.getValue("Artist"))
        assertEquals(listOf("Artist"), sortedPlaylistPickerGroupNames(newGroups))
    }

    @Test
    fun categoryCountsKeepExistingDistinctAndFolderSemantics() {
        val songs = listOf(
            song("1", artist = "A", album = "X"),
            song("2", artist = "A", album = "Y"),
            song("3", artist = " ", album = null)
        )

        val counts = playlistPickerCategoryCounts(songs) { if (it.id == "3") "Other" else "Main" }

        assertEquals(1, counts.getValue("Artistas"))
        assertEquals(2, counts.getValue("Álbumes"))
        assertEquals(3, counts.getValue("Canciones"))
        assertEquals(2, counts.getValue("Carpetas"))
    }

    @Test
    fun folderGroupingAndCategoryCountsUseTheSameFolderResolver() {
        val first = song("1")
        val second = song("2")
        val folderFor: (Song) -> String = { if (it === first) "Carpeta A" else "Carpeta B" }
        val songs = listOf(first, second)

        val groups = playlistPickerGroups(songs, "Carpetas", folderFor)
        val counts = playlistPickerCategoryCounts(songs, folderFor)

        assertEquals(listOf(first), groups.getValue("Carpeta A"))
        assertEquals(listOf(second), groups.getValue("Carpeta B"))
        assertEquals(2, counts.getValue("Carpetas"))
    }
}
