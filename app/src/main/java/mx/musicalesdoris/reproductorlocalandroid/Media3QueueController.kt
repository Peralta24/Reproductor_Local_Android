package mx.musicalesdoris.reproductorlocalandroid

import androidx.media3.common.C
import androidx.media3.common.Player

internal data class Media3QueueSnapshot(
    val ids: List<String>,
    val currentIndex: Int,
    val upcomingIds: List<String>
)

/** Thin queue operations over the one player owned by PlaybackService. */
internal class Media3QueueController(private val player: Player) {
    fun snapshot(): Media3QueueSnapshot {
        val ids = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
        val current = player.currentMediaItemIndex.takeIf { player.currentMediaItem != null && it in ids.indices } ?: -1
        val nextIndex = player.nextMediaItemIndex.takeIf { it != C.INDEX_UNSET }
        val upcoming = upcomingMediaIds(ids, current) { index -> if (index == current) nextIndex else null }
        return Media3QueueSnapshot(ids, current, upcoming)
    }

    fun playItem(index: Int): Boolean {
        if (!player.isCommandAvailable(Player.COMMAND_SEEK_TO_MEDIA_ITEM) || index !in 0 until player.mediaItemCount) return false
        player.seekTo(index, C.TIME_UNSET)
        player.play()
        return true
    }

    fun removeItem(index: Int): Boolean {
        if (!player.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS) || index !in 0 until player.mediaItemCount) return false
        player.removeMediaItem(index)
        return true
    }

    fun moveItem(fromIndex: Int, toIndex: Int): Boolean {
        val target = queueMoveDestination(player.mediaItemCount, fromIndex, toIndex - fromIndex) ?: return false
        if (!player.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) return false
        if (fromIndex != target) player.moveMediaItem(fromIndex, target)
        return true
    }
}
