package mx.musicalesdoris.reproductorlocalandroid

data class ComponentVariant(val id: String, val name: String)

enum class LibraryComponent(val key: String, val title: String) {
    HEADER("header", "Cabecera"),
    NAVIGATION("navigation", "Barra de navegación"),
    SONG("song", "Canción"),
    PLAYLIST("playlist", "Lista"),
    ALBUM("album", "Álbum"),
    ARTIST("artist", "Artista"),
    MINI_PLAYER("miniPlayer", "Reproductor compacto");

    val availableVariants: List<ComponentVariant>
        get() = if (this == NAVIGATION) {
            listOf(ComponentVariant("neon", "Neón"), ComponentVariant("aurora", "Aurora"),
                ComponentVariant("segmented", "Segmentada"))
        } else {
            listOf(ComponentVariant("neon", "Neón"), ComponentVariant("aurora", "Aurora"))
        }
}
