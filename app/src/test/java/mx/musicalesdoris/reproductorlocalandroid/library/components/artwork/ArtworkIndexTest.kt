package mx.musicalesdoris.reproductorlocalandroid.library.components.artwork

import kotlinx.coroutines.runBlocking
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.albumKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkIndexTest {
    @Test fun initialCatalogPreservesDirectArtworkPriorityAndLastAlbumMatch() = runBlocking {
        val first = Song("1", "1.mp3", "Track 1", album = "Album", artist = "Artist", artworkFileName = "first.jpg")
        val second = Song("2", "2.mp3", "Track 2", album = "Album", artist = "Artist", artworkFileName = "last.jpg")
        val direct = Song("3", "3.mp3", "Track 3", album = "Other Album", artist = "Artist", artworkFileName = "direct.jpg")
        val catalog = listOf(first, second, direct)
        val cache = ArtworkIndexCache()
        cache.invalidateFor(catalog)
        assertNull(cache.artworkName(first.copy(artworkFileName = null), catalog))

        val index = buildArtworkIndex(catalog)
        assertTrue(cache.publishIfCurrent(index) { catalog })

        assertEquals("direct.jpg", cache.artworkName(direct, catalog))
        assertEquals("last.jpg", cache.artworkName(first.copy(artworkFileName = null), catalog))
        assertEquals(1L, cache.revision.value)
    }

    @Test fun emptyCatalogPublishesAReadyEmptyIndex() = runBlocking {
        val catalog = emptyList<Song>()
        val cache = ArtworkIndexCache()
        val index = buildArtworkIndex(catalog)

        assertTrue(cache.publishIfCurrent(index) { catalog })
        assertEquals(1L, cache.revision.value)
        assertEquals(emptyMap<String, String>(), index.byAlbum)
    }

    @Test fun separateAlbumsKeepSeparateArtwork() = runBlocking {
        val albumOne = Song("1", "1.mp3", "One", album = "Album One", artist = "Artist", artworkFileName = "one.jpg")
        val albumTwo = Song("2", "2.mp3", "Two", album = "Album Two", artist = "Artist", artworkFileName = "two.jpg")
        val catalog = listOf(albumOne, albumTwo)
        val cache = ArtworkIndexCache()
        cache.publishIfCurrent(buildArtworkIndex(catalog)) { catalog }

        assertEquals("one.jpg", cache.artworkName(albumOne.copy(artworkFileName = null), catalog))
        assertEquals("two.jpg", cache.artworkName(albumTwo.copy(artworkFileName = null), catalog))
    }

    @Test fun replacementRejectsOldIndexAndNotifiesWhenNewIndexIsReady() = runBlocking {
        val oldCatalog = listOf(Song("1", "1.mp3", "Old", album = "Old Album", artist = "Artist", artworkFileName = "old.jpg"))
        val newCatalog = listOf(Song("2", "2.mp3", "New", album = "New Album", artist = "Artist", artworkFileName = "new.jpg"))
        val cache = ArtworkIndexCache()
        cache.invalidateFor(oldCatalog)
        val oldIndex = buildArtworkIndex(oldCatalog)

        cache.invalidateFor(newCatalog)
        assertFalse(cache.publishIfCurrent(oldIndex) { newCatalog })
        assertEquals(0L, cache.revision.value)
        assertNull(cache.artworkName(newCatalog.single().copy(artworkFileName = null), newCatalog))

        val newIndex = buildArtworkIndex(newCatalog)
        assertTrue(cache.publishIfCurrent(newIndex) { newCatalog })
        assertEquals(1L, cache.revision.value)
        assertEquals("new.jpg", cache.artworkName(newCatalog.single().copy(artworkFileName = null), newCatalog))
    }

    @Test fun largeCatalogBuildsOneBoundedAlbumMap() = runBlocking {
        val catalog = List(7_500) { index ->
            Song("$index", "$index.mp3", "Track $index", album = "Album ${index / 3}",
                artist = "Artist", artworkFileName = "cover-${index / 3}.jpg")
        }

        val index = buildArtworkIndex(catalog)

        assertTrue(index.catalog === catalog)
        assertEquals(2_500, index.byAlbum.size)
        assertEquals("cover-0.jpg", index.byAlbum[catalog.first().albumKey()])
    }

    @Test fun artworkRevisionKeyChangesWhenIndexBecomesReady() {
        val catalog = listOf(Song("1", "1.mp3", "Track"))

        assertFalse(ArtworkRevisionKey(catalog, 0L) == ArtworkRevisionKey(catalog, 1L))
        assertTrue(ArtworkRevisionKey(catalog, 1L) == ArtworkRevisionKey(catalog, 1L))
    }
}
