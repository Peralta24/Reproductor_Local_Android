package mx.musicalesdoris.reproductorlocalandroid.library.components.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.LibraryComponent
import mx.musicalesdoris.reproductorlocalandroid.library.AlbumSummary
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkView
import java.io.File

@Composable
fun AlbumCard(album: AlbumSummary, artwork: File?, theme: PlayerTheme, onClick: () -> Unit) {
    when (theme.variantFor(LibraryComponent.ALBUM)) {
        "soniq" -> SoniqAlbumCard(album, artwork, theme, onClick)
        "aurora" -> AuroraAlbumCard(album, artwork, theme, onClick)
        else -> NeonAlbumCard(album, artwork, theme, onClick)
    }
}

@Composable
internal fun AlbumCardLayout(album: AlbumSummary, artwork: File?, theme: PlayerTheme,
                             onClick: () -> Unit, soniq: Boolean) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            ArtworkView(artwork, theme, maxWidth, album.id)
        }
        Spacer(Modifier.height(8.dp))
        Text(album.title, color = theme.foreground, fontSize = if (soniq) 14.sp else 16.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(album.artist, color = theme.muted, fontSize = if (soniq) 11.sp else 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${album.songCount} ${if (album.songCount == 1) "canción" else "canciones"}",
            color = theme.muted, fontSize = if (soniq) 9.sp else 12.sp)
    }
}
