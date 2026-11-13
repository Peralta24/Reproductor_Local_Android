package mx.musicalesdoris.reproductorlocalandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistShufflePolicyTest {
    @Test fun repeatedButtonPressesAlternateShuffleWithoutReplacingTheActiveQueue() {
        val originalQueue = listOf("a", "b", "c")
        val expectedModes = listOf(true, false, true)
        var currentMode = false

        expectedModes.forEach { expectedMode ->
            val decision = decidePlaylistShuffleToggle(
                playlistQueueIds = originalQueue,
                activeQueueIds = originalQueue,
                selectedSongId = "b",
                currentShuffleEnabled = currentMode
            )
            assertFalse(decision.shouldStartPlaylist)
            assertEquals(expectedMode, decision.shuffleEnabled)
            currentMode = decision.shuffleEnabled
        }
    }

    @Test fun activePlaylistStartedAtAnotherTrackIsRecognizedAsTheSameQueue() {
        val decision = decidePlaylistShuffleToggle(
            playlistQueueIds = listOf("a", "b", "c"),
            activeQueueIds = listOf("b", "c", "a"),
            selectedSongId = "b",
            currentShuffleEnabled = true
        )

        assertFalse(decision.shouldStartPlaylist)
        assertFalse(decision.shuffleEnabled)
    }

    @Test fun aDifferentQueueStartsTheRequestedPlaylistInShuffleMode() {
        val decision = decidePlaylistShuffleToggle(
            playlistQueueIds = listOf("a", "b", "c"),
            activeQueueIds = listOf("a", "c", "b"),
            selectedSongId = "a",
            currentShuffleEnabled = false
        )

        assertTrue(decision.shouldStartPlaylist)
        assertTrue(decision.shuffleEnabled)
    }

    @Test fun emptyAndSingleSongQueuesAreHandledWithoutInventingQueueEntries() {
        val empty = decidePlaylistShuffleToggle(emptyList(), emptyList(), null, false)
        assertTrue(empty.shouldStartPlaylist)
        assertTrue(empty.shuffleEnabled)

        val single = decidePlaylistShuffleToggle(listOf("only"), listOf("only"), "only", false)
        assertFalse(single.shouldStartPlaylist)
        assertTrue(single.shuffleEnabled)
    }

    @Test fun queueWithSameTracksButDifferentNonRotatedOrderIsNotTreatedAsCurrentPlaylist() {
        val decision = decidePlaylistShuffleToggle(
            playlistQueueIds = listOf("a", "b", "c", "d"),
            activeQueueIds = listOf("a", "c", "b", "d"),
            selectedSongId = "a",
            currentShuffleEnabled = true
        )

        assertTrue(decision.shouldStartPlaylist)
        assertTrue(decision.shuffleEnabled)
    }
}
