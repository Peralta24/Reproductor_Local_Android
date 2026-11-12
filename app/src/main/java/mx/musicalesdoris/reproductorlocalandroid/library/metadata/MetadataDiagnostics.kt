package mx.musicalesdoris.reproductorlocalandroid.library.metadata

import android.util.Log

internal data class MetadataExtractionSummary(
    val title: String?,
    val artist: String?,
    val albumArtist: String?,
    val album: String?,
    val genre: String?,
    val durationSeconds: Double?,
    val embeddedArtworkPresent: Boolean
)

internal data class ArtworkStorageSummary(
    val embedded: Boolean,
    val saved: Boolean,
    val pathExists: Boolean,
    val decodable: Boolean
)

internal interface MetadataDiagnosticsReporter {
    fun onExtraction(summary: MetadataExtractionSummary)
    fun onExtractionFailure()
    fun onArtwork(summary: ArtworkStorageSummary)
}

internal object NoopMetadataDiagnosticsReporter : MetadataDiagnosticsReporter {
    override fun onExtraction(summary: MetadataExtractionSummary) = Unit
    override fun onExtractionFailure() = Unit
    override fun onArtwork(summary: ArtworkStorageSummary) = Unit
}

internal object AndroidLogMetadataDiagnosticsReporter : MetadataDiagnosticsReporter {
    override fun onExtraction(summary: MetadataExtractionSummary) {
        if (!Log.isLoggable(TAG, Log.DEBUG)) return
        Log.d(TAG, "extracted title=${summary.title ?: "<null>"}, artist=${summary.artist ?: "<null>"}, " +
            "albumArtist=${summary.albumArtist ?: "<null>"}, album=${summary.album ?: "<null>"}, " +
            "genre=${summary.genre ?: "<null>"}, durationSeconds=${summary.durationSeconds}, " +
            "embeddedArtwork=${summary.embeddedArtworkPresent}")
    }

    override fun onArtwork(summary: ArtworkStorageSummary) {
        if (!Log.isLoggable(TAG, Log.DEBUG)) return
        Log.d(TAG, "artwork embedded=${summary.embedded}, saved=${summary.saved}, " +
            "pathExists=${summary.pathExists}, decodable=${summary.decodable}")
    }

    override fun onExtractionFailure() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "extraction failed; retry scheduled")
    }

    private const val TAG = "SONIQMetadata"
}
