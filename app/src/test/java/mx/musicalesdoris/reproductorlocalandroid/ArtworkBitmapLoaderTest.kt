package mx.musicalesdoris.reproductorlocalandroid

import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.calculateArtworkSampleSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkBitmapLoaderTest {
    @Test fun keepsSmallArtworkAtFullResolution() {
        assertEquals(1, calculateArtworkSampleSize(640, 640, 1200))
    }

    @Test fun downsamplesLargeArtworkToNearRequestedBounds() {
        val sample = calculateArtworkSampleSize(6000, 3000, 1200)
        assertEquals(4, sample)
        assertTrue(6000 / sample <= 1500)
        assertTrue(3000 / sample <= 1200)
    }

    @Test fun usesSafeDefaultForInvalidBounds() {
        assertEquals(1, calculateArtworkSampleSize(0, 2000, 1200))
        assertEquals(1, calculateArtworkSampleSize(2000, 2000, 0))
    }
}
