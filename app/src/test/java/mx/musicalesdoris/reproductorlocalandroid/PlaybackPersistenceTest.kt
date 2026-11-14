package mx.musicalesdoris.reproductorlocalandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPersistenceTest {
    @Test
    fun restoredSnapshotKeepsSongPositionQueueOrderShuffleAndRepeatButStaysPaused() {
        val queue = listOf("media:external_primary:11", "private-2", "media:external_primary:35")
        val restored = savedPlaybackSnapshot(
            selectedSongId = queue[1], positionMs = 72_000, hasCustomQueue = true,
            serializedQueueIds = serializePlaybackQueue(queue), shuffle = true, repeat = true
        )

        val state = restoredPlayerState(restored)

        assertEquals(queue[1], state.selectedId)
        assertEquals(queue, state.queueIds)
        assertTrue(state.shuffle)
        assertTrue(state.repeat)
        assertFalse(state.isPlaying)
        assertEquals(72_000, restored.positionMs)
    }

    @Test
    fun missingSongReferenceIsRetainedAndDoesNotChooseAnotherSong() {
        val restored = savedPlaybackSnapshot(
            selectedSongId = "not-in-current-catalog", positionMs = 15_000, hasCustomQueue = false,
            serializedQueueIds = null, shuffle = false, repeat = false
        )

        assertEquals("not-in-current-catalog", restoredPlayerState(restored).selectedId)
        assertNull(restored.queueIds)
        assertFalse(restoredPlayerState(restored).isPlaying)
    }

    @Test
    fun nullAndEmptyQueuesRemainDistinctAndPositionIsNeverNegative() {
        val noCustomQueue = savedPlaybackSnapshot(null, -1, false, null, false, false)
        val emptyCustomQueue = savedPlaybackSnapshot(null, -1, true, "", false, false)

        assertNull(noCustomQueue.queueIds)
        assertEquals(emptyList<String>(), emptyCustomQueue.queueIds)
        assertEquals(0, noCustomQueue.positionMs)
        assertEquals(0, emptyCustomQueue.positionMs)
    }

    @Test
    fun restoredSeekPositionIsClampedToTheResolvedTrackDuration() {
        assertEquals(72_000, clampRestoredPosition(72_000, 240_000))
        assertEquals(240_000, clampRestoredPosition(300_000, 240_000))
        assertEquals(0, clampRestoredPosition(-1, 240_000))
    }

    @Test
    fun stableMediaStoreIdentityCanResolveToThePrivateCopyAfterCatalogReconciliation() {
        val sourceUri = "content://media/external/audio/media/41"
        val mediaStoreSong = Song("media-id", "track.mp3", "Track", importedAt = 500,
            contentUri = sourceUri, sizeBytes = 1200, mediaStoreVolume = "external_primary")
        val importedSong = Song("private-id", "private.mp3", "Track", importSourceIdentity =
            mediaStoreIdentity(sourceUri, "track.mp3", 1200, 500), privateAudioFingerprint = "verified-sha")
        val snapshot = playbackIdentitySnapshot(mediaStoreSong, 65_000)

        assertEquals("private-id", resolveSavedSongId(snapshot, listOf(importedSong)))
    }

    @Test
    fun titleAloneDoesNotResolveADeletedSong() {
        val snapshot = savedPlaybackSnapshot("old-id", 10_000, false, null, false, false)
        val unrelated = Song("new-id", "different.mp3", "Same title")

        assertNull(resolveSavedSongId(snapshot, listOf(unrelated)))
    }
}
