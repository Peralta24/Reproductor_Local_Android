package mx.musicalesdoris.reproductorlocalandroid

/** Describes whether a playlist button should install its queue or only toggle the active session. */
internal data class PlaylistShuffleDecision(
    val shouldStartPlaylist: Boolean,
    val shuffleEnabled: Boolean
)

internal fun decidePlaylistShuffleToggle(
    playlistQueueIds: List<String>,
    activeQueueIds: List<String>,
    selectedSongId: String?,
    currentShuffleEnabled: Boolean
): PlaylistShuffleDecision {
    val sameQueue = isSameQueueOrRotation(playlistQueueIds, activeQueueIds) &&
        selectedSongId in playlistQueueIds

    return if (sameQueue) {
        PlaylistShuffleDecision(shouldStartPlaylist = false, shuffleEnabled = !currentShuffleEnabled)
    } else {
        PlaylistShuffleDecision(shouldStartPlaylist = true, shuffleEnabled = true)
    }
}

private fun isSameQueueOrRotation(expected: List<String>, actual: List<String>): Boolean {
    if (expected.isEmpty() || expected.size != actual.size) return false

    return expected.indices
        .filter { expected[it] == actual.first() }
        .any { start ->
            actual.indices.all { offset -> actual[offset] == expected[(start + offset) % expected.size] }
        }
}
