package mx.musicalesdoris.reproductorlocalandroid.library

import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.playlists.buildPlaylistPlaybackQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistPlaybackQueueTest {
    private fun song(id: String) = Song(id = id, fileName = "$id.mp3", title = id)
    private val playlist = Playlist("p", "List", listOf("c", "missing", "a", "b"), 1L, 1L)

    @Test fun queueFollowsPlaylistOrderAndOmitsUnavailableOrMissingTracks() {
        val catalog = listOf(song("a"), song("b"), song("c"), song("outside"))
        val queue = buildPlaylistPlaybackQueue(playlist, catalog, { it.id != "b" })
        assertEquals(listOf("c", "a"), queue.songs.map(Song::id))
        assertEquals(0, queue.startIndex)
        assertTrue(queue.songs.none { it.id == "outside" })
    }

    @Test fun selectedTrackStartsAtItsPlaylistPositionForFollowingNavigation() {
        val catalog = listOf(song("a"), song("b"), song("c"))
        val queue = buildPlaylistPlaybackQueue(playlist, catalog, { true }, startingSongId = "a")
        assertEquals(listOf("c", "a", "b"), queue.songs.map(Song::id))
        assertEquals(1, queue.startIndex)
        assertEquals("b", queue.songs[queue.startIndex + 1].id)
    }

    @Test fun absentStartingTrackFallsBackToFirstAvailableEntry() {
        val queue = buildPlaylistPlaybackQueue(playlist, listOf(song("b"), song("c")), { true }, "unknown")
        assertEquals(0, queue.startIndex)
        assertEquals("c", queue.songs[queue.startIndex].id)
    }
}
