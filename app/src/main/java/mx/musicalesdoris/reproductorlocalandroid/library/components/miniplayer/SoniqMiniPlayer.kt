package mx.musicalesdoris.reproductorlocalandroid.library.components.miniplayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.animation.ValueAnimator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.musicalesdoris.reproductorlocalandroid.PlayerState
import mx.musicalesdoris.reproductorlocalandroid.PlaybackProgress
import mx.musicalesdoris.reproductorlocalandroid.PlayerTheme
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.R
import mx.musicalesdoris.reproductorlocalandroid.Song
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqMonoFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.soniqClickable

@Composable
fun SoniqMiniPlayer(song: Song?, state: PlayerState, progress: PlaybackProgress, theme: PlayerTheme,
                    player: PlayerViewModel, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 8.dp, bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth().height(88.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(
                Color(0xFFD6E8F8), Color(0xFFAACBEE), Color(0xFF8BB8E8), Color(0xFFB0CEEA)
            )))
            .border(1.dp, SoniqPalette.metalBorder, RoundedCornerShape(20.dp)),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(72.dp).fillMaxHeight().padding(start = 8.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                Box(Modifier.size(44.dp).shadow(2.dp, CircleShape).clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(SoniqPalette.metalLight,
                        SoniqPalette.metalDark, Color(0xFFA8C8E8))), CircleShape)
                    .border(1.dp, Color(0xFF6090C0), CircleShape)
                    .soniqClickable(CircleShape, enabled = song != null, onClick = player::togglePlayback),
                    contentAlignment = Alignment.Center) {
                    if (state.isPlaying) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(2) { Box(Modifier.width(4.dp).height(14.dp)
                                .background(SoniqPalette.controlInk, RoundedCornerShape(2.dp))) }
                        }
                    } else {
                        Canvas(Modifier.size(width = 18.dp, height = 16.dp)) {
                            val path = Path().apply {
                                moveTo(size.width * .18f, size.height * .05f)
                                lineTo(size.width * .92f, size.height * .5f)
                                lineTo(size.width * .18f, size.height * .95f)
                                close()
                            }
                            drawPath(path, SoniqPalette.controlInk)
                        }
                    }
                }
                Text(song?.let { songFormatLabel(it.fileName) } ?: "AUDIO", color = SoniqPalette.cyan, fontFamily = SoniqMonoFont,
                    fontWeight = FontWeight.Bold, fontSize = 7.sp, letterSpacing = .5.sp,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.background(SoniqPalette.lcdTop, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp))
            }

            Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 4.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.verticalGradient(listOf(SoniqPalette.lcdTop, SoniqPalette.lcdBottom)),
                    RoundedCornerShape(6.dp))
                .border(1.dp, SoniqPalette.lcdBorder, RoundedCornerShape(6.dp))
                .soniqClickable(RoundedCornerShape(6.dp), onClick = onOpen)
                .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)) {
                Text("DIGITAL MP3 PLAYER", color = SoniqPalette.lcdText, fontFamily = SoniqMonoFont,
                    fontSize = 6.sp, letterSpacing = .8.sp, textAlign = TextAlign.Center,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth())
                Box(Modifier.fillMaxWidth().height(1.dp).background(SoniqPalette.lcdBorder))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    EqualizerMark(playing = state.isPlaying)
                    Text(song?.title ?: "Sin canción", color = SoniqPalette.lcdActiveText,
                        fontFamily = SoniqMonoFont, fontWeight = FontWeight.Bold, fontSize = 8.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                Text(song?.artist?.takeIf(String::isNotBlank) ?: if (song == null) "Importa música" else "Artista desconocido",
                    color = SoniqPalette.lcdText, fontFamily = SoniqMonoFont, fontSize = 7.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(time(progress.currentMs), color = SoniqPalette.lcdText, fontFamily = SoniqMonoFont,
                        fontSize = 6.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.weight(1f).height(4.dp)
                        .background(SoniqPalette.lcdTrack, RoundedCornerShape(1.dp))) {
                        Box(Modifier.fillMaxWidth(if (progress.durationMs > 0)
                            (progress.currentMs.toFloat() / progress.durationMs).coerceIn(0f, 1f) else 0f)
                            .height(4.dp).background(SoniqPalette.cyan, RoundedCornerShape(1.dp)))
                    }
                    Text(time(progress.durationMs), color = SoniqPalette.lcdText, fontFamily = SoniqMonoFont,
                        fontSize = 6.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
            }

            Column(Modifier.width(56.dp).fillMaxHeight().padding(start = 4.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                Box(Modifier.size(32.dp).shadow(2.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFB8D4F0), SoniqPalette.metalDark)),
                        RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF6090C0), RoundedCornerShape(16.dp))
                    .soniqClickable(RoundedCornerShape(16.dp)) { player.nextSong() }, contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.soniq_skip_forward), "Siguiente canción", Modifier.size(16.dp, 18.dp))
                }
                Text("♪", color = SoniqPalette.metaText, fontSize = 12.sp, lineHeight = 14.sp)
            }
        }
        Row(Modifier.fillMaxWidth().height(14.dp).padding(horizontal = 16.dp).padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MP3/WMA/REC", color = Color(0xFF8A96A8), fontFamily = SoniqMonoFont,
                fontSize = 7.sp, letterSpacing = .5.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("★", color = Color(0xFFC0392B), fontSize = 9.sp)
                Text("★", color = SoniqPalette.electricBlue, fontSize = 9.sp)
                Text("★", color = Color(0xFFC0392B), fontSize = 9.sp)
            }
            Text("USB ♫", color = Color(0xFF8A96A8), fontFamily = SoniqMonoFont,
                fontSize = 7.sp, letterSpacing = .5.sp)
        }
    }
}

@Composable
private fun EqualizerMark(playing: Boolean) {
    val heights = if (playing && ValueAnimator.areAnimatorsEnabled()) {
        val transition = rememberInfiniteTransition()
        listOf(3f, 6f, 4f, 7f).mapIndexed { index, resting ->
            val animated by transition.animateFloat(
                initialValue = resting,
                targetValue = listOf(7f, 3f, 8f, 4f)[index],
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 440 + index * 90, delayMillis = index * 75,
                        easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            animated
        }
    } else listOf(3f, 8f, 5f, 7f)
    Row(Modifier.height(8.dp), horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.Bottom) {
        heights.forEach { height ->
            Box(Modifier.width(1.dp).height(height.dp).background(SoniqPalette.cyan))
        }
    }
}

private fun time(ms: Int): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
