package mx.musicalesdoris.reproductorlocalandroid

import androidx.media3.common.Player

internal enum class PreviousAction { NONE, RESTART_CURRENT, GO_TO_PREVIOUS }

internal data class PlaybackSessionSnapshot(
    val songId: String?,
    val isPlaying: Boolean,
    val positionMs: Int,
    val durationMs: Int,
    val isPreparing: Boolean,
    val fileAvailable: Boolean,
    val repeatMode: Int,
    val shuffle: Boolean,
    val canPrevious: Boolean,
    val canNext: Boolean,
    val canSeek: Boolean,
    val queueSongIds: List<String>,
    val queueIndex: Int,
    val upcomingSongIds: List<String> = emptyList()
)

internal fun PlayerState.withSessionSnapshot(snapshot: PlaybackSessionSnapshot): PlayerState = copy(
    selectedId = snapshot.songId,
    isPlaying = snapshot.isPlaying,
    currentMs = snapshot.positionMs.coerceAtLeast(0),
    durationMs = snapshot.durationMs.coerceAtLeast(0),
    isPreparing = snapshot.isPreparing,
    selectedFileAvailable = snapshot.fileAvailable,
    repeatMode = snapshot.repeatMode,
    shuffle = snapshot.shuffle,
    canSkipPrevious = snapshot.canPrevious,
    canSkipNext = snapshot.canNext,
    canSeek = snapshot.canSeek,
    queueSongIds = snapshot.queueSongIds,
    queueIndex = snapshot.queueIndex,
    upcomingSongIds = snapshot.upcomingSongIds
)

internal fun upcomingMediaIds(queue: List<String>, currentIndex: Int, nextIndex: (Int) -> Int?): List<String> {
    if (currentIndex !in queue.indices) return emptyList()
    val seen = mutableSetOf(currentIndex)
    val result = mutableListOf<String>()
    var index = nextIndex(currentIndex)
    while (index != null && index in queue.indices && seen.add(index)) {
        result += queue[index]
        index = nextIndex(index)
    }
    return result
}

internal fun queueMoveDestination(count: Int, fromIndex: Int, delta: Int): Int? {
    if (count <= 0 || fromIndex !in 0 until count || delta == 0) return null
    return (fromIndex + delta).takeIf { it in 0 until count }
}

internal fun previousAction(positionMs: Long, hasCurrentItem: Boolean, hasPreviousItem: Boolean): PreviousAction = when {
    !hasCurrentItem -> PreviousAction.NONE
    positionMs > PREVIOUS_RESTART_THRESHOLD_MS || !hasPreviousItem -> PreviousAction.RESTART_CURRENT
    else -> PreviousAction.GO_TO_PREVIOUS
}

internal fun canAdvanceNext(
    hasCurrentItem: Boolean,
    hasNextItem: Boolean,
    repeatMode: Int,
    repeatOffMode: Int,
    shuffleEnabled: Boolean
): Boolean = hasCurrentItem && (hasNextItem || repeatMode != repeatOffMode || shuffleEnabled)

internal fun nextRepeatMode(currentMode: Int): Int = when (currentMode) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
    else -> Player.REPEAT_MODE_ALL
}

internal fun shouldRestoreSavedSelection(hasCurrentMediaItem: Boolean): Boolean = !hasCurrentMediaItem

/** Returns null when a seek would be unsafe or would not change the current position. */
internal fun calculateSkipTarget(
    currentPositionMs: Long,
    durationMs: Long?,
    deltaMs: Long,
    playbackEnded: Boolean,
    endGuardMs: Long = 0L
): Long? {
    if (playbackEnded || currentPositionMs < 0L || deltaMs == 0L) return null
    if (deltaMs > 0L && (durationMs == null || durationMs < 0L)) return null
    val safeEnd = durationMs?.takeIf { it >= 0L }
        ?.minus(endGuardMs.coerceAtLeast(0L))?.coerceAtLeast(0L)
    if (deltaMs > 0L && safeEnd != null && currentPositionMs >= safeEnd) return null
    val requested = if (deltaMs > 0L && currentPositionMs > Long.MAX_VALUE - deltaMs) {
        Long.MAX_VALUE
    } else {
        (currentPositionMs + deltaMs).coerceAtLeast(0L)
    }
    val bounded = safeEnd?.let(requested::coerceAtMost) ?: requested
    return bounded.takeIf { it != currentPositionMs }
}

internal fun clampSeekPosition(requestedPositionMs: Long, durationMs: Long?, endGuardMs: Long = 0L): Long? {
    if (requestedPositionMs < 0L || durationMs == null || durationMs < 0L) return null
    val safeEnd = (durationMs - endGuardMs.coerceAtLeast(0L)).coerceAtLeast(0L)
    return requestedPositionMs.coerceAtMost(safeEnd)
}

internal fun seekEndGuardMs(fileExtension: String?): Long =
    if (fileExtension.equals("flac", ignoreCase = true)) FLAC_EOF_SEEK_GUARD_MS else 0L

internal fun playbackSourceErrorMessage(title: String?, fileReadable: Boolean): String = if (!fileReadable) {
    "No se puede acceder al archivo de audio de ${title ?: "esta canción"}. La referencia permanece en tu biblioteca."
} else {
    "No se pudo reproducir ${title ?: "el audio"}. El archivo existe, pero puede estar dañado o usar un formato no compatible."
}

private const val PREVIOUS_RESTART_THRESHOLD_MS = 3_000L
private const val FLAC_EOF_SEEK_GUARD_MS = 1_000L
