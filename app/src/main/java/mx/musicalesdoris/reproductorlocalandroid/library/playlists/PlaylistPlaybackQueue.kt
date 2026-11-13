package mx.musicalesdoris.reproductorlocalandroid.library.playlists

import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.Playlist

/** Ordered, availability-filtered playback snapshot for one playlist. */
data class PlaylistPlaybackQueue(val songs: List<Song>, val startIndex: Int)

fun buildPlaylistPlaybackQueue(
    playlist: Playlist,
    catalog: List<Song>,
    isAvailable: (Song) -> Boolean,
    startingSongId: String? = null
): PlaylistPlaybackQueue {
    val playable = playlist.songsIn(catalog).filter(isAvailable)
    val index = startingSongId?.let { id -> playable.indexOfFirst { it.id == id } }
        ?.takeIf { it >= 0 } ?: 0
    return PlaylistPlaybackQueue(playable, index)
}
