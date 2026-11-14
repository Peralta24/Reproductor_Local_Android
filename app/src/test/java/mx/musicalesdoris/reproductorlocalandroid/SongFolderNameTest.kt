package mx.musicalesdoris.reproductorlocalandroid

import org.junit.Assert.assertEquals
import org.junit.Test

class SongFolderNameTest {
    @Test
    fun mediaStoreRowsUseTheIndexedSourceFolder() {
        val song = Song(
            id = "media:42",
            fileName = "track.flac",
            title = "Track",
            sourceFolder = "Music/Albums/",
            contentUri = "content://media/external/audio/media/42"
        )

        assertEquals("Albums", songFolderName(song, "Songs"))
    }

    @Test
    fun importedRowsUseTheirPrivateStorageFolder() {
        val song = Song(id = "private", fileName = "track.mp3", title = "Track")

        assertEquals("Songs", songFolderName(song, "Songs"))
    }

    @Test
    fun missingFolderMetadataUsesSafeFallback() {
        val mediaStoreSong = Song(
            id = "media:42", fileName = "track.mp3", title = "Track",
            contentUri = "content://media/external/audio/media/42"
        )

        assertEquals("Carpeta local", songFolderName(mediaStoreSong, "Songs"))
        assertEquals("Carpeta local", songFolderName(Song("id", "x", "x"), null))
    }
}
