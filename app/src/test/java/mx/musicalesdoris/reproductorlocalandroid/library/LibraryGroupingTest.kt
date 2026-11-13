package mx.musicalesdoris.reproductorlocalandroid.library

import mx.musicalesdoris.reproductorlocalandroid.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotSame
import org.junit.Test
import java.io.File
import kotlinx.coroutines.runBlocking

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

    @Test fun genresNormalizeCaseWhitespaceAndIgnoreMissingValues() {
        val songs = listOf(
            Song("1", "1.mp3", "One", genre = "  Drum   & Bass "),
            Song("2", "2.mp3", "Two", genre = "drum & bass"),
            Song("3", "3.mp3", "Three", genre = " "),
            Song("4", "4.mp3", "Four", genre = null)
        )
        val genres = LibraryGrouping.genres(songs)
        assertEquals(1, genres.size)
        assertEquals("Drum & Bass", genres.single().name)
        assertEquals(2, genres.single().songCount)
        assertEquals(listOf("1", "2"), genres.single().songIds)
    }

    @Test fun foldersGroupOnlyExistingFilesByActualPrivateParent() {
        val root = java.nio.file.Files.createTempDirectory("soniq-folders-test").toFile()
        try {
            val firstDir = File(root, "Songs").apply { mkdirs() }
            val otherDir = File(root, "Imported").apply { mkdirs() }
            val songs = listOf(
                Song("1", "1.mp3", "One"), Song("2", "2.mp3", "Two"),
                Song("3", "3.mp3", "Three"), Song("4", "4.mp3", "Gone")
            )
            File(firstDir, "1.mp3").writeBytes(byteArrayOf(1))
            File(firstDir, "2.mp3").writeBytes(byteArrayOf(1))
            File(otherDir, "3.mp3").writeBytes(byteArrayOf(1))
            val groups = LibraryGrouping.folders(songs) { song ->
                File(when (song.id) { "1", "2" -> firstDir; "3" -> otherDir; else -> firstDir }, song.fileName)
            }
            assertEquals(2, groups.size)
            assertEquals(2, groups.single { it.name == "Songs" }.songCount)
            assertEquals(listOf("1", "2"), groups.single { it.name == "Songs" }.songIds)
        } finally { root.deleteRecursively() }
    }

    @Test fun folderFilesystemGroupingRunsAwayFromTheCallingThread() = runBlocking {
        val root = java.nio.file.Files.createTempDirectory("soniq-folder-dispatcher-test").toFile()
        try {
            val file = File(root, "Songs/one.mp3").apply {
                parentFile?.mkdirs()
                writeBytes(byteArrayOf(1))
            }
            val callerThread = Thread.currentThread()
            var filesystemThread: Thread? = null

            val groups = groupFoldersOnIo(listOf(Song("1", "one.mp3", "One"))) {
                filesystemThread = Thread.currentThread()
                file
            }

            assertNotSame(callerThread, filesystemThread)
            assertEquals(1, groups.size)
            assertEquals("Songs", groups.single().name)
        } finally {
            root.deleteRecursively()
        }
    }
}
