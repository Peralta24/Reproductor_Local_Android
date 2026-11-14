package mx.musicalesdoris.reproductorlocalandroid.library.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.library.LibrarySection
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun SoniqSectionNavigation(section: LibrarySection, theme: PlayerTheme, onSelect: (LibrarySection) -> Unit) {
    val scrollState = rememberScrollState()
    Row(Modifier.fillMaxWidth().height(52.dp)
        .background(Brush.verticalGradient(listOf(SoniqPalette.pageTop, SoniqPalette.pageMiddle)))
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        LibrarySection.entries.forEach { item ->
            val selected = item == section
            val bringIntoViewRequester = remember(item) { BringIntoViewRequester() }
            LaunchedEffect(selected) {
                if (selected) bringIntoViewRequester.bringIntoView()
            }
            val shape = RoundedCornerShape(if (selected) 12.dp else 8.dp)
            Box(Modifier.padding(horizontal = 2.dp).widthIn(min = 48.dp).height(44.dp)
                .clip(shape)
                .background(if (selected) Brush.verticalGradient(listOf(
                    androidx.compose.ui.graphics.Color.White, androidx.compose.ui.graphics.Color(0xFFD6E8F8),
                    androidx.compose.ui.graphics.Color(0xFFBFDBF7), SoniqPalette.pageTop))
                    else Brush.verticalGradient(listOf(SoniqPalette.pageTop, SoniqPalette.pageMiddle)), shape)
                .border(if (selected) 1.dp else 0.dp, if (selected) SoniqPalette.border
                    else androidx.compose.ui.graphics.Color.Transparent, shape)
                .bringIntoViewRequester(bringIntoViewRequester)
                .selectable(selected = selected, role = Role.Tab) { onSelect(item) }
                .padding(horizontal = 4.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(item.title, color = if (selected) SoniqPalette.ink else SoniqPalette.mutedInk,
                    fontFamily = SoniqBodyFont, fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1, softWrap = false)
            }
        }
    }
}
