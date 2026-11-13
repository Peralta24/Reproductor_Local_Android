package mx.musicalesdoris.reproductorlocalandroid

import mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer.songFormatLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class SongFormatLabelTest {
    @Test fun recognizesSupportedFileExtensionsWithoutCaseSensitivity() {
        assertEquals("MP3", songFormatLabel("track.MP3"))
        assertEquals("FLAC", songFormatLabel("lossless.flac"))
        assertEquals("WAV", songFormatLabel("recording.wave"))
        assertEquals("AAC", songFormatLabel("audio.aac"))
        assertEquals("OGG", songFormatLabel("voice.oga"))
        assertEquals("M4A", songFormatLabel("track.m4a"))
        assertEquals("OPUS", songFormatLabel("track.opus"))
    }

    @Test fun usesNeutralLabelWhenExtensionIsUnknownOrMissing() {
        assertEquals("AUDIO", songFormatLabel("file.unknown"))
        assertEquals("AUDIO", songFormatLabel("file"))
    }
}
