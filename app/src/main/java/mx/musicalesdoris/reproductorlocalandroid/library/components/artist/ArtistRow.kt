package mx.musicalesdoris.reproductorlocalandroid.library.components.artist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.LibraryComponent
import mx.musicalesdoris.reproductorlocalandroid.library.ArtistSummary
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkView
import java.io.File

@Composable
fun ArtistRow(artist: ArtistSummary, artwork: File?, theme: PlayerTheme, onClick: () -> Unit) {
    when (theme.variantFor(LibraryComponent.ARTIST)) {
        "soniq" -> SoniqArtistRow(artist, artwork, theme, onClick)
        "aurora" -> AuroraArtistRow(artist, artwork, theme, onClick)
        else -> NeonArtistRow(artist, artwork, theme, onClick)
    }
}

@Composable
internal fun ArtistRowLayout(artist: ArtistSummary, artwork: File?, theme: PlayerTheme, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        ArtworkView(artwork, theme, 58.dp, artist.id, circular = true)
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            Text(artist.name, color = theme.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${artist.albumCount} ${if (artist.albumCount == 1) "álbum" else "álbumes"}",
                color = theme.muted, fontSize = 12.sp)
        }
    }
}
