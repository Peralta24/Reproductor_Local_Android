package mx.musicalesdoris.reproductorlocalandroid.library.components.navigation

import mx.musicalesdoris.reproductorlocalandroid.*
import mx.musicalesdoris.reproductorlocalandroid.library.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AuroraSectionNavigation(section: LibrarySection, theme: PlayerTheme,
                            onSelect: (LibrarySection) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LibrarySection.entries.forEach { item ->
            Column(Modifier.clickable { onSelect(item) }.padding(horizontal = 4.dp)) {
                Text(item.title, color = if (item == section) Color.White else Color.White.copy(alpha = .65f),
                    fontWeight = if (item == section) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 11.dp))
                Box(Modifier.fillMaxWidth().height(3.dp).background(
                    if (item == section) theme.accent else Color.Transparent))
            }
        }
    }
}
