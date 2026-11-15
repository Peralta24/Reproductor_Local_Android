package mx.musicalesdoris.reproductorlocalandroid.library.sections

import mx.musicalesdoris.reproductorlocalandroid.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongLibraryStatsTest {
    @Test fun statsAggregateBytesAndKnownDurations() {
        val songs = listOf(
            Song(id = "a", fileName = "a.mp3", title = "A", durationMs = 90_000),
            Song(id = "b", fileName = "b.flac", title = "B", durationMs = 150_000)
        )

        val stats = calculateSongLibraryStats(songs) { if (it.id == "a") 100L else 250L }

        assertEquals(350L, stats.totalBytes)
        assertTrue(stats.allDurationsKnown)
        assertEquals(4L, stats.totalMinutes)
    }

    @Test fun unknownDurationAndEmptyCatalogKeepExistingSemantics() {
        val song = Song(id = "a", fileName = "a.mp3", title = "A", durationMs = null)
        val unknown = calculateSongLibraryStats(listOf(song)) { 12L }
        val empty = calculateSongLibraryStats(emptyList()) { error("No size should be requested") }

        assertFalse(unknown.allDurationsKnown)
        assertEquals(0L, unknown.totalMinutes)
        assertEquals(0L, empty.totalBytes)
        assertTrue(empty.allDurationsKnown)
    }

    @Test fun largeCatalogIsAggregatedWithoutChangingCounts() {
        val songs = List(7_500) { index ->
            Song(id = "song-$index", fileName = "$index.mp3", title = "Song $index", durationMs = 60_000)
        }

        val stats = calculateSongLibraryStats(songs) { 1_024L }

        assertEquals(7_500L * 1_024L, stats.totalBytes)
        assertTrue(stats.allDurationsKnown)
        assertEquals(7_500L, stats.totalMinutes)
    }
}
