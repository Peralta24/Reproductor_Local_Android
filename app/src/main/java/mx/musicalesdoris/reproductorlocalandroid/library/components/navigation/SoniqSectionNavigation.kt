package mx.musicalesdoris.reproductorlocalandroid.library.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.library.LibrarySection
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

@Composable
fun SoniqSectionNavigation(section: LibrarySection, theme: PlayerTheme, onSelect: (LibrarySection) -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp)
        .background(Brush.verticalGradient(listOf(SoniqPalette.pageTop, SoniqPalette.pageMiddle)))
        .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        LibrarySection.entries.forEach { item ->
            val selected = item == section
            val shape = RoundedCornerShape(if (selected) 12.dp else 8.dp)
            Box(Modifier.height(44.dp)
                .clip(shape)
                .background(if (selected) Brush.verticalGradient(listOf(
                    androidx.compose.ui.graphics.Color.White, androidx.compose.ui.graphics.Color(0xFFD6E8F8),
                    androidx.compose.ui.graphics.Color(0xFFBFDBF7), SoniqPalette.pageTop))
                    else Brush.verticalGradient(listOf(SoniqPalette.pageTop, SoniqPalette.pageMiddle)), shape)
                .border(if (selected) 1.dp else 0.dp, if (selected) SoniqPalette.border
                    else androidx.compose.ui.graphics.Color.Transparent, shape)
                .clickable { onSelect(item) }.padding(horizontal = 4.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(item.title, color = if (selected) SoniqPalette.ink else SoniqPalette.mutedInk,
                    fontFamily = SoniqBodyFont, fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1, softWrap = false)
            }
        }
    }
}
