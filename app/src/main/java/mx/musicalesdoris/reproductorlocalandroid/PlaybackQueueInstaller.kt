package mx.musicalesdoris.reproductorlocalandroid

import androidx.media3.common.MediaItem

/** Small command boundary so queue setup order can be verified without starting a second player. */
internal interface Media3QueueSink {
    fun setMediaItems(items: List<MediaItem>, startIndex: Int)
    fun setShuffleModeEnabled(enabled: Boolean)
    fun seekToDefaultPosition()
    fun prepare()
    fun play()
}

internal fun installMedia3Queue(
    sink: Media3QueueSink,
    items: List<MediaItem>,
    startIndex: Int,
    shuffleMode: Boolean?,
    startAtShuffledDefault: Boolean,
    autoplay: Boolean
) {
    sink.setMediaItems(items, startIndex)
    shuffleMode?.let(sink::setShuffleModeEnabled)
    if (startAtShuffledDefault) sink.seekToDefaultPosition()
    sink.prepare()
    if (autoplay) sink.play()
}
