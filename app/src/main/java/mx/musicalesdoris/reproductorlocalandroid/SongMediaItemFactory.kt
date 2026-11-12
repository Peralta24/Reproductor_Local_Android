package mx.musicalesdoris.reproductorlocalandroid

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.io.File

/** Converts the persisted local catalog model into metadata published by the media session. */
internal object SongMediaItemFactory {
    fun create(song: Song, audioFile: File, artwork: ByteArray? = null): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist ?: song.albumArtist)
            .setAlbumTitle(song.album)
            .setTrackNumber(song.trackNumber)
            .setGenre(song.genre)
            .apply {
                if (artwork != null) setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
            .build()
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(Uri.fromFile(audioFile))
            .setMediaMetadata(metadata)
            .build()
    }
}
