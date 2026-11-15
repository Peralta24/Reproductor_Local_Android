package mx.musicalesdoris.reproductorlocalandroid.library.sections

/** Returns the first position for each ID, matching List.indexOf semantics. */
internal fun firstPlaylistSongPositions(songIds: List<String>): Map<String, Int> =
    buildMap(songIds.size) {
        songIds.forEachIndexed { index, id ->
            if (id !in this) put(id, index)
        }
    }
