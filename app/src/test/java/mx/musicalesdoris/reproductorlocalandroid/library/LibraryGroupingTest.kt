package mx.musicalesdoris.reproductorlocalandroid.library

import mx.musicalesdoris.reproductorlocalandroid.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryGroupingTest {
    @Test fun albumsNormalizeCaseAndWhitespaceAndKeepRepresentativeDisplayName() {
        val songs = listOf(
            Song("1", "1.mp3", "One", artist = "Artist", album = "  Blue   Room ", albumArtist = "  The Band "),
            Song("2", "2.mp3", "Two", artist = "Artist", album = "blue room", albumArtist = "the   band")
        )

        val albums = LibraryGrouping.albums(songs)

        assertEquals(1, albums.size)
        assertEquals("Blue Room", albums.single().title)
        assertEquals("The Band", albums.single().artist)
        assertEquals(2, albums.single().songCount)
        assertEquals(listOf("1", "2"), albums.single().songIds)
    }

    @Test fun artistGroupingNormalizesCaseWhitespaceAndUsesAlbumArtistAsFallback() {
        val songs = listOf(
            Song("1", "1.mp3", "One", artist = "  Mira   Sol ", album = "First"),
            Song("2", "2.mp3", "Two", artist = "mira sol", album = "Second"),
            Song("3", "3.mp3", "Three", albumArtist = "Guest Artist")
        )

        val artists = LibraryGrouping.artists(songs)

        assertEquals(2, artists.size)
        val mira = artists.single { it.name == "Mira Sol" }
        assertEquals(2, mira.songCount)
        assertEquals(2, mira.albumCount)
        assertEquals(listOf("1", "2"), mira.songIds)
        assertEquals("Guest Artist", artists.single { it.name == "Guest Artist" }.name)
    }

    @Test fun blankAlbumAndArtistAreNotInventedOrGrouped() {
        val songs = listOf(
            Song("1", "1.mp3", "One", album = " ", artist = "\t"),
            Song("2", "2.mp3", "Two", album = null, artist = null),
            Song("3", "3.mp3", "Three", album = "Present album", artist = "Present artist")
        )

        assertEquals(listOf("Present album"), LibraryGrouping.albums(songs).map { it.title })
        assertEquals(listOf("Present artist"), LibraryGrouping.artists(songs).map { it.name })
        assertTrue(LibraryGrouping.albums(songs).none { "1" in it.songIds || "2" in it.songIds })
        assertTrue(LibraryGrouping.artists(songs).none { "1" in it.songIds || "2" in it.songIds })
    }

    @Test fun sameAlbumTitleByDifferentAlbumArtistsRemainsDistinct() {
        val songs = listOf(
            Song("1", "1.mp3", "One", artist = "Singer A", album = "Collection", albumArtist = "Band A"),
            Song("2", "2.mp3", "Two", artist = "Singer B", album = "Collection", albumArtist = "Band B")
        )

        val albums = LibraryGrouping.albums(songs)

        assertEquals(2, albums.size)
        assertEquals(setOf("Band A", "Band B"), albums.mapNotNull { it.artist }.toSet())
    }
}
