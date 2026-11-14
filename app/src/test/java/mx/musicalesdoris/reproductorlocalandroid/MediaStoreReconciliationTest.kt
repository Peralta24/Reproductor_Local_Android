package mx.musicalesdoris.reproductorlocalandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreReconciliationTest {
    private fun mediaSong(id: String, available: Boolean = true, favorite: Boolean = false) = Song(
        id = id,
        fileName = "$id.mp3",
        title = id,
        favorite = favorite,
        favoritedAt = if (favorite) 123L else null,
        contentUri = "content://media/external/audio/media/$id",
        sizeBytes = 100L,
        mediaAvailable = available
    )

    @Test
    fun completeSnapshotMarksMissingRowsUnavailableWithoutDroppingUserData() {
        val retained = mediaSong("1", favorite = true)
        val playlist = listOf(retained.id)

        val result = reconcileMediaStoreSnapshot(listOf(retained), emptyList())

        assertEquals(1, result.size)
        assertFalse(result.single().mediaAvailable)
        assertTrue(result.single().favorite)
        assertEquals(playlist, listOf(result.single().id))
    }

    @Test
    fun partialVisibleSnapshotKeepsMissingRowsAndUpdatesVisibleRows() {
        val missing = mediaSong("1", favorite = true)
        val visible = mediaSong("2")

        val result = reconcileMediaStoreSnapshot(listOf(missing), listOf(visible))

        assertEquals(listOf("1", "2"), result.map { it.id })
        assertFalse(result.first().mediaAvailable)
        assertTrue(result.last().mediaAvailable)
    }

    @Test
    fun recoveredMediaUsesItsStableIdAndRestoresAvailability() {
        val old = mediaSong("1", available = false, favorite = true)
        val current = mediaSong("1", available = true)

        val result = reconcileMediaStoreSnapshot(listOf(old), listOf(current)).single()

        assertEquals(old.id, result.id)
        assertTrue(result.mediaAvailable)
        assertTrue(result.favorite)
    }

    @Test
    fun privateImportsAreNotChangedByMediaStoreReconciliation() {
        val imported = Song("private", "private.mp3", "Private")

        assertEquals(listOf(imported), reconcileMediaStoreSnapshot(listOf(imported), emptyList()))
    }

    @Test
    fun restoredReferencesAreUnavailableUntilTheirAudioIsVerified() {
        val mediaStoreSong = mediaSong("1")
        val privateSong = Song("private", "missing.mp3", "Private")

        assertFalse(restoredMediaAvailability(mediaStoreSong, privateAudioExists = true))
        assertFalse(restoredMediaAvailability(privateSong, privateAudioExists = false))
        assertTrue(restoredMediaAvailability(privateSong, privateAudioExists = true))
    }

    @Test
    fun exactMediaStoreIdentityIsStableButDoesNotUseTitleAlone() {
        val identity = mediaStoreIdentity("content://media/external/audio/media/7", "same.mp3", 100, 500)

        assertEquals(identity, mediaStoreIdentity("content://media/external/audio/media/7", "same.mp3", 100, 500))
        assertFalse(identity == mediaStoreIdentity("content://media/external/audio/media/8", "same.mp3", 100, 500))
        assertEquals("content://media/external/audio/media/7",
            canonicalMediaStoreAudioUri("content://com.android.providers.media.documents/document/audio%3A7", "audio:7"))
        assertEquals(null, canonicalMediaStoreAudioUri("content://com.android.providers.media.documents/document/image%3A7", "image:7"))
    }

    @Test
    fun revokedPermissionAndDeviceScanCooldownNeverDeleteCatalogEntries() {
        val media = mediaSong("1", favorite = true)
        val imported = Song("private", "private.mp3", "Private", favorite = true)
        val unavailable = markMediaStoreSnapshotUnavailable(listOf(media, imported))

        assertEquals(2, unavailable.size)
        assertFalse(unavailable.first().mediaAvailable)
        assertTrue(unavailable.first().favorite)
        assertTrue(unavailable.last().mediaAvailable)
        assertTrue(isDeviceScanCoolingDown(10_000, 12_000, 5_000))
        assertFalse(isDeviceScanCoolingDown(10_000, 16_000, 5_000))
        assertFalse(isDeviceScanCoolingDown(0, 1_000, 5_000))
    }

    @Test
    fun importDedupRequiresExactMediaStoreIdentity() {
        val key = mediaStoreIdentity("content://media/external/audio/media/7", "same.mp3", 100, 500)
        val copy = Song("copy", "local.mp3", "same", importSourceIdentity = key)
        val different = Song("different", "other.mp3", "same", contentUri = "content://media/external/audio/media/8",
            sizeBytes = 100, importedAt = 500)

        assertTrue(copy.matchesMediaStoreIdentity(key))
        assertFalse(different.matchesMediaStoreIdentity(key))
    }

    @Test
    fun confirmedRelinkPreservesFavoriteAndPlaylistIdentity() {
        val old = Song("saved-id", "missing.mp3", "Saved", favorite = true,
            favoritedAt = 77, privateAudioFingerprint = "same-hash")
        val imported = Song("new-id", "restored.mp3", "Restored", privateAudioFingerprint = "same-hash")
        val playlist = Playlist(title = "Mix", songIds = listOf(old.id))

        val linked = confirmedSongRelink(old, imported)

        assertEquals(old.id, linked.id)
        assertTrue(linked.favorite)
        assertEquals(old.favoritedAt, linked.favoritedAt)
        assertEquals(listOf(linked.id), playlist.songIds)
        assertEquals(imported.fileName, linked.fileName)
    }

    @Test
    fun repeatedIdenticalMediaStoreSnapshotsDoNotAddSongs() {
        val existing = mediaSong("1").copy(mediaStoreVolume = "external_primary")
        val visible = existing.copy(mediaAvailable = true)

        val first = reconcileMediaStoreSnapshot(listOf(existing), listOf(visible))
        val second = reconcileMediaStoreSnapshot(first, listOf(visible))

        assertEquals(first, second)
        assertEquals(1, second.size)
    }

    @Test
    fun historicalRowsWithTheSameExactMediaSourceConsolidateAndRemapReferences() {
        val canonical = mediaSong("old-row", available = false, favorite = true)
            .copy(favoritedAt = 10, mediaStoreVolume = "external_primary")
        val duplicate = mediaSong("new-row", available = true)
            .copy(contentUri = canonical.contentUri, mediaStoreVolume = canonical.mediaStoreVolume,
                album = "Album", artist = "Guest artist")
        val playlist = Playlist(title = "Saved", songIds = listOf(duplicate.id, canonical.id, duplicate.id))

        val result = consolidateExactDuplicateSongs(listOf(canonical, duplicate))
        val remappedPlaylist = remapPlaylistSongIds(listOf(playlist), result.idRemap).single()

        assertEquals(1, result.songs.size)
        assertEquals(canonical.id, result.songs.single().id)
        assertTrue(result.songs.single().mediaAvailable)
        assertTrue(result.songs.single().favorite)
        assertEquals(listOf(canonical.id, canonical.id, canonical.id), remappedPlaylist.songIds)
        assertEquals(result.songs, consolidateExactDuplicateSongs(result.songs).songs)
    }

    @Test
    fun sameTitleWithDifferentMediaIdentityIsNotDeduplicated() {
        val first = mediaSong("1").copy(title = "Same title", contentUri = "content://media/external/audio/media/1")
        val second = mediaSong("2").copy(title = "Same title", contentUri = "content://media/external/audio/media/2")

        assertEquals(2, consolidateExactDuplicateSongs(listOf(first, second)).songs.size)
    }

    @Test
    fun privateImportsConsolidateOnlyWhenSourceAndFingerprintBothMatch() {
        val first = Song("1", "one.mp3", "Track", importSourceIdentity = "source", privateAudioFingerprint = "sha")
        val exactCopy = Song("2", "two.mp3", "Track", importSourceIdentity = "source", privateAudioFingerprint = "sha")
        val changedContent = Song("3", "three.mp3", "Track", importSourceIdentity = "source", privateAudioFingerprint = "other")
        val detectedSameFile = mediaSong("4").copy(importSourceIdentity = "source", privateAudioFingerprint = "sha")

        val result = consolidateExactDuplicateSongs(listOf(first, exactCopy, detectedSameFile, changedContent))
        assertEquals(2, result.songs.size)
        assertEquals(first.id, result.songs.first().id)
        assertEquals(null, result.songs.first().contentUri)
    }

    @Test
    fun albumGroupingUsesAlbumArtistAndVolumeScopedAlbumId() {
        val trackA = Song("1", "a.mp3", "A", album = "DYR Part 2", artist = "Lead feat. One",
            albumArtist = "Main Artist", mediaAlbumId = 23, mediaStoreVolume = "external_primary")
        val trackB = Song("2", "b.mp3", "B", album = "DYR Part 2", artist = "Lead feat. Two",
            albumArtist = "Main Artist", mediaAlbumId = 23, mediaStoreVolume = "external_primary")
        val otherVolume = trackB.copy(id = "3", mediaStoreVolume = "ABCD-1234")
        val fallbackA = trackA.copy(mediaAlbumId = null, mediaStoreVolume = null)
        val fallbackB = trackB.copy(mediaAlbumId = null, mediaStoreVolume = null)
        val otherAlbumArtist = fallbackB.copy(albumArtist = "Another Artist")

        assertEquals(trackA.albumKey(), trackB.albumKey())
        assertFalse(trackA.albumKey() == otherVolume.albumKey())
        assertEquals(fallbackA.albumKey(), fallbackB.albumKey())
        assertFalse(fallbackA.albumKey() == otherAlbumArtist.albumKey())
        assertEquals("Main Artist", albumDisplayArtist(listOf(trackA, trackB)))
    }
}
