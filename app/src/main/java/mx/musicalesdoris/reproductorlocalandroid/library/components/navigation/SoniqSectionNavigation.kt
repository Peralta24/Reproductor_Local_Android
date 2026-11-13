package mx.musicalesdoris.reproductorlocalandroid.library.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.library.LibrarySection

@Composable
fun SoniqSectionNavigation(section: LibrarySection, theme: PlayerTheme, onSelect: (LibrarySection) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFDDEEFF), Color(0xFFC7E0F7))))
        .horizontalScroll(rememberScrollState()).padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        LibrarySection.entries.forEach { item ->
            val selected = item == section
            Text(item.title, color = if (selected) theme.foreground else theme.muted,
                fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.background(if (selected) Brush.verticalGradient(listOf(Color.White.copy(alpha = .85f),
                    Color.White.copy(alpha = .35f))) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)),
                    RoundedCornerShape(12.dp))
                    .border(1.dp, if (selected) Color(0xFF9AB2CC) else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { onSelect(item) }.padding(horizontal = 9.dp, vertical = 12.dp))
        }
    }
}
