package mx.musicalesdoris.reproductorlocalandroid.library.components.navigation

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NeonSectionNavigation(section: LibrarySection, theme: PlayerTheme,
                          onSelect: (LibrarySection) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LibrarySection.entries.forEach { item ->
            Text(item.title, color = if (item == section) Color.White else Color.White.copy(alpha = .65f),
                fontWeight = if (item == section) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.background(
                    if (item == section) Color.White.copy(alpha = .09f) else Color.Transparent,
                    RoundedCornerShape(12.dp)).clickable { onSelect(item) }
                    .padding(horizontal = 14.dp, vertical = 11.dp))
        }
    }
}
