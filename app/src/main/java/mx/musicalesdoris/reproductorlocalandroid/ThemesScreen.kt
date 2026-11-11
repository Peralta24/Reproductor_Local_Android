package mx.musicalesdoris.reproductorlocalandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThemesScreen(theme: PlayerTheme, selectedId: String, themes: ThemeViewModel) {
    val overrides by themes.componentOverrides.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Temas", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text("Temas completos", color = Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        PlayerTheme.all.forEach { option ->
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp)
                .background(Color.White.copy(alpha = .08f), RoundedCornerShape(22.dp))
                .border(1.dp, if (selectedId == option.id) option.accent else Color.White.copy(alpha = .14f), RoundedCornerShape(22.dp))
                .clickable { themes.select(option.id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(80.dp).background(
                    Brush.linearGradient(listOf(option.topGlow, option.background, option.bottomGlow)), RoundedCornerShape(16.dp)))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(option.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(option.description, color = Color.White.copy(alpha = .65f), fontSize = 14.sp)
                }
                if (selectedId == option.id) Text("✓", color = option.accent, fontSize = 24.sp)
            }
        }
        Text("Combinar componentes", color = Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        LibraryComponent.entries.forEach { component ->
            var expanded by remember(component) { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)
                .background(Color.White.copy(alpha = .08f), RoundedCornerShape(16.dp))
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(component.title, color = Color.White, modifier = Modifier.weight(1f))
                Box {
                    val current = themes.variantFor(component, theme)
                    Text((component.availableVariants.firstOrNull { it.id == current }?.name ?: "Diseño") + " ⌄",
                        color = theme.accent, modifier = Modifier.clickable { expanded = true })
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Usar tema completo") }, onClick = {
                            themes.setVariant(component, null); expanded = false
                        })
                        component.availableVariants.forEach { variant ->
                            DropdownMenuItem(text = { Text(variant.name) }, onClick = {
                                themes.setVariant(component, variant.id); expanded = false
                            })
                        }
                    }
                }
            }
        }
    }
}
