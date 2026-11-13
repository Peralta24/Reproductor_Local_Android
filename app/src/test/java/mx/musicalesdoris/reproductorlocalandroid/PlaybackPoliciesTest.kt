package mx.musicalesdoris.reproductorlocalandroid

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPoliciesTest {
    @Test fun previousRestartsCurrentAfterThresholdAndAtQueueStart() {
        assertEquals(PreviousAction.RESTART_CURRENT, previousAction(3_001L, true, true))
        assertEquals(PreviousAction.RESTART_CURRENT, previousAction(100L, true, false))
        assertEquals(PreviousAction.GO_TO_PREVIOUS, previousAction(3_000L, true, true))
        assertEquals(PreviousAction.NONE, previousAction(10_000L, false, true))
    }

    @Test fun nextIsSafeForEmptyAndSingleItemQueuesAndRespectsRepeatAndShuffle() {
        assertFalse(canAdvanceNext(false, false, Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_OFF, false))
        assertFalse(canAdvanceNext(true, false, Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_OFF, false))
        assertTrue(canAdvanceNext(true, true, Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_OFF, false))
        assertTrue(canAdvanceNext(true, false, Player.REPEAT_MODE_ONE, Player.REPEAT_MODE_OFF, false))
        assertTrue(canAdvanceNext(true, false, Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_OFF, true))
    }

    @Test fun reconnectingToActiveSessionDoesNotRestoreSavedSelection() {
        assertFalse(shouldRestoreSavedSelection(hasCurrentMediaItem = true))
        assertTrue(shouldRestoreSavedSelection(hasCurrentMediaItem = false))
    }

    @Test fun sessionSnapshotRestoresSongPositionPauseModesAndQueue() {
        val state = PlayerState().withSessionSnapshot(PlaybackSessionSnapshot(
            songId = "song-b", isPlaying = false, positionMs = 42_500, durationMs = 180_000,
            isPreparing = false, fileAvailable = true, repeatMode = Player.REPEAT_MODE_ALL, shuffle = true,
            canPrevious = true, canNext = true, canSeek = true,
            queueSongIds = listOf("song-a", "song-b", "song-c"), queueIndex = 1
        ))
        assertEquals("song-b", state.selectedId)
        assertEquals(42_500, state.currentMs)
        assertEquals(180_000, state.durationMs)
        assertFalse(state.isPlaying)
        assertEquals(Player.REPEAT_MODE_ALL, state.repeatMode)
        assertTrue(state.shuffle)
        assertEquals(listOf("song-a", "song-b", "song-c"), state.queueSongIds)
        assertEquals(1, state.queueIndex)
        assertTrue(state.canSeek)
    }

    @Test fun repeatAllCanAdvanceAtQueueEndAndSingleItemQueuesRemainPlayable() {
        assertTrue(canAdvanceNext(true, false, Player.REPEAT_MODE_ALL, Player.REPEAT_MODE_OFF, false))
        assertTrue(canAdvanceNext(true, false, Player.REPEAT_MODE_ALL, Player.REPEAT_MODE_OFF, true))
        assertFalse(canAdvanceNext(false, false, Player.REPEAT_MODE_ALL, Player.REPEAT_MODE_OFF, false))
        assertTrue(canAdvanceNext(true, false, Player.REPEAT_MODE_ALL, Player.REPEAT_MODE_OFF, false))
    }

    @Test fun repeatControlCyclesOffAllOneAndBackToOff() {
        assertEquals(Player.REPEAT_MODE_ALL, nextRepeatMode(Player.REPEAT_MODE_OFF))
        assertEquals(Player.REPEAT_MODE_ONE, nextRepeatMode(Player.REPEAT_MODE_ALL))
        assertEquals(Player.REPEAT_MODE_OFF, nextRepeatMode(Player.REPEAT_MODE_ONE))
        assertEquals(Player.REPEAT_MODE_ALL, nextRepeatMode(-1))
    }

    @Test fun upcomingQueueTraversalUsesMedia3NextIndicesAndStopsAtEndOrCycle() {
        val queue = listOf("a", "b", "c", "d")
        assertEquals(listOf("c", "d"), upcomingMediaIds(queue, 1) { index ->
            when (index) { 1 -> 2; 2 -> 3; else -> null }
        })
        assertEquals(emptyList<String>(), upcomingMediaIds(queue, 8) { it + 1 })
        assertEquals(listOf("b", "c"), upcomingMediaIds(queue, 0) { index -> when (index) { 0 -> 1; 1 -> 2; 2 -> 0; else -> null } })
    }

    @Test fun queueReorderBoundsHandleEmptySingleAndMultipleItems() {
        assertNull(queueMoveDestination(0, 0, 1))
        assertNull(queueMoveDestination(1, 0, -1))
        assertEquals(2, queueMoveDestination(4, 1, 1))
        assertEquals(0, queueMoveDestination(4, 1, -1))
    }

    @Test fun sourceErrorsDistinguishMissingFileFromPresentButUnreadableMedia() {
        assertTrue(playbackSourceErrorMessage("Demo", false).contains("no se puede acceder", ignoreCase = true))
        assertTrue(playbackSourceErrorMessage("Demo", true).contains("formato no compatible"))
    }

    @Test fun skipClampsToKnownDurationAndRepeatedEndSkipsAreNoOps() {
        assertEquals(50_000L, calculateSkipTarget(35_000L, 60_000L, 15_000L, false))
        assertEquals(60_000L, calculateSkipTarget(50_000L, 60_000L, 15_000L, false)) // 10 seconds remain
        assertEquals(60_000L, calculateSkipTarget(57_000L, 60_000L, 15_000L, false)) // 3 seconds remain
        assertEquals(60_000L, calculateSkipTarget(59_000L, 60_000L, 15_000L, false)) // 1 second remains
        assertEquals(60_000L, calculateSkipTarget(45_000L, 60_000L, 15_000L, false)) // exactly 15 seconds remain
        val afterFirstTap = calculateSkipTarget(50_000L, 60_000L, 15_000L, false)
        assertEquals(60_000L, afterFirstTap)
        assertEquals(null, calculateSkipTarget(afterFirstTap!!, 60_000L, 15_000L, false)) // repeated tap at end
        assertEquals(null, calculateSkipTarget(60_000L, 60_000L, 15_000L, false))
    }

    @Test fun skipHandlesPauseAndUnknownDurationWithoutInventingAnEnd() {
        assertEquals(60_000L, calculateSkipTarget(45_000L, 60_000L, 15_000L, false)) // same seek target while paused
        assertEquals(null, calculateSkipTarget(45_000L, null, 15_000L, false))
        assertEquals(30_000L, calculateSkipTarget(45_000L, null, -15_000L, false))
        assertEquals(0L, calculateSkipTarget(5_000L, null, -15_000L, false))
        assertEquals(null, calculateSkipTarget(0L, null, -15_000L, false))
        assertEquals(null, calculateSkipTarget(60_000L, 60_000L, 15_000L, true))
        assertEquals(null, clampSeekPosition(20_000L, null))
        assertEquals(60_000L, clampSeekPosition(70_000L, 60_000L))
    }

    @Test fun flacSeekLeavesAOneSecondEndGuardWhileOtherFormatsUseTheReportedDuration() {
        val guard = seekEndGuardMs("flac")
        assertEquals(1_000L, guard)
        assertEquals(50_200L, calculateSkipTarget(43_413L, 51_200L, 15_000L, false, guard))
        assertEquals(50_200L, clampSeekPosition(51_200L, 51_200L, guard))
        assertEquals(0L, seekEndGuardMs("m4a"))
        assertEquals(51_200L, calculateSkipTarget(43_413L, 51_200L, 15_000L, false,
            seekEndGuardMs("m4a")))
        assertEquals(null, calculateSkipTarget(50_700L, 51_200L, 15_000L, false, guard))
    }
}
