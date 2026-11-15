package mx.musicalesdoris.reproductorlocalandroid.library.components.song

import java.util.Locale

internal fun formatSongDuration(durationMs: Long?): String {
    val ms = durationMs ?: return "—"
    val seconds = ms.coerceAtLeast(0) / 1000
    return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
}

internal fun formatSongSubtitle(artist: String?, album: String?): String {
    val availableArtist = artist?.takeIf(String::isNotBlank)
    val availableAlbum = album?.takeIf(String::isNotBlank)
    return when {
        availableArtist != null && availableAlbum != null -> "$availableArtist · $availableAlbum"
        availableArtist != null -> availableArtist
        availableAlbum != null -> availableAlbum
        else -> "Archivo local"
    }
}
