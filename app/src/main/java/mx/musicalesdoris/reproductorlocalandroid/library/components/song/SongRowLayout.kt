package mx.musicalesdoris.reproductorlocalandroid.library.components.song

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.R
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkView
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqMonoFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.soniqClickable
import java.io.File

@Composable
internal fun SongRowLayout(song: Song, selected: Boolean, artwork: File?, theme: PlayerTheme,
                           onSelect: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit,
                           destructiveTitle: String, onMoveUp: (() -> Unit)?, onMoveDown: (() -> Unit)?,
                           variant: String, onAddToPlaylist: (() -> Unit)? = null) {
    var expanded by remember { mutableStateOf(false) }
    val soniq = variant == "soniq"
    val shape = RoundedCornerShape(if (variant == "aurora") 24.dp else if (soniq) 13.dp else 14.dp)
    val background = when (variant) {
        "soniq" -> if (selected) Brush.verticalGradient(listOf(Color(0xFFEAF2FB), Color(0xFFC0D8F0)))
            else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
        "aurora" -> Brush.verticalGradient(listOf(theme.surface, theme.surface))
        else -> Brush.verticalGradient(listOf(theme.surface.copy(alpha = .45f), theme.surface.copy(alpha = .45f)))
    }
    Row(Modifier.fillMaxWidth().then(if (soniq) Modifier.height(52.dp) else Modifier)
        .background(background, shape)
        .then(if (soniq && selected) Modifier.border(1.dp, SoniqPalette.border, shape) else Modifier)
        .padding(start = if (soniq) 8.dp else if (variant == "aurora") 14.dp else 10.dp,
            end = if (soniq) 0.dp else if (variant == "aurora") 14.dp else 10.dp,
            top = if (soniq) 0.dp else if (variant == "aurora") 14.dp else 10.dp,
            bottom = if (soniq) 0.dp else if (variant == "aurora") 14.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (soniq) 0.dp else 12.dp)) {
        Row(Modifier.weight(1f).then(if (soniq) Modifier.soniqClickable(shape, onClick = onSelect)
            else Modifier.clickable(onClick = onSelect)), verticalAlignment = Alignment.CenterVertically) {
            ArtworkView(artwork, theme, if (soniq) 42.dp else 48.dp, song.id, circular = variant == "aurora")
            Spacer(Modifier.width(if (soniq) 11.dp else 11.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = if (selected && soniq) SoniqPalette.electricBlue else theme.foreground,
                    fontFamily = if (soniq) SoniqBodyFont else FontFamily.Default,
                    fontSize = if (soniq) 13.sp else 16.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(if (soniq) 2.dp else 3.dp))
                Text(song.subtitle, color = if (soniq) SoniqPalette.secondaryText else theme.muted,
                    fontFamily = if (soniq) SoniqBodyFont else FontFamily.Default,
                    fontSize = if (soniq) 11.sp else 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (soniq) Spacer(Modifier.width(11.dp))
            Text(song.displayDuration(), color = if (soniq) SoniqPalette.metaText else theme.muted,
                fontSize = if (soniq) 10.sp else 12.sp,
                fontFamily = if (soniq) SoniqMonoFont else FontFamily.Monospace,
                maxLines = if (soniq) 1 else Int.MAX_VALUE, softWrap = !soniq,
                modifier = if (soniq) Modifier.width(IntrinsicSize.Max) else Modifier)
            if (soniq) Spacer(Modifier.width(11.dp))
        }
        if (!soniq) Text(if (song.favorite) "♥" else "♡", color = theme.accent, fontSize = 20.sp,
            modifier = Modifier.clickable(onClick = onFavorite).padding(3.dp))
        Box(Modifier.then(if (soniq) Modifier.width(28.dp).height(44.dp) else Modifier),
            contentAlignment = Alignment.Center) {
            if (soniq) Box(Modifier.size(28.dp).soniqClickable(RoundedCornerShape(8.dp)) { expanded = true },
                contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(painterResource(R.drawable.soniq_ellipsis_vertical),
                    contentDescription = "Más opciones", modifier = Modifier.size(18.dp))
            }
            else Text("⋮", color = theme.muted, fontSize = 18.sp,
                modifier = Modifier.clickable { expanded = true }.padding(horizontal = 4.dp, vertical = 8.dp))
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (soniq) {
                    if (onAddToPlaylist != null) DropdownMenuItem(text = { Text("Agregar a…") },
                        leadingIcon = { androidx.compose.foundation.Image(painterResource(R.drawable.soniq_action_add), null, Modifier.size(20.dp)) },
                        colors = MenuDefaults.itemColors(textColor = SoniqPalette.ink),
                        onClick = { expanded = false; onAddToPlaylist() })
                    DropdownMenuItem(text = { Text(if (destructiveTitle == "Eliminar archivo") "Eliminar" else destructiveTitle,
                        color = Color(0xFFA62D47)) },
                        leadingIcon = { androidx.compose.foundation.Image(painterResource(R.drawable.soniq_action_delete), null, Modifier.size(20.dp)) },
                        colors = MenuDefaults.itemColors(textColor = SoniqPalette.ink),
                        onClick = { expanded = false; onDelete() })
                    DropdownMenuItem(enabled = false, text = { Text("Compartir · No disponible") },
                        leadingIcon = { androidx.compose.foundation.Image(painterResource(R.drawable.soniq_action_share), null, Modifier.size(20.dp)) },
                        colors = MenuDefaults.itemColors(textColor = SoniqPalette.ink), onClick = {})
                    if (onMoveUp != null) DropdownMenuItem(text = { Text("Mover arriba") }, onClick = { expanded = false; onMoveUp() })
                    if (onMoveDown != null) DropdownMenuItem(text = { Text("Mover abajo") }, onClick = { expanded = false; onMoveDown() })
                } else {
                    DropdownMenuItem(text = { Text(if (song.favorite) "Quitar de favoritos" else "Agregar a favoritos") },
                        onClick = { expanded = false; onFavorite() })
                    if (onMoveUp != null) DropdownMenuItem(text = { Text("Mover arriba") }, onClick = { expanded = false; onMoveUp() })
                    if (onMoveDown != null) DropdownMenuItem(text = { Text("Mover abajo") }, onClick = { expanded = false; onMoveDown() })
                    DropdownMenuItem(text = { Text(destructiveTitle) }, onClick = { expanded = false; onDelete() })
                }
            }
        }
    }
}

private fun Song.displayDuration(): String {
    val ms = durationMs ?: return "—"
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
