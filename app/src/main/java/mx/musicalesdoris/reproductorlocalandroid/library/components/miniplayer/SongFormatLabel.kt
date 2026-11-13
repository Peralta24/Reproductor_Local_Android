package mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer

/** Labels the file/container extension we can verify; it does not claim the encoded codec. */
internal fun songFormatLabel(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
    "mp3" -> "MP3"
    "flac" -> "FLAC"
    "wav", "wave" -> "WAV"
    "aac" -> "AAC"
    "ogg", "oga" -> "OGG"
    "m4a" -> "M4A"
    "opus" -> "OPUS"
    "wma" -> "WMA"
    "aif", "aiff" -> "AIFF"
    "amr" -> "AMR"
    else -> "AUDIO"
}
