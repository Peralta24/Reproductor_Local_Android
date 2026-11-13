package mx.musicalesdoris.reproductorlocalandroid

import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlaybackQueueInstallerTest {
    private class FakeQueueSink : Media3QueueSink {
        val calls = mutableListOf<String>()
        var installedIds: List<String> = emptyList()
        override fun setMediaItems(items: List<MediaItem>, startIndex: Int) {
            calls += "items:$startIndex"
            installedIds = items.map(MediaItem::mediaId)
        }
        override fun setShuffleModeEnabled(enabled: Boolean) { calls += "shuffle:$enabled" }
        override fun seekToDefaultPosition() { calls += "random-default" }
        override fun prepare() { calls += "prepare" }
        override fun play() { calls += "play" }
    }

    private fun item(id: String) = MediaItem.Builder().setMediaId(id).setUri("file:///$id.flac").build()

    @Test fun shuffleIsAppliedAfterTheOriginalQueueIsInstalledAndDoesNotRewriteItsOrder() {
        val sourceOrder = listOf(item("a"), item("b"), item("c"))
        val sink = FakeQueueSink()
        installMedia3Queue(sink, sourceOrder, 0, shuffleMode = true,
            startAtShuffledDefault = true, autoplay = true)
        assertEquals(listOf("a", "b", "c"), sink.installedIds)
        assertEquals(listOf("items:0", "shuffle:true", "random-default", "prepare", "play"), sink.calls)
        assertEquals(listOf("a", "b", "c"), sourceOrder.map(MediaItem::mediaId))
    }

    @Test fun disablingShuffleRestoresNormalQueueOrderWithoutAdditionalRandomOperations() {
        val sink = FakeQueueSink()
        installMedia3Queue(sink, listOf(item("a"), item("b")), 0,
            shuffleMode = false, startAtShuffledDefault = false, autoplay = false)
        assertEquals(listOf("items:0", "shuffle:false", "prepare"), sink.calls)
        assertEquals(listOf("a", "b"), sink.installedIds)
    }

    @Test fun unspecifiedShuffleModeIsPreservedAndSingleItemCanBePreparedWithoutAutoplay() {
        val sink = FakeQueueSink()
        installMedia3Queue(sink, listOf(item("a")), 0,
            shuffleMode = null, startAtShuffledDefault = false, autoplay = false)
        assertEquals(listOf("items:0", "prepare"), sink.calls)
        assertEquals(listOf("a"), sink.installedIds)
    }
}
