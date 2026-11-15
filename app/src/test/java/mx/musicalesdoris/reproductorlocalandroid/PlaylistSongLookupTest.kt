package mx.musicalesdoris.reproductorlocalandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSongLookupTest {
    private fun song(id: String, title: String = id) = Song(id = id, fileName = "$id.mp3", title = title)

    @Test
    fun smallPlaylistPreservesOrderAndSkipsMissingIds() {
        val first = song("first")
        val second = song("second")

        val resolved = PlaylistSongLookupIndex().resolve(
            songIds = listOf(second.id, "missing", first.id),
            catalog = listOf(first, second)
        )

        assertEquals(listOf(second, first), resolved)
    }

    @Test
    fun largePlaylistResolvesAllSongsInPlaylistOrder() {
        val catalog = (0 until 7_500).map { song("song-$it") }
        val requestedIds = catalog.asReversed().map { it.id }

        val resolved = PlaylistSongLookupIndex().resolve(requestedIds, catalog)

        assertEquals(7_500, resolved.size)
        assertEquals(requestedIds, resolved.map { it.id })
    }

    @Test
    fun repeatedPersistedIdsRemainRepeatedAndMissingIdsAreOmitted() {
        val first = song("first")
        val second = song("second")
        val playlist = Playlist(title = "Legacy", songIds = listOf(second.id, first.id, second.id, "missing"))

        val resolved = PlaylistSongLookupIndex().resolve(playlist.songIds, listOf(first, second))

        assertEquals(listOf(second, first, second), resolved)
        assertEquals(listOf(second.id, first.id, second.id, "missing"), playlist.songIds)
    }

    @Test
    fun catalogUpdateRebuildsIndexAndReturnsUpdatedSongInstances() {
        val index = PlaylistSongLookupIndex()
        val oldSong = song("same-id", "Old title")
        val initialCatalog = listOf(oldSong)
        assertSame(oldSong, index.resolve(listOf(oldSong.id), initialCatalog).single())

        val updatedSong = song("same-id", "Updated title")
        val newSong = song("new-id")
        val updatedCatalog = listOf(updatedSong, newSong)
        val resolved = index.resolve(listOf("new-id", "same-id", "removed-id"), updatedCatalog)

        assertEquals(listOf(newSong, updatedSong), resolved)
        assertSame(updatedSong, resolved.last())
    }

    @Test
    fun duplicateCatalogIdsKeepTheFirstSongLikeThePreviousLinearLookup() {
        val first = song("duplicate", "First")
        val later = song("duplicate", "Later")

        val resolved = PlaylistSongLookupIndex().resolve(listOf("duplicate"), listOf(first, later))

        assertSame(first, resolved.single())
    }

    @Test
    fun playlistSongValidationKeepsExistingOrderAndDeduplicatesAsBefore() {
        val catalog = listOf(song("first"), song("second"))

        val valid = validatedPlaylistSongIds(
            songIds = listOf("second", "missing", "first", "second", "first"),
            catalog = catalog
        )

        assertEquals(listOf("second", "first"), valid)
    }

    @Test
    fun existingPlaylistIdsAreNotRewrittenByLookup() {
        val originalIds = listOf("media:external_primary:12", "private-uuid", "media:external_primary:12")
        val existingPlaylist = Playlist(id = "existing-id", title = "Existing", songIds = originalIds)

        PlaylistSongLookupIndex().resolve(existingPlaylist.songIds,
            listOf(song("media:external_primary:12"), song("private-uuid")))

        assertEquals("existing-id", existingPlaylist.id)
        assertEquals(originalIds, existingPlaylist.songIds)
        assertTrue(existingPlaylist.songIds.all { it.isNotBlank() })
    }
}
