package mx.musicalesdoris.reproductorlocalandroid.library.components.song

import org.junit.Assert.assertEquals
import org.junit.Test

class SongRowTextTest {
    @Test fun durationFormattingPreservesDisplayCases() {
        assertEquals("—", formatSongDuration(null))
        assertEquals("3:57", formatSongDuration(237_000L))
        assertEquals("12:45", formatSongDuration(765_000L))
        assertEquals("62:30", formatSongDuration(3_750_000L))
        assertEquals("0:00", formatSongDuration(-1L))
    }

    @Test fun subtitleFormattingPreservesFallbackAndSeparators() {
        assertEquals("Artista · Álbum", formatSongSubtitle("Artista", "Álbum"))
        assertEquals("Artista", formatSongSubtitle("Artista", null))
        assertEquals("Álbum", formatSongSubtitle(null, "Álbum"))
        assertEquals("Archivo local", formatSongSubtitle(" ", ""))
    }

    @Test fun changedMetadataProducesUpdatedSubtitle() {
        assertEquals("Artista · Álbum nuevo", formatSongSubtitle("Artista", "Álbum nuevo"))
    }
}
