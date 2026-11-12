package mx.musicalesdoris.reproductorlocalandroid

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

class AndroidSongImporter(private val context: Context) : SongImporter {
    override fun createSource(uri: Uri): SongImportSource {
        val displayName = context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment ?: "Audio"

        return SongImportSource(displayName) { destination ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            } ?: error("No se pudo abrir el archivo")
        }
    }
}
