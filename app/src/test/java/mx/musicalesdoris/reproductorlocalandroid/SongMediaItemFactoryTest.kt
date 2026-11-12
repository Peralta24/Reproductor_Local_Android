package mx.musicalesdoris.reproductorlocalandroid

import androidx.media3.common.MediaMetadata
import android.net.Uri
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SongMediaItemFactoryTest {
    @Test
    fun createsLocalMediaItemWithSongMetadataAndArtwork() {
        val artwork = byteArrayOf(1, 2, 3, 4)
        val audio = File("/music/track.mp3")
        val song = Song(
            id = "stable-id",
            fileName = "track.mp3",
            title = "Track title",
            artist = "Performer",
            albumArtist = "Album performer",
            album = "Album name",
            trackNumber = 7,
            genre = "Jazz"
        )

        val item = SongMediaItemFactory.create(song, audio, artwork)

        assertEquals("stable-id", item.mediaId)
        assertEquals(Uri.fromFile(audio), item.localConfiguration?.uri)
        assertEquals("Track title", item.mediaMetadata.title.toString())
        assertEquals("Performer", item.mediaMetadata.artist.toString())
        assertEquals("Album name", item.mediaMetadata.albumTitle.toString())
        assertEquals(7, item.mediaMetadata.trackNumber)
        assertEquals("Jazz", item.mediaMetadata.genre.toString())
        assertEquals(MediaMetadata.PICTURE_TYPE_FRONT_COVER, item.mediaMetadata.artworkDataType)
        assertArrayEquals(artwork, item.mediaMetadata.artworkData)
    }

    @Test
    fun missingOptionalMetadataRemainsMissing() {
        val song = Song(id = "id", fileName = "unknown.mp3", title = "unknown")
        val item = SongMediaItemFactory.create(song, File("/music/unknown.mp3"))

        assertEquals("unknown", item.mediaMetadata.title.toString())
        assertNull(item.mediaMetadata.artist)
        assertNull(item.mediaMetadata.albumTitle)
        assertNull(item.mediaMetadata.genre)
        assertNull(item.mediaMetadata.artworkData)
    }

    @Test
    fun fallsBackToAlbumArtistWhenTrackArtistIsMissing() {
        val song = Song(
            id = "id",
            fileName = "track.mp3",
            title = "Track",
            albumArtist = "Album performer"
        )

        val item = SongMediaItemFactory.create(song, File("/music/track.mp3"))

        assertEquals("Album performer", item.mediaMetadata.artist.toString())
    }
}
