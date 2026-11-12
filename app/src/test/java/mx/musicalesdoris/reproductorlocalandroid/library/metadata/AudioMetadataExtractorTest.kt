package mx.musicalesdoris.reproductorlocalandroid.library.metadata

import android.media.MediaMetadataRetriever
import java.io.File
import java.io.IOException
import mx.musicalesdoris.reproductorlocalandroid.JsonSongCatalogRepository
import mx.musicalesdoris.reproductorlocalandroid.SongImportSource
import mx.musicalesdoris.reproductorlocalandroid.SongLibrary
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.artwork.EmbeddedArtworkNormalizer
import mx.musicalesdoris.reproductorlocalandroid.library.artwork.ArtworkFileValidator
import mx.musicalesdoris.reproductorlocalandroid.library.artwork.FileSongArtworkStore
import mx.musicalesdoris.reproductorlocalandroid.library.artwork.SongArtworkStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AudioMetadataExtractorTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun extractsFullMetadataAndReleasesRetriever() {
        val audio = temp.newFile("fake.mp3")
        val values = mapOf(
            MediaMetadataRetriever.METADATA_KEY_TITLE to "Tag title",
            MediaMetadataRetriever.METADATA_KEY_ARTIST to "Artist",
            MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST to "Album artist",
            MediaMetadataRetriever.METADATA_KEY_ALBUM to "Album",
            MediaMetadataRetriever.METADATA_KEY_DURATION to "125000",
            MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER to "3/12",
            MediaMetadataRetriever.METADATA_KEY_GENRE to "Jazz"
        )
        var released = false
        val extractor = AndroidSongMetadataExtractor(MetadataRetrieverFactory {
            FakeRetriever(values, byteArrayOf(1, 2, 3)) { released = true }
        })

        val result = extractor.extract(audio)

        assertEquals("Tag title", result.title)
        assertEquals("Artist", result.artist)
        assertEquals("Album artist", result.albumArtist)
        assertEquals("Album", result.album)
        assertEquals(125.0, result.durationSeconds!!, 0.0)
        assertEquals(3, result.trackNumber)
        assertEquals("Jazz", result.genre)
        assertTrue(result.embeddedArtwork!!.contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(released)
    }

    @Test fun partialAndMalformedMetadataRemainUnknown() {
        val audio = temp.newFile("partial.mp3")
        val extractor = AndroidSongMetadataExtractor(MetadataRetrieverFactory {
            FakeRetriever(mapOf(
                MediaMetadataRetriever.METADATA_KEY_TITLE to "  ",
                MediaMetadataRetriever.METADATA_KEY_DURATION to "unknown",
                MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER to "0"
            ), null)
        })

        val result = extractor.extract(audio)

        assertNull(result.title)
        assertNull(result.artist)
        assertNull(result.durationSeconds)
        assertNull(result.trackNumber)
        assertNull(result.embeddedArtwork)
    }

    @Test fun extractionFailureStillReleasesRetriever() {
        val audio = temp.newFile("broken.mp3")
        var released = false
        val extractor = AndroidSongMetadataExtractor(MetadataRetrieverFactory {
            object : MetadataRetrieverHandle {
                override fun setDataSource(path: String) { throw IOException("bad media") }
                override fun extractMetadata(key: Int): String? = null
                override fun getEmbeddedPicture(): ByteArray? = null
                override fun release() { released = true }
            }
        })

        org.junit.Assert.assertThrows(IOException::class.java) { extractor.extract(audio) }
        assertTrue(released)
    }

    @Test fun importedMetadataAndArtworkPersistWithoutChangingIdentityOrFavorite() {
        val files = temp.newFolder("files")
        val songsDir = File(files, "Songs")
        val repository = JsonSongCatalogRepository(songsDir, File(files, "songs.json"))
        val extractor = CountingExtractor(ExtractedAudioMetadata(
            title = "Embedded title", artist = "Artist", albumArtist = "Album artist", album = "Album", durationSeconds = 91.5,
            trackNumber = 4, genre = "Rock", embeddedArtwork = byteArrayOf(5, 6)
        ))
        val normalizer = EmbeddedArtworkNormalizer { it.copyOf() }
        val artStore = AlwaysReadableArtworkStore(File(files, "AlbumArtwork"))
        val library = SongLibrary(files, repository, metadataExtractor = extractor,
            artworkNormalizer = normalizer, artworkStore = artStore)

        val song = library.importSources(listOf(SongImportSource("fallback.mp3") { it.writeBytes(byteArrayOf(7)) })).single()
        val reread = repository.loadSongs().single()

        assertEquals(song.id, reread.id)
        assertFalse(reread.favorite)
        assertEquals("Embedded title", reread.title)
        assertEquals("Artist", reread.artist)
        assertEquals("Album artist", reread.albumArtist)
        assertEquals("Album", reread.album)
        assertEquals(91.5, reread.duration!!, 0.0)
        assertEquals(4, reread.trackNumber)
        assertEquals("Rock", reread.genre)
        assertNotNull(reread.metadataFingerprint)
        assertNotNull(artStore.fileFor(reread.artworkPath)?.takeIf(File::isFile))
        library.scanMissingMetadata()
        assertEquals("unchanged file should not be extracted twice", 1, extractor.extractCount)
        assertEquals(reread, repository.loadSongs().single())
    }

    @Test fun metadataFailureDoesNotLoseImportedAudioOrInventFields() {
        val files = temp.newFolder("files")
        val repository = JsonSongCatalogRepository(File(files, "Songs"), File(files, "songs.json"))
        val extractor = object : SongMetadataExtractor {
            override fun fingerprint(file: File) = "${file.length()}:${file.lastModified()}"
            override fun extract(file: File): ExtractedAudioMetadata = throw IOException("bad tags")
        }
        val library = SongLibrary(files, repository, metadataExtractor = extractor)

        val song = library.importSources(listOf(SongImportSource("original-name.mp3") { it.writeBytes(byteArrayOf(9)) })).single()

        assertEquals("original-name", song.title)
        assertNull(song.artist)
        assertNull(song.album)
        assertNull(song.genre)
        assertTrue(library.fileFor(song).isFile)
        assertEquals(song.id, repository.loadSongs().single().id)
        assertNotNull(library.error.value)
    }

    @Test fun corruptArtworkIsIgnoredAndReportedWithoutRejectingAudio() {
        val files = temp.newFolder("files")
        val repository = JsonSongCatalogRepository(File(files, "Songs"), File(files, "songs.json"))
        val extractor = CountingExtractor(ExtractedAudioMetadata(
            title = "Tagged", embeddedArtwork = byteArrayOf(0, 1, 2)
        ))
        val library = SongLibrary(
            files, repository,
            metadataExtractor = extractor,
            artworkNormalizer = EmbeddedArtworkNormalizer { null }
        )

        val song = library.importSources(listOf(SongImportSource("tagged.mp3") { it.writeBytes(byteArrayOf(4)) })).single()

        assertNull(song.artworkPath)
        assertTrue(library.fileFor(song).isFile)
        assertNotNull(library.error.value)
        assertEquals(song.id, repository.loadSongs().single().id)
    }

    @Test fun scanningLegacySongPreservesIdFavoriteAndAddsNewMetadata() {
        val files = temp.newFolder("files")
        val songsDir = File(files, "Songs").apply { mkdirs() }
        val audio = File(songsDir, "legacy.mp3").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val repository = JsonSongCatalogRepository(songsDir, File(files, "songs.json"))
        repository.updateSongs { listOf(Song("stable-id", audio.name, "Legacy name", favorite = true)) }
        val extractor = CountingExtractor(ExtractedAudioMetadata(title = "Tagged", artist = "Known"))
        val library = SongLibrary(files, repository, metadataExtractor = extractor)

        library.scanMissingMetadata()
        val result = repository.loadSongs().single()

        assertEquals("stable-id", result.id)
        assertTrue(result.favorite)
        assertEquals("Tagged", result.title)
        assertEquals("Known", result.artist)
    }

    @Test fun incompleteMetadataRetriesAfterBackoffAndKeepsPreviouslyValidValues() {
        val files = temp.newFolder("files")
        val songsDir = File(files, "Songs").apply { mkdirs() }
        val audio = File(songsDir, "legacy.mp3").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val repository = JsonSongCatalogRepository(songsDir, File(files, "songs.json"))
        val coverStore = AlwaysReadableArtworkStore(File(files, "AlbumArtwork"))
        val oldCover = coverStore.save(byteArrayOf(1, 2, 3))
        val currentFingerprint = "${audio.length()}:${audio.lastModified()}"
        repository.updateSongs { listOf(Song(
            "retry-id", audio.name, "Old title", favorite = true,
            artist = "Known artist", artworkPath = oldCover,
            metadataFingerprint = "previous-file-fingerprint"
        )) }
        var now = 100L
        val extractor = object : SongMetadataExtractor {
            var attempts = 0
            override fun fingerprint(file: File) = if (file.isFile) currentFingerprint else null
            override fun extract(file: File): ExtractedAudioMetadata {
                attempts++
                return if (attempts == 1) ExtractedAudioMetadata(title = "New title")
                else ExtractedAudioMetadata(title = "New title", artist = "Known artist", album = "Known album",
                    embeddedArtwork = byteArrayOf(4, 5, 6))
            }
        }
        val library = SongLibrary(files, repository, metadataExtractor = extractor,
            artworkNormalizer = EmbeddedArtworkNormalizer { it.copyOf() }, artworkStore = coverStore,
            clockMillis = { now })

        library.scanMissingMetadata()
        var song = repository.loadSongs().single()
        assertEquals("Known artist", song.artist)
        assertNull(song.album)
        assertEquals(oldCover, song.artworkPath)
        assertEquals(1, song.metadataRetryCount)
        assertEquals(1, extractor.attempts)

        val restartedLibrary = SongLibrary(files, JsonSongCatalogRepository(songsDir, File(files, "songs.json")),
            metadataExtractor = extractor, artworkNormalizer = EmbeddedArtworkNormalizer { it.copyOf() },
            artworkStore = coverStore, clockMillis = { now })
        restartedLibrary.scanMissingMetadata()
        assertEquals(1, extractor.attempts)
        now = requireNotNull(song.metadataRetryAfterMillis)
        restartedLibrary.scanMissingMetadata()
        song = repository.loadSongs().single()
        assertEquals(2, extractor.attempts)
        assertEquals("retry-id", song.id)
        assertTrue(song.favorite)
        assertEquals("Known artist", song.artist)
        assertEquals("Known album", song.album)
        assertEquals(0, song.metadataRetryCount)
        assertNull(song.metadataRetryAfterMillis)
        assertTrue(coverStore.isReadable(song.artworkPath))
    }

    @Test fun nullExtractionDoesNotClearPreviouslyValidArtistOrAlbum() {
        val files = temp.newFolder("files")
        val songsDir = File(files, "Songs").apply { mkdirs() }
        val audio = File(songsDir, "preserve.mp3").apply { writeBytes(byteArrayOf(2, 3, 4)) }
        val repository = JsonSongCatalogRepository(songsDir, File(files, "songs.json"))
        val coverStore = AlwaysReadableArtworkStore(File(files, "AlbumArtwork"))
        val cover = coverStore.save(byteArrayOf(7, 8, 9))
        repository.updateSongs { listOf(Song("keep-meta", audio.name, "Title", artist = "Valid artist",
            album = "Valid album", artworkPath = cover, metadataFingerprint = "previous")) }
        val extractor = CountingExtractor(ExtractedAudioMetadata())
        val library = SongLibrary(files, repository, metadataExtractor = extractor, artworkStore = coverStore)

        library.scanMissingMetadata()
        val result = repository.loadSongs().single()

        assertEquals("Valid artist", result.artist)
        assertEquals("Valid album", result.album)
        assertEquals(cover, result.artworkPath)
    }

    @Test fun identicalArtworkIsDeduplicatedAndRetainedUntilLastSongReference() {
        val files = temp.newFolder("files")
        val store = FileSongArtworkStore(File(files, "AlbumArtwork"), ArtworkFileValidator { it.isFile })
        val firstPath = store.save(byteArrayOf(1, 2, 3))
        val secondPath = store.save(byteArrayOf(1, 2, 3))
        assertEquals(firstPath, secondPath)
        val artwork = store.fileFor(firstPath)!!
        assertTrue(artwork.isFile)
        val oneReference = mx.musicalesdoris.reproductorlocalandroid.Song("1", "1.mp3", "One", artworkPath = firstPath)
        val twoReferences = oneReference.copy(id = "2", fileName = "2.mp3")

        store.deleteIfUnreferenced(firstPath, listOf(twoReferences))
        assertTrue(artwork.isFile)
        store.deleteIfUnreferenced(firstPath, emptyList())
        assertFalse(artwork.exists())
    }

    @Test fun artworkStoreRejectsMissingOrUndecodableCoverFiles() {
        val files = temp.newFolder("files")
        val validator = ArtworkFileValidator { file -> file.isFile && file.readBytes().contentEquals(byteArrayOf(4, 5, 6)) }
        val store = FileSongArtworkStore(File(files, "AlbumArtwork"), validator)
        val readablePath = store.save(byteArrayOf(4, 5, 6))
        val corruptPath = store.save(byteArrayOf(1, 2, 3))

        assertTrue(store.isReadable(readablePath))
        assertFalse(store.isReadable(corruptPath))
        assertFalse(store.isReadable("AlbumArtwork/${"f".repeat(64)}.jpg"))
    }

    @Test fun savingArtworkRepairsCorruptExistingContentAddressedFile() {
        val files = temp.newFolder("files")
        val expected = byteArrayOf(4, 5, 6)
        val validator = ArtworkFileValidator { file -> file.isFile && file.readBytes().contentEquals(expected) }
        val store = FileSongArtworkStore(File(files, "AlbumArtwork"), validator)
        val relativePath = store.save(expected)
        val target = store.fileFor(relativePath)!!
        target.writeBytes(byteArrayOf(1, 2, 3))
        assertFalse(store.isReadable(relativePath))

        store.save(expected)

        assertTrue(store.isReadable(relativePath))
        assertEquals(expected.toList(), target.readBytes().toList())
    }

    private class FakeRetriever(
        private val values: Map<Int, String>,
        private val picture: ByteArray?,
        private val onRelease: () -> Unit = {}
    ) : MetadataRetrieverHandle {
        override fun setDataSource(path: String) = Unit
        override fun extractMetadata(key: Int): String? = values[key]
        override fun getEmbeddedPicture(): ByteArray? = picture
        override fun release() = onRelease()
    }

    private class CountingExtractor(private val metadata: ExtractedAudioMetadata) : SongMetadataExtractor {
        var extractCount = 0
        override fun fingerprint(file: File): String? = if (file.isFile) "${file.length()}:${file.lastModified()}" else null
        override fun extract(file: File): ExtractedAudioMetadata { extractCount++; return metadata }
    }

    private class AlwaysReadableArtworkStore(directory: File) : SongArtworkStore {
        private val delegate = FileSongArtworkStore(directory)
        override fun save(normalizedJpeg: ByteArray) = delegate.save(normalizedJpeg)
        override fun fileFor(relativePath: String?) = delegate.fileFor(relativePath)
        override fun isReadable(relativePath: String?) = fileFor(relativePath)?.isFile == true
        override fun deleteIfUnreferenced(relativePath: String?, songs: List<Song>) =
            delegate.deleteIfUnreferenced(relativePath, songs)
    }
}
