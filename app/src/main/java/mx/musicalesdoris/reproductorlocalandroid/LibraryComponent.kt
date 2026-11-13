package mx.musicalesdoris.reproductorlocalandroid

/** Stable visual slots shared with the iOS theme architecture. */
enum class LibraryComponent(val key: String) {
    HEADER("header"), SETTINGS_MENU("settingsMenu"), NAVIGATION("navigation"),
    SONG("song"), SONGS_VIEW("songsView"), FAVORITES_VIEW("favoritesView"),
    PLAYLISTS_VIEW("playlistsView"), PLAYLIST("playlist"), PLAYLIST_EDITOR("playlistEditor"),
    PLAYLIST_DETAIL("playlistDetail"), ALBUMS_VIEW("albumsView"), ALBUM("album"),
    ALBUM_DETAIL("albumDetail"), ARTISTS_VIEW("artistsView"), ARTIST("artist"),
    ARTIST_DETAIL("artistDetail"), GENRES_VIEW("genresView"), GENRE("genre"),
    GENRE_DETAIL("genreDetail"), FOLDERS_VIEW("foldersView"), FOLDER("folder"),
    FOLDER_DETAIL("folderDetail"), MINI_PLAYER("miniPlayer"), PLAYER_SCREEN("playerScreen")
}
