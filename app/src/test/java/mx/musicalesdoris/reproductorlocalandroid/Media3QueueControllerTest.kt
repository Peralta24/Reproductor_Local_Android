package mx.musicalesdoris.reproductorlocalandroid

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Media3QueueControllerTest {
    private fun item(id: String) = MediaItem.Builder().setMediaId(id)
        .setUri("file:///music/$id.mp3").build()

    @Test fun queueSnapshotSupportsEmptySingleAndMultipleItems() {
        val player = newPlayer()
        try {
            val queue = Media3QueueController(player)
            assertEquals(emptyList<String>(), queue.snapshot().ids)
            player.setMediaItems(listOf(item("one")))
            assertEquals(listOf("one"), queue.snapshot().ids)
            assertEquals("one", queue.snapshot().ids[queue.snapshot().currentIndex])
            player.setMediaItems(listOf(item("one"), item("two"), item("three")))
            assertEquals(listOf("one", "two", "three"), queue.snapshot().ids)
        } finally { player.release() }
    }

    @Test fun selectingRemovingAndReorderingMutateTheSingleActiveExoPlayerQueue() {
        val player = newPlayer()
        try {
            player.setMediaItems(listOf(item("one"), item("two"), item("three")))
            val queue = Media3QueueController(player)
            assertTrue(queue.playItem(1))
            assertEquals("two", player.currentMediaItem?.mediaId)
            assertTrue(player.playWhenReady)
            assertTrue(queue.moveItem(0, 2))
            assertEquals(listOf("two", "three", "one"), queue.snapshot().ids)
            assertEquals("two", player.currentMediaItem?.mediaId)
            assertTrue(queue.removeItem(1))
            assertEquals(listOf("two", "one"), queue.snapshot().ids)
            assertFalse(queue.removeItem(8))
            assertFalse(queue.moveItem(0, 7))
        } finally { player.release() }
    }

    @Test fun repeatAndShuffleRemainMedia3StateAndUpcomingTraversalIsBounded() {
        val player = newPlayer()
        try {
            player.setMediaItems(listOf(item("one"), item("two"), item("three"), item("four")))
            val queue = Media3QueueController(player)
            player.seekTo(1, C.TIME_UNSET)
            player.repeatMode = Player.REPEAT_MODE_ALL
            player.shuffleModeEnabled = true
            val snapshot = queue.snapshot()
            assertEquals(listOf("one", "two", "three", "four"), snapshot.ids)
            assertEquals("two", snapshot.ids[snapshot.currentIndex])
            assertTrue(snapshot.upcomingIds.size <= 3)
            assertFalse("current track cannot appear in upcoming cycle", "two" in snapshot.upcomingIds)
            player.repeatMode = Player.REPEAT_MODE_ONE
            assertEquals(Player.REPEAT_MODE_ONE, player.repeatMode)
            player.repeatMode = Player.REPEAT_MODE_OFF
            assertEquals(Player.REPEAT_MODE_OFF, player.repeatMode)
        } finally { player.release() }
    }

    private fun newPlayer() = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build()
}
