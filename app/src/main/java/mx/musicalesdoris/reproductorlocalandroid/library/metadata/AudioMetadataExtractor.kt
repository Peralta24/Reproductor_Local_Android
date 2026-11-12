package mx.musicalesdoris.reproductorlocalandroid.library.metadata

import android.media.MediaMetadataRetriever
import java.io.File

internal data class ExtractedAudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val albumArtist: String? = null,
    val album: String? = null,
    val durationSeconds: Double? = null,
    val trackNumber: Int? = null,
    val genre: String? = null,
    val embeddedArtwork: ByteArray? = null
)

internal interface SongMetadataExtractor {
    /** Stable for app-managed imported files unless their bytes/attributes change. */
    fun fingerprint(file: File): String?
    fun extract(file: File): ExtractedAudioMetadata
}

internal interface MetadataRetrieverHandle {
    fun setDataSource(path: String)
    fun extractMetadata(key: Int): String?
    fun getEmbeddedPicture(): ByteArray?
    fun release()
}

internal fun interface MetadataRetrieverFactory {
    fun create(): MetadataRetrieverHandle
}

private class FrameworkMetadataRetrieverHandle(
    private val delegate: MediaMetadataRetriever
) : MetadataRetrieverHandle {
    override fun setDataSource(path: String) = delegate.setDataSource(path)
    override fun extractMetadata(key: Int): String? = delegate.extractMetadata(key)
    override fun getEmbeddedPicture(): ByteArray? = delegate.embeddedPicture
    override fun release() = delegate.release()
}

internal class AndroidSongMetadataExtractor(
    private val retrieverFactory: MetadataRetrieverFactory = MetadataRetrieverFactory {
        FrameworkMetadataRetrieverHandle(MediaMetadataRetriever())
    },
    private val diagnostics: MetadataDiagnosticsReporter = NoopMetadataDiagnosticsReporter
) : SongMetadataExtractor {
    override fun fingerprint(file: File): String? =
        if (file.isFile) "${file.length()}:${file.lastModified()}" else null

    override fun extract(file: File): ExtractedAudioMetadata {
        require(file.isFile) { "El archivo de audio no está disponible" }
        val retriever = retrieverFactory.create()
        try {
            retriever.setDataSource(file.absolutePath)
            fun value(key: Int): String? = try {
                retriever.extractMetadata(key).cleanMetadata()
            } catch (_: RuntimeException) {
                null
            }

            val durationSeconds = value(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
                ?.div(1000.0)

            val artwork = try {
                retriever.getEmbeddedPicture()?.takeIf { it.isNotEmpty() }
            } catch (_: RuntimeException) {
                null
            }

            val metadata = ExtractedAudioMetadata(
                title = value(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = value(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                albumArtist = value(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                album = value(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationSeconds = durationSeconds,
                trackNumber = value(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                    ?.substringBefore('/')
                    ?.trim()
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 },
                genre = value(MediaMetadataRetriever.METADATA_KEY_GENRE),
                embeddedArtwork = artwork
            )
            diagnostics.onExtraction(MetadataExtractionSummary(
                title = metadata.title,
                artist = metadata.artist,
                albumArtist = metadata.albumArtist,
                album = metadata.album,
                genre = metadata.genre,
                durationSeconds = metadata.durationSeconds,
                embeddedArtworkPresent = artwork != null
            ))
            return metadata
        } finally {
            retriever.release()
        }
    }

    private fun String?.cleanMetadata(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}

/** Host-side tests use this implementation to avoid invoking Android framework stubs. */
internal object NoopSongMetadataExtractor : SongMetadataExtractor {
    override fun fingerprint(file: File): String? =
        if (file.isFile) "${file.length()}:${file.lastModified()}" else null

    override fun extract(file: File): ExtractedAudioMetadata = ExtractedAudioMetadata()
}
