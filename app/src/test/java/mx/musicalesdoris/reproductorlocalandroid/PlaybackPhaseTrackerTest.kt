package mx.musicalesdoris.reproductorlocalandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPhaseTrackerTest {
    @Test
    fun preparationGatesPlayerOperationsUntilPrepared() {
        val tracker = PlaybackPhaseTracker()

        tracker.beginPreparation()

        assertEquals(PlaybackPhase.PREPARING, tracker.phase)
        assertTrue(tracker.isPreparing)
        assertFalse(tracker.canControlPlayer)
        assertTrue(tracker.onPrepared())
        assertEquals(PlaybackPhase.PREPARED, tracker.phase)
        assertTrue(tracker.canControlPlayer)
    }

    @Test
    fun playbackCanPauseAndResumeWithoutLosingPreparedState() {
        val tracker = PlaybackPhaseTracker()
        tracker.beginPreparation()
        tracker.onPrepared()

        assertTrue(tracker.onPlaybackStarted())
        assertEquals(PlaybackPhase.PLAYING, tracker.phase)
        assertTrue(tracker.onPlaybackPaused())
        assertEquals(PlaybackPhase.PREPARED, tracker.phase)
        assertTrue(tracker.onPlaybackStarted())
    }

    @Test
    fun ignoresStaleCallbacksAndAllowsRecoveryAfterRelease() {
        val tracker = PlaybackPhaseTracker()
        assertFalse(tracker.onPrepared())

        tracker.beginPreparation()
        assertTrue(tracker.onError())
        assertEquals(PlaybackPhase.ERROR, tracker.phase)
        assertFalse(tracker.onPrepared())

        tracker.onReleased()
        assertEquals(PlaybackPhase.IDLE, tracker.phase)
        tracker.beginPreparation()
        assertTrue(tracker.onPrepared())
        assertEquals(PlaybackPhase.PREPARED, tracker.phase)
    }

    @Test
    fun playbackErrorDisablesOperationsUntilThePlayerIsReleasedAndRecovered() {
        val tracker = PlaybackPhaseTracker()
        tracker.beginPreparation()
        tracker.onPrepared()
        tracker.onPlaybackStarted()

        assertTrue(tracker.onError())
        assertEquals(PlaybackPhase.ERROR, tracker.phase)
        assertFalse(tracker.canControlPlayer)
        assertFalse(tracker.onPlaybackPaused())

        tracker.onReleased()
        tracker.beginPreparation()
        assertTrue(tracker.onPrepared())
        assertTrue(tracker.canControlPlayer)
    }
}
