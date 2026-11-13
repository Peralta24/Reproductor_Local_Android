package mx.musicalesdoris.reproductorlocalandroid

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import mx.musicalesdoris.reproductorlocalandroid.library.Playlist
import org.junit.Assert.*
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistCatalogRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun missingCatalogStartsEmptyAndCrudPersistsStableIdsAndOrder() {
        val file = File(temporary.root, "playlists.json")
        val repository = JsonPlaylistCatalogRepository(file)
        assertTrue(repository.load().isEmpty())
        val playlist = repository.create("  Road trip  ")
        repository.addSongs(playlist.id, listOf("b", "a", "b", ""))
        val renamed = repository.rename(playlist.id, "Drive")
        assertEquals("Drive", renamed.title)
        assertEquals(listOf("b", "a"), renamed.songIds)
        assertEquals(renamed, JsonPlaylistCatalogRepository(file).load().single())
        repository.removeSong(playlist.id, "b")
        assertEquals(listOf("a"), repository.load().single().songIds)
        assertTrue(repository.delete(playlist.id))
        assertTrue(repository.load().isEmpty())
    }

    @Test fun titlesTrimAndRejectBlankAndDuplicateNamesRemainAllowed() {
        val repository = JsonPlaylistCatalogRepository(File(temporary.root, "playlists.json"))
        val one = repository.create(" Same ")
        val two = repository.create("Same")
        assertNotEquals(one.id, two.id)
        assertEquals("Same", one.title)
        assertEquals(2, repository.load().size)
        try { repository.create("   "); fail("blank title accepted") } catch (_: IllegalArgumentException) { }
    }

    @Test fun unavailableSongIdsAreKeptInPlaylistAndNotDuplicated() {
        val repository = JsonPlaylistCatalogRepository(File(temporary.root, "playlists.json"))
        val playlist = repository.create("Saved")
        repository.addSongs(playlist.id, listOf("missing", "present", "missing"))
        assertEquals(listOf("missing", "present"), repository.load().single().songIds)
    }

    @Test fun legacyPlaylistJsonLoadsAndNewDescriptionAndCoverSurviveRestart() {
        val file = File(temporary.root, "playlists.json")
        file.writeText("""[{"id":"legacy-id","title":"Antigua","songIds":["b","a"],"createdAtMillis":10,"updatedAtMillis":11}]""")
        val repository = JsonPlaylistCatalogRepository(file)
        val legacy = repository.load().single()
        assertNull(legacy.description)
        assertNull(legacy.artworkPath)
        val edited = repository.updateDetails(legacy.id, legacy.title, "Para la noche", imageBytes(), false)
        assertEquals("legacy-id", edited.id)
        assertEquals(listOf("b", "a"), edited.songIds)
        assertEquals("Para la noche", edited.description)
        assertTrue(PlaylistArtworkStore(temporary.root).fileFor(edited.artworkPath)!!.isFile)
        val restored = JsonPlaylistCatalogRepository(file).load().single()
        assertEquals(edited, restored)
    }

    @Test fun removingCoverKeepsPlaylistAndDeletesOnlyUnreferencedGeneratedImage() {
        val file = File(temporary.root, "playlists.json")
        val repository = JsonPlaylistCatalogRepository(file)
        val one = repository.create("Uno", artworkBytes = imageBytes())
        val two = repository.create("Dos", artworkBytes = imageBytes())
        assertEquals(one.artworkPath, two.artworkPath)
        val sharedFile = PlaylistArtworkStore(temporary.root).fileFor(one.artworkPath)!!
        assertTrue(repository.updateDetails(one.id, one.title, one.description, null, true).artworkPath == null)
        assertTrue(sharedFile.isFile)
        repository.updateDetails(two.id, two.title, two.description, null, true)
        assertFalse(sharedFile.exists())
        assertEquals(2, repository.load().size)
    }

    @Test fun deletingPlaylistsCollectsOnlyCoverFilesWithNoRemainingReferences() {
        val repository = JsonPlaylistCatalogRepository(File(temporary.root, "playlists.json"))
        val first = repository.create("Uno", artworkBytes = imageBytes())
        val second = repository.create("Dos", artworkBytes = imageBytes())
        val cover = PlaylistArtworkStore(temporary.root).fileFor(first.artworkPath)!!
        assertTrue(repository.delete(first.id))
        assertTrue(cover.isFile)
        assertTrue(repository.delete(second.id))
        assertFalse(cover.exists())
    }

    @Test fun missingCoverFileDoesNotInvalidatePlaylistMetadata() {
        val file = File(temporary.root, "playlists.json")
        val repository = JsonPlaylistCatalogRepository(file)
        val playlist = repository.create("Cover", artworkBytes = imageBytes())
        val cover = PlaylistArtworkStore(temporary.root).fileFor(playlist.artworkPath)!!
        assertTrue(cover.delete())
        val restored = JsonPlaylistCatalogRepository(file).load().single()
        assertEquals(playlist.artworkPath, restored.artworkPath)
        val missingCoverPath = PlaylistArtworkStore.pathFor(temporary.root, restored.artworkPath)
        assertNotNull(missingCoverPath)
        assertFalse(missingCoverPath!!.isFile)
    }

    @Test fun validatedArtworkPathCanBeResolvedWithoutReadingTheImageFile() {
        val relative = "playlist-artwork/${"a".repeat(64)}.jpg"
        val path = PlaylistArtworkStore.pathFor(temporary.root, relative)
        assertEquals(File(temporary.root, relative).absolutePath, path?.absolutePath)
        assertFalse(path!!.exists())
        assertNull(PlaylistArtworkStore.pathFor(temporary.root, "../outside.jpg"))
    }

    @Test fun corruptImageIsRejectedWithoutChangingExistingPlaylistData() {
        val file = File(temporary.root, "playlists.json")
        val repository = JsonPlaylistCatalogRepository(file)
        val playlist = repository.create("Intacta", "Descripción")
        val original = file.readBytes()
        try {
            repository.updateDetails(playlist.id, "Alterada", "Nuevo", byteArrayOf(), false)
            fail("invalid image should be rejected")
        } catch (_: java.io.IOException) { }
        assertArrayEquals(original, file.readBytes())
        assertEquals(playlist, repository.load().single())
    }

    @Test fun coverFailureDuringCatalogReplaceKeepsOldJsonAndOldArtworkReference() {
        val file = File(temporary.root, "playlists.json")
        val store = PlaylistArtworkStore(temporary.root)
        val base = JsonPlaylistCatalogRepository(file)
        val created = base.create("Before", artworkBytes = imageBytes())
        val originalJson = file.readBytes()
        val originalCover = created.artworkPath
        val failing = JsonPlaylistCatalogRepository.withAtomicMover(file,
            PlaylistCatalogAtomicMover { _, _ -> throw AtomicMoveNotSupportedException("tmp", "target", "test") },
            artworkStore = store)
        try {
            failing.updateDetails(created.id, "After", "New description", imageBytes(0xff00ff00.toInt()), false)
            fail("catalog replacement should fail")
        } catch (_: AtomicPlaylistReplacementUnavailableException) { }
        assertArrayEquals(originalJson, file.readBytes())
        assertEquals(created, base.load().single())
        assertNotNull(store.fileFor(originalCover))
    }

    @Test fun explicitRecoveryPreservesExactUnreadableBytesBeforeReset() {
        val file = File(temporary.root, "playlists.json").apply { writeBytes(byteArrayOf(0x7b, 0x78, 0x00, 0x7d)) }
        val original = file.readBytes()
        val repository = JsonPlaylistCatalogRepository(file)
        val backup = repository.recoverUnreadableCatalog()
        assertArrayEquals(original, backup.readBytes())
        assertTrue(repository.load().isEmpty())
    }

    @Test fun failedEmptyCatalogCommitAfterVerifiedBackupKeepsOriginalAndBackup() {
        val file = File(temporary.root, "playlists.json").apply { writeText("{broken-original") }
        val original = file.readBytes()
        val repository = JsonPlaylistCatalogRepository.withAtomicMover(file,
            PlaylistCatalogAtomicMover { _, _ -> throw AtomicMoveNotSupportedException("tmp", "backup", "test") })
        try { repository.recoverUnreadableCatalog(); fail("recovery should stop if backup cannot be committed") }
        catch (_: AtomicPlaylistReplacementUnavailableException) { }
        assertArrayEquals(original, file.readBytes())
        val backup = temporary.root.listFiles()!!.single { it.name.endsWith(".bak") }
        assertArrayEquals(original, backup.readBytes())
    }

    @Test fun unreadableCatalogBlocksMutationAndPreservesOriginalBytes() {
        val file = File(temporary.root, "playlists.json").apply { writeText("{broken") }
        val original = file.readBytes()
        val repository = JsonPlaylistCatalogRepository(file)
        try { repository.create("No overwrite"); fail("mutation should fail") }
        catch (_: PlaylistCatalogReadException) { }
        assertArrayEquals(original, file.readBytes())
        assertEquals(0, temporary.root.listFiles()!!.count { it.extension == "tmp" })
    }

    @Test fun failedAtomicReplacementPreservesPriorCatalogAndCleansTemporaryFile() {
        val file = File(temporary.root, "playlists.json")
        val working = JsonPlaylistCatalogRepository(file)
        working.create("Before")
        val original = file.readBytes()
        val failing = JsonPlaylistCatalogRepository.withAtomicMover(file,
            PlaylistCatalogAtomicMover { _, _ -> throw AtomicMoveNotSupportedException("tmp", "target", "test") })
        try { failing.create("After"); fail("write should fail") }
        catch (_: AtomicPlaylistReplacementUnavailableException) { }
        assertArrayEquals(original, file.readBytes())
        assertEquals(listOf("Before"), working.load().map(Playlist::title))
        assertEquals(0, temporary.root.listFiles()!!.count { it.name.endsWith(".tmp") })
    }

    @Test fun separateRepositoryInstancesSerializeConcurrentAddsWithoutLostUpdates() {
        val file = File(temporary.root, "playlists.json")
        val setup = JsonPlaylistCatalogRepository(file)
        val playlist = setup.create("Concurrent")
        val first = JsonPlaylistCatalogRepository(file)
        val second = JsonPlaylistCatalogRepository(file)
        val start = CountDownLatch(1)
        val ready = CountDownLatch(2)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val a = pool.submit { ready.countDown(); start.await(); first.addSongs(playlist.id, listOf("a")) }
            val b = pool.submit { ready.countDown(); start.await(); second.addSongs(playlist.id, listOf("b")) }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            a.get(5, TimeUnit.SECONDS); b.get(5, TimeUnit.SECONDS)
        } finally { pool.shutdownNow() }
        assertEquals(setOf("a", "b"), JsonPlaylistCatalogRepository(file).load().single().songIds.toSet())
    }

    private fun imageBytes(color: Int = 0xffff0000.toInt()): ByteArray {
        val bitmap = Bitmap.createBitmap(20, 12, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        return try { ByteArrayOutputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); output.toByteArray() } }
        finally { bitmap.recycle() }
    }
}
