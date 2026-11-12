package mx.musicalesdoris.reproductorlocalandroid

import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SongCatalogRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var filesDirectory: File
    private lateinit var songsDirectory: File
    private lateinit var catalogFile: File
    private lateinit var repository: JsonSongCatalogRepository

    @Before
    fun setUp() {
        filesDirectory = temporaryFolder.newFolder("app-files")
        songsDirectory = File(filesDirectory, "Songs").apply { mkdirs() }
        catalogFile = File(filesDirectory, "songs.json")
        repository = JsonSongCatalogRepository(songsDirectory, catalogFile)
    }

    @Test
    fun readsExistingCatalogFormat() {
        val audioFile = File(songsDirectory, "existing.mp3").apply { writeBytes(byteArrayOf(1)) }
        catalogFile.writeText(
            """[{"id":"song-1","fileName":"${audioFile.name}","title":"Existing","favorite":true}]"""
        )

        val song = repository.loadSongs().single()

        assertEquals("song-1", song.id)
        assertEquals("Existing", song.title)
        assertTrue(song.favorite)
        assertNull(song.duration)
        assertNull(song.artist)
        assertNull(song.albumArtist)
        assertNull(song.album)
        assertNull(song.trackNumber)
        assertNull(song.artworkPath)
        assertNull(song.metadataRetryAfterMillis)
        assertEquals(0, song.metadataRetryCount)
    }

    @Test
    fun readsSongWhenOptionalFieldsAndFavoriteAreAbsent() {
        File(songsDirectory, "legacy.m4a").writeBytes(byteArrayOf(1))
        catalogFile.writeText("""[{"id":"legacy-id","fileName":"legacy.m4a","title":"Legacy"}]""")

        val song = repository.loadSongs().single()

        assertEquals("legacy-id", song.id)
        assertFalse(song.favorite)
        assertNull(song.duration)
        assertNull(song.artist)
        assertNull(song.albumArtist)
        assertNull(song.album)
        assertNull(song.trackNumber)
        assertNull(song.artworkPath)
        assertNull(song.metadataRetryAfterMillis)
        assertEquals(0, song.metadataRetryCount)
    }

    @Test
    fun missingAudioRecordAndMetadataSurviveLaterCatalogUpdates() {
        val missing = Song(
            id = "temporarily-missing",
            fileName = "temporarily-missing.mp3",
            title = "Keep this record",
            favorite = true,
            duration = 123.0,
            artist = "Preserved artist",
            album = "Preserved album",
            trackNumber = 7,
            artworkPath = "AlbumArtwork/kept.jpg"
        )
        repository.updateSongs { listOf(missing) }
        val library = SongLibrary(filesDirectory, repository)

        assertEquals(listOf(missing), library.songs.value)
        assertFalse(library.isAvailable(missing))
        library.toggleFavorite(missing.id)

        val inMemory = library.songs.value.single()
        val persisted = repository.loadSongs().single()
        assertFalse(inMemory.favorite)
        assertEquals("Preserved artist", persisted.artist)
        assertEquals("Preserved album", persisted.album)
        assertEquals(7, persisted.trackNumber)
        assertEquals("AlbumArtwork/kept.jpg", persisted.artworkPath)
        assertEquals(missing.id, persisted.id)
    }

    @Test
    fun savesAndReloadsOptionalFieldsAndKeepsIdentifier() {
        val song = Song(
            id = "stable-id",
            fileName = "track.mp3",
            title = "Track",
            favorite = true,
            duration = 183.5,
            artist = "Artist",
            albumArtist = "Album artist",
            album = "Album",
            trackNumber = 4,
            artworkPath = "AlbumArtwork/cover.jpg",
            genre = "Rock",
            metadataFingerprint = "3:1000",
            metadataRetryAfterMillis = 5000L,
            metadataRetryCount = 2
        )
        File(songsDirectory, song.fileName).writeBytes(byteArrayOf(1))

        repository.updateSongs { listOf(song) }
        val restored = repository.loadSongs().single()

        assertEquals(song, restored)
        assertEquals("stable-id", restored.id)
    }

    @Test
    fun savesAndRestoresFavoriteChanges() {
        val song = Song(id = "favorite-id", fileName = "favorite.mp3", title = "Favorite")
        File(songsDirectory, song.fileName).writeBytes(byteArrayOf(1))
        repository.updateSongs { listOf(song) }
        val library = SongLibrary(filesDirectory, repository)

        library.toggleFavorite(song.id)

        assertTrue(library.songs.value.single().favorite)
        assertTrue(repository.loadSongs().single().favorite)
    }

    @Test
    fun deleteRemovesRecordFromPersistedJson() {
        val song = Song(id = "delete-id", fileName = "delete.mp3", title = "Delete")
        val audioFile = File(songsDirectory, song.fileName).apply { writeBytes(byteArrayOf(1)) }
        repository.updateSongs { listOf(song) }
        val library = SongLibrary(filesDirectory, repository)

        assertTrue(library.delete(song))

        assertFalse(audioFile.exists())
        val persisted = JSONArray(catalogFile.readText())
        assertEquals(0, persisted.length())
        assertTrue(library.songs.value.isEmpty())
    }

    @Test
    fun importsSourceAndPersistsSong() {
        val library = SongLibrary(filesDirectory, repository)

        val imported = library.importSources(listOf(
            SongImportSource("New track.mp3") { destination -> destination.writeBytes(byteArrayOf(7, 8, 9)) }
        )).single()

        assertEquals("New track", imported.title)
        assertTrue(imported.fileName.endsWith(".mp3"))
        assertTrue(File(songsDirectory, imported.fileName).isFile)
        assertEquals(imported, repository.loadSongs().single())
    }

    @Test
    fun importPersistenceFailureCleansCopiedAudioFile() {
        val existing = Song("existing-id", "existing.mp3", "Existing", favorite = true)
        File(songsDirectory, existing.fileName).writeBytes(byteArrayOf(1))
        repository.updateSongs { listOf(existing) }
        val failingRepository = FailingUpdateRepository(repository)
        val library = SongLibrary(filesDirectory, failingRepository)

        val imported = library.importSources(listOf(
            SongImportSource("Uncommitted.mp3") { destination -> destination.writeBytes(byteArrayOf(3, 4)) }
        ))

        assertTrue(imported.isEmpty())
        assertEquals(listOf(existing.fileName), songsDirectory.listFiles().orEmpty().map { it.name })
        assertEquals(listOf(existing), library.songs.value)
        assertNotNull(library.error.value)
        assertEquals(listOf(existing), repository.loadSongs())
    }

    @Test
    fun copyFailureCleansPartialFileAndKeepsExistingCatalog() {
        val existing = Song("existing-id", "existing.mp3", "Existing", favorite = true)
        File(songsDirectory, existing.fileName).writeBytes(byteArrayOf(1))
        repository.updateSongs { listOf(existing) }
        val library = SongLibrary(filesDirectory, repository)

        val imported = library.importSources(listOf(
            SongImportSource("Partial.mp3") { destination ->
                destination.writeBytes(byteArrayOf(4, 5))
                throw IOException("Source read failed after partial copy")
            }
        ))

        assertTrue(imported.isEmpty())
        assertEquals(listOf(existing.fileName), songsDirectory.listFiles().orEmpty().map { it.name })
        assertEquals(listOf(existing), library.songs.value)
        assertEquals(listOf(existing), repository.loadSongs())
        assertTrue(library.error.value.orEmpty().contains("Partial.mp3"))
    }

    @Test
    fun malformedCatalogRaisesReadFailure() {
        val original = "[{broken-json".toByteArray()
        catalogFile.writeBytes(original)

        val failure = org.junit.Assert.assertThrows(CatalogReadException::class.java) {
            repository.loadSongs()
        }

        assertNotNull(failure.cause)
        assertArrayEquals(original, catalogFile.readBytes())
    }

    @Test
    fun unreadableCatalogBlocksMutationsAndPreservesOriginal() {
        val original = "[{broken-json".toByteArray()
        catalogFile.writeBytes(original)
        val existingAudio = File(songsDirectory, "keep.mp3").apply { writeBytes(byteArrayOf(9)) }
        val library = SongLibrary(filesDirectory, repository)

        assertTrue(library.catalogNeedsRecovery.value)
        assertNotNull(library.error.value)
        library.toggleFavorite("keep-id")
        assertFalse(library.delete(Song("keep-id", existingAudio.name, "Keep")))
        val imported = library.importSources(listOf(
            SongImportSource("blocked.mp3") { destination -> destination.writeBytes(byteArrayOf(5)) }
        ))

        assertTrue(imported.isEmpty())
        assertTrue(existingAudio.exists())
        assertEquals(listOf(existingAudio), songsDirectory.listFiles()?.toList())
        assertArrayEquals(original, catalogFile.readBytes())
        assertTrue(library.catalogNeedsRecovery.value)
    }

    @Test
    fun explicitRecoveryKeepsExactBackupBeforeStartingEmptyCatalog() {
        val original = "[{broken-json".toByteArray()
        catalogFile.writeBytes(original)
        val library = SongLibrary(filesDirectory, repository)

        assertTrue(library.recoverUnreadableCatalog())

        val backup = filesDirectory.listFiles().orEmpty().single { it.name.startsWith("songs.json.corrupt-") }
        assertArrayEquals(original, backup.readBytes())
        assertEquals("[]", catalogFile.readText())
        assertFalse(library.catalogNeedsRecovery.value)
        assertTrue(library.songs.value.isEmpty())
        assertTrue(library.recoveryMessage.value.orEmpty().contains(backup.name))
    }

    @Test
    fun backupCopyFailureKeepsOriginalAndRemovesPartialBackupTemporary() {
        val original = "[{broken-json".toByteArray()
        catalogFile.writeBytes(original)
        val failingRepository = JsonSongCatalogRepository.withBackupCopier(
            songsDirectory,
            catalogFile,
            CatalogBackupCopier { _, destination ->
                destination.writeBytes(byteArrayOf(1, 2, 3))
                throw IOException("Simulated backup copy failure")
            }
        )
        val library = SongLibrary(filesDirectory, failingRepository)

        assertFalse(library.recoverUnreadableCatalog())

        assertArrayEquals(original, catalogFile.readBytes())
        assertTrue(library.catalogNeedsRecovery.value)
        assertTrue(library.error.value.orEmpty().contains("No se pudo recuperar"))
        assertTrue(filesDirectory.listFiles().orEmpty().none { it.name.startsWith("songs.json.corrupt-") })
    }

    @Test
    fun failedEmptyCatalogCommitAfterVerifiedBackupKeepsOriginalAndBackup() {
        val original = "[{broken-json".toByteArray()
        catalogFile.writeBytes(original)
        val failingRepository = JsonSongCatalogRepository.withAtomicMover(
            songsDirectory,
            catalogFile,
            CatalogAtomicMover { _, _ -> throw IOException("Simulated empty catalog commit failure") }
        )
        val library = SongLibrary(filesDirectory, failingRepository)

        assertFalse(library.recoverUnreadableCatalog())

        assertArrayEquals(original, catalogFile.readBytes())
        val backup = filesDirectory.listFiles().orEmpty().single { it.name.startsWith("songs.json.corrupt-") }
        assertTrue(backup.name.endsWith(".bak"))
        assertArrayEquals(original, backup.readBytes())
        assertTrue(library.catalogNeedsRecovery.value)
        assertNotNull(library.error.value)
    }

    @Test
    fun failedAtomicReplacementLeavesPreviousCatalogUntouched() {
        val existing = Song("original-id", "original.mp3", "Original", favorite = true)
        File(songsDirectory, existing.fileName).writeBytes(byteArrayOf(1))
        repository.updateSongs { listOf(existing) }
        val originalBytes = catalogFile.readBytes()
        val failingRepository = JsonSongCatalogRepository.withAtomicMover(
            songsDirectory,
            catalogFile,
            CatalogAtomicMover { temporary, target ->
                throw AtomicMoveNotSupportedException(temporary.path, target.path, "test filesystem")
            }
        )

        org.junit.Assert.assertThrows(AtomicCatalogReplacementUnavailableException::class.java) {
            failingRepository.updateSongs { current -> current + Song("new-id", "new.mp3", "New") }
        }

        assertArrayEquals(originalBytes, catalogFile.readBytes())
        assertEquals(listOf(existing), repository.loadSongs())
        assertTrue(filesDirectory.listFiles().orEmpty().none { it.name.endsWith(".tmp") })
    }

    @Test
    fun audioDeleteFailureLeavesCatalogAndMemoryUntouchedAndReportsError() {
        val song = Song("keep-on-delete-failure", "keep.mp3", "Keep")
        val audioFile = File(songsDirectory, song.fileName).apply { writeBytes(byteArrayOf(1)) }
        repository.updateSongs { listOf(song) }
        val originalCatalog = catalogFile.readBytes()
        val fileOperations = object : SongFileOperations {
            override fun isFile(file: File): Boolean = file.isFile
            override fun deleteIfExists(file: File): Boolean = throw IOException("Simulated delete failure")
        }
        val library = SongLibrary(filesDirectory, repository, songFileOperations = fileOperations)

        assertFalse(library.delete(song))

        assertTrue(audioFile.isFile)
        assertArrayEquals(originalCatalog, catalogFile.readBytes())
        assertEquals(listOf(song), repository.loadSongs())
        assertEquals(listOf(song), library.songs.value)
        assertTrue(library.error.value.orEmpty().contains("Simulated delete failure"))
    }

    @Test
    fun catalogWriteFailureAfterAudioDeleteKeepsRecordAndReportsRecoverableUnavailableSong() {
        val song = Song("recoverable-delete", "recoverable.mp3", "Recoverable")
        val audioFile = File(songsDirectory, song.fileName).apply { writeBytes(byteArrayOf(2)) }
        repository.updateSongs { listOf(song) }
        val originalCatalog = catalogFile.readBytes()
        val failingRepository = JsonSongCatalogRepository.withAtomicMover(
            songsDirectory,
            catalogFile,
            CatalogAtomicMover { _, _ -> throw IOException("Simulated catalog commit failure") }
        )
        val library = SongLibrary(filesDirectory, failingRepository)

        assertFalse(library.delete(song))

        assertFalse(audioFile.exists())
        assertArrayEquals(originalCatalog, catalogFile.readBytes())
        assertEquals(listOf(song), failingRepository.loadSongs())
        assertEquals(listOf(song), library.songs.value)
        assertFalse(library.isAvailable(library.songs.value.single()))
        assertTrue(library.error.value.orEmpty().contains("permanece en la biblioteca como no disponible"))
    }

    @Test
    fun concurrentRepositoryUpdatesDoNotOverwriteSongs() {
        val repository2 = JsonSongCatalogRepository(songsDirectory, catalogFile)
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        val tasks = (0 until 24).map { index ->
            File(songsDirectory, "concurrent-$index.mp3").writeBytes(byteArrayOf(index.toByte()))
            executor.submit {
                assertTrue(start.await(10, TimeUnit.SECONDS))
                val targetRepository = if (index % 2 == 0) repository else repository2
                val song = Song("concurrent-$index", "concurrent-$index.mp3", "Concurrent $index")
                targetRepository.updateSongs { current -> current + song }
            }
        }
        start.countDown()
        try {
            tasks.forEach { it.get(20, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        assertEquals(24, repository.loadSongs().size)
        assertEquals(24, repository.loadSongs().map { it.id }.toSet().size)
    }

    @Test
    fun concurrentFavoritesAndImportsPreserveBothKindsOfUpdates() {
        val favorites = (0 until 12).map { index ->
            Song("favorite-$index", "favorite-$index.mp3", "Favorite $index")
                .also { File(songsDirectory, it.fileName).writeBytes(byteArrayOf(index.toByte())) }
        }
        repository.updateSongs { favorites }
        val library = SongLibrary(filesDirectory, repository)
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        val tasks = mutableListOf<java.util.concurrent.Future<*>>()
        favorites.forEach { song ->
            tasks += executor.submit {
                assertTrue(start.await(10, TimeUnit.SECONDS))
                library.toggleFavorite(song.id)
            }
        }
        (0 until 12).forEach { index ->
            tasks += executor.submit {
                assertTrue(start.await(10, TimeUnit.SECONDS))
                library.importSources(listOf(
                    SongImportSource("import-$index.mp3") { destination ->
                        destination.writeBytes(byteArrayOf(index.toByte()))
                    }
                ))
            }
        }
        start.countDown()
        try {
            tasks.forEach { it.get(20, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        val stored = repository.loadSongs()
        assertEquals(24, stored.size)
        assertTrue(favorites.all { favorite -> stored.single { it.id == favorite.id }.favorite })
        assertEquals(12, stored.count { it.title.startsWith("import-") })
    }

    private class FailingUpdateRepository(private val delegate: SongCatalogRepository) : SongCatalogRepository {
        override fun loadSongs(): List<Song> = delegate.loadSongs()

        override fun updateSongs(transform: (List<Song>) -> List<Song>): List<Song> {
            throw IOException("Simulated catalog write failure")
        }

        override fun recoverUnreadableCatalog(): File = delegate.recoverUnreadableCatalog()
    }
}
