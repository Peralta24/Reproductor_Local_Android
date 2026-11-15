package mx.musicalesdoris.reproductorlocalandroid.library.sections

import org.junit.Assert.assertEquals
import org.junit.Test

class SongCollectionIndexTest {
    @Test fun positionsPreserveListOrderAndFirstDuplicatePosition() {
        val ids = listOf("song-b", "song-a", "song-b", "song-c")

        val positions = firstPlaylistSongPositions(ids)

        assertEquals(0, positions["song-b"])
        assertEquals(1, positions["song-a"])
        assertEquals(3, positions["song-c"])
        assertEquals(null, positions["missing"])
        assertEquals(0, ids.indexOf("song-b"))
    }

    @Test fun largePlaylistIndexContainsEverySong() {
        val ids = List(7_500) { "song-$it" }

        val positions = firstPlaylistSongPositions(ids)

        assertEquals(ids.size, positions.size)
        ids.forEachIndexed { index, id -> assertEquals(index, positions[id]) }
    }

    @Test fun updatedPlaylistCreatesUpdatedIndex() {
        val first = firstPlaylistSongPositions(listOf("song-a", "song-b"))
        val updated = firstPlaylistSongPositions(listOf("song-b", "song-c"))

        assertEquals(0, updated["song-b"])
        assertEquals(1, updated["song-c"])
        assertEquals(null, updated["song-a"])
        assertEquals(1, first["song-b"])
    }
}
