package mx.musicalesdoris.reproductorlocalandroid.player

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.mediarouter.app.SystemOutputSwitcherDialogController
import mx.musicalesdoris.reproductorlocalandroid.PlayerViewModel
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqBodyFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqHeadingFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqMonoFont
import mx.musicalesdoris.reproductorlocalandroid.ui.theme.SoniqPalette

@Composable
fun SoniqAudioOutputDialog(
    player: PlayerViewModel,
    isPlaying: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember(context) { context.getSystemService(AudioManager::class.java) }
    var devices by remember(player) { mutableStateOf(player.availableAudioOutputs().mediaMusicChoices()) }
    var routedDevice by remember(player) { mutableStateOf<AudioDeviceInfo?>(null) }
    var requestedRouteKey by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    DisposableEffect(audioManager, player) {
        fun refreshDevices() {
            val connected = player.availableAudioOutputs().mediaMusicChoices()
            devices = connected
            if (routedDevice != null && connected.none { it.matches(routedDevice) }) {
                routedDevice = null
            }
            if (requestedRouteKey != null && connected.none { it.identityKey == requestedRouteKey }) {
                requestedRouteKey = null
                message = "La salida solicitada ya no está disponible."
            }
        }
        val deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refreshDevices()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refreshDevices()
        }
        audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        refreshDevices()
        val routeSubscription = player.observeAudioOutputRouting { route ->
            routedDevice = route
            if (requestedRouteKey != null && route?.mediaIdentityKey() == requestedRouteKey) {
                requestedRouteKey = null
                message = null
            }
        }
        onDispose {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
            routeSubscription.close()
        }
    }

    val directSelectionAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
        player.hasPreparedMediaPlayer()
    val activeRoute = routedDevice.takeIf { isPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            val dialogShape = RoundedCornerShape(22.dp)
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                    .heightIn(max = minOf(640.dp, maxHeight * .92f)),
                shape = dialogShape,
                color = SoniqPalette.pageTop,
                shadowElevation = 18.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = .9f))
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(
                            Color(0xFFF7FAFE), SoniqPalette.pageTop, SoniqPalette.pageBottom
                        )))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("SALIDA DE AUDIO", color = SoniqPalette.ink,
                                fontFamily = SoniqHeadingFont, fontWeight = FontWeight.Bold, fontSize = 19.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("ENRUTAMIENTO DEL SISTEMA", color = SoniqPalette.secondaryText,
                                fontFamily = SoniqMonoFont, fontSize = 8.sp, letterSpacing = 1.sp)
                        }
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(Brush.verticalGradient(listOf(Color.White, SoniqPalette.metalLight)))
                                .border(1.dp, SoniqPalette.border, CircleShape)
                                .clickable(onClick = onDismiss)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Cerrar salida de audio"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("×", color = SoniqPalette.ink, fontFamily = SoniqHeadingFont,
                                fontSize = 25.sp, textAlign = TextAlign.Center)
                        }
                    }

                    Spacer(Modifier.height(13.dp))
                    CurrentOutputCard(activeRoute, isPlaying)
                    Spacer(Modifier.height(17.dp))

                    Text("SALIDAS VISIBLES PARA ANDROID", color = SoniqPalette.secondaryText,
                        fontFamily = SoniqMonoFont, fontSize = 9.sp, letterSpacing = .7.sp)
                    Spacer(Modifier.height(8.dp))

                    if (devices.isEmpty()) {
                        InfoPanel("Android no reporta salidas de audio en este momento.")
                    } else {
                        val hasExternalOutput = devices.any { !it.device.isBuiltInOutput() }
                        if (!hasExternalOutput) {
                            InfoPanel("No se detectan otras salidas conectadas. El altavoz interno sigue disponible.")
                            Spacer(Modifier.height(7.dp))
                        }
                        devices.forEach { choice ->
                            val device = choice.device
                            val confirmedInUse = choice.matches(activeRoute)
                            val selectionPending = requestedRouteKey == choice.identityKey
                            OutputDeviceRow(
                                device = device,
                                confirmedInUse = confirmedInUse,
                                directSelectionAvailable = directSelectionAvailable && !selectionPending,
                                selectionPending = selectionPending,
                                onSelect = {
                                    if (!directSelectionAvailable) {
                                        message = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                                            "La selección directa requiere Android 9 o posterior. Usa el selector de Android."
                                        } else {
                                            "No hay un reproductor preparado para aplicar esta salida."
                                        }
                                    } else if (!player.setPreferredAudioOutput(device)) {
                                        requestedRouteKey = null
                                        message = when (openSystemOutputPicker(context, player).result) {
                                            OutputPickerResult.SYSTEM -> "La ruta directa no fue aceptada; se abrió el selector de Android."
                                            OutputPickerResult.BLUETOOTH_SETTINGS -> "La ruta directa no fue aceptada; se abrieron los ajustes Bluetooth."
                                            OutputPickerResult.UNAVAILABLE -> "Android no pudo aplicar la ruta ni abrir otro selector."
                                        }
                                    } else {
                                        requestedRouteKey = choice.identityKey
                                        val routeNow = player.currentRoutedAudioOutput()
                                        routedDevice = routeNow
                                        if (choice.matches(routeNow)) {
                                            requestedRouteKey = null
                                            message = null
                                        } else {
                                            message = "Solicitud enviada; Android aún no confirma esta ruta."
                                        }
                                    }
                                }
                            )
                            Spacer(Modifier.height(7.dp))
                        }
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        InfoPanel("Android 8.x no ofrece selección directa desde el reproductor. Usa el selector del sistema o Ajustes Bluetooth.")
                    } else if (!player.hasPreparedMediaPlayer()) {
                        InfoPanel("Para solicitar una salida directamente desde SONIQ, carga una canción. También puedes abrir el selector de Android.")
                    }

                    message?.let {
                        Spacer(Modifier.height(5.dp))
                        Text(it, color = SoniqPalette.electricBlue, fontFamily = SoniqBodyFont,
                            fontSize = 11.sp, lineHeight = 15.sp)
                    }

                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(Color.White, SoniqPalette.metalLight)))
                            .border(1.dp, SoniqPalette.border, RoundedCornerShape(12.dp))
                            .clickable {
                                requestedRouteKey = null
                                val outcome = openSystemOutputPicker(context, player)
                                when (outcome.result) {
                                    OutputPickerResult.SYSTEM -> message = if (outcome.preferenceReset) {
                                        "Selector de Android abierto; el sistema controla ahora la salida."
                                    } else {
                                        "Selector abierto, pero no se pudo restablecer la preferencia de SONIQ."
                                    }
                                    OutputPickerResult.BLUETOOTH_SETTINGS -> message =
                                        "Este Android no ofrece el selector aquí; se abrieron los ajustes Bluetooth."
                                    OutputPickerResult.UNAVAILABLE -> message =
                                        "No se pudieron abrir el selector ni los ajustes Bluetooth."
                                }
                            }
                            .semantics {
                                role = Role.Button
                                contentDescription = "Cambiar salida con Android"
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CAMBIAR SALIDA CON ANDROID", color = SoniqPalette.ink,
                            fontFamily = SoniqBodyFont, fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                    Text(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                            "Android muestra aquí las rutas que puede gestionar para la reproducción."
                        else
                            "En esta versión, Android abrirá Ajustes Bluetooth para conectar o desconectar una salida.",
                        color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont,
                        fontSize = 10.sp, lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentOutputCard(device: AudioDeviceInfo?, isPlaying: Boolean) {
    val cardShape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(cardShape)
            .background(Brush.verticalGradient(listOf(SoniqPalette.lcdTop, Color(0xFF102544))))
            .border(1.dp, SoniqPalette.lcdBorder, cardShape)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape)
            .background(Color(0xFF173A65)).border(1.dp, SoniqPalette.cyan.copy(alpha = .65f), CircleShape),
            contentAlignment = Alignment.Center) {
            Text("♫", color = SoniqPalette.lcdActiveText, fontSize = 18.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("SALIDA ACTUAL", color = SoniqPalette.lcdText,
                fontFamily = SoniqMonoFont, fontSize = 8.sp, letterSpacing = .7.sp)
            Spacer(Modifier.height(3.dp))
            if (device != null) {
                Text(device.displayName(), color = Color.White, fontFamily = SoniqBodyFont,
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(device.typeLabel(), color = SoniqPalette.lcdText,
                    fontFamily = SoniqMonoFont, fontSize = 8.sp)
            } else {
                Text("No confirmada", color = Color.White, fontFamily = SoniqBodyFont,
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    if (isPlaying) "ANDROID NO REPORTA LA RUTA ACTIVA"
                    else "LA RUTA SE CONFIRMA DURANTE LA REPRODUCCIÓN",
                    color = SoniqPalette.lcdText, fontFamily = SoniqMonoFont, fontSize = 7.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (device != null) {
            Text("EN USO", color = SoniqPalette.lcdActiveText,
                fontFamily = SoniqMonoFont, fontWeight = FontWeight.Bold, fontSize = 8.sp)
        }
    }
}

@Composable
private fun OutputDeviceRow(
    device: AudioDeviceInfo,
    confirmedInUse: Boolean,
    directSelectionAvailable: Boolean,
    selectionPending: Boolean,
    onSelect: () -> Unit
) {
    val rowShape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().clip(rowShape)
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE1EDF9))))
            .border(1.dp, if (confirmedInUse) SoniqPalette.electricBlue else SoniqPalette.border, rowShape)
            .clickable(enabled = directSelectionAvailable, onClick = onSelect)
            .semantics {
                role = Role.Button
                contentDescription = buildString {
                    append(device.displayName())
                    append(". ")
                    append(device.typeLabel())
                    if (confirmedInUse) append(". En uso, confirmado por Android")
                    if (selectionPending) append(". Esperando confirmación de Android")
                    if (!directSelectionAvailable && !selectionPending) append(". Usa cambiar salida con Android")
                }
            }
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape)
            .background(if (confirmedInUse) Color(0xFFD7F4FF) else Color(0xFFEAF2FB))
            .border(1.dp, SoniqPalette.border, CircleShape), contentAlignment = Alignment.Center) {
            Text(device.iconGlyph(), color = SoniqPalette.electricBlue, fontSize = 15.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(device.displayName(), color = SoniqPalette.ink, fontFamily = SoniqBodyFont,
                fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(device.typeLabel(), color = SoniqPalette.secondaryText,
                fontFamily = SoniqMonoFont, fontSize = 8.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (confirmedInUse) "EN USO" else if (selectionPending) "ESPERANDO"
            else if (directSelectionAvailable) "DISPONIBLE" else "DETECTADA",
            color = if (confirmedInUse || selectionPending) SoniqPalette.electricBlue else SoniqPalette.secondaryText,
            fontFamily = SoniqMonoFont, fontWeight = FontWeight.Bold, fontSize = 7.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun InfoPanel(text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
        .background(Color.White.copy(alpha = .7f))
        .border(1.dp, SoniqPalette.border.copy(alpha = .8f), RoundedCornerShape(11.dp))
        .padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(text, color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont,
            fontSize = 10.sp, lineHeight = 14.sp)
    }
}

private enum class OutputPickerResult { SYSTEM, BLUETOOTH_SETTINGS, UNAVAILABLE }
private data class OutputPickerOutcome(val result: OutputPickerResult, val preferenceReset: Boolean)

private fun openSystemOutputPicker(context: Context, player: PlayerViewModel): OutputPickerOutcome {
    val preferenceReset = player.clearPreferredAudioOutput()
    val opened = runCatching {
        SystemOutputSwitcherDialogController.showDialog(context)
    }.getOrDefault(false)
    if (opened) return OutputPickerOutcome(OutputPickerResult.SYSTEM, preferenceReset)
    val result = try {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        OutputPickerResult.BLUETOOTH_SETTINGS
    } catch (_: Exception) {
        OutputPickerResult.UNAVAILABLE
    }
    return OutputPickerOutcome(result, preferenceReset)
}

private data class AudioOutputChoice(
    val device: AudioDeviceInfo,
    val identityKey: String,
    val deviceIds: Set<Int>
) {
    fun matches(route: AudioDeviceInfo?): Boolean = route != null &&
        (route.id in deviceIds || route.mediaIdentityKey() == identityKey)
}

private fun List<AudioDeviceInfo>.mediaMusicChoices(): List<AudioOutputChoice> {
    val eligible = filter { it.isSelectableMediaOutput() }
    return eligible.groupBy { it.mediaIdentityKey() }.map { (key, equivalentRoutes) ->
        val speaker = equivalentRoutes.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        AudioOutputChoice(speaker ?: equivalentRoutes.first(), key, equivalentRoutes.mapTo(linkedSetOf()) { it.id })
    }
}

private fun AudioDeviceInfo.isSelectableMediaOutput(): Boolean {
    if (!isSink) return false
    return when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY,
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_LINE_ANALOG,
        AudioDeviceInfo.TYPE_LINE_DIGITAL,
        AudioDeviceInfo.TYPE_DOCK,
        AudioDeviceInfo.TYPE_AUX_LINE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_HEARING_AID -> true
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER ->
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        AudioDeviceInfo.TYPE_BLE_BROADCAST -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        AudioDeviceInfo.TYPE_HDMI_ARC -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        AudioDeviceInfo.TYPE_HDMI_EARC -> Build.VERSION.SDK_INT >= 34
        else -> false
    }
}

private fun AudioDeviceInfo.mediaIdentityKey(): String {
    if (type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE)) {
        return "builtin-speaker"
    }
    val addressValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) address.trim() else ""
    return if (addressValue.isNotEmpty()) "type:$type|address:$addressValue" else "id:$id"
}

private fun AudioDeviceInfo.isBuiltInOutput(): Boolean =
    type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE)

private fun AudioDeviceInfo.displayName(): String = productName.toString().trim().ifBlank { typeLabel() }

private fun AudioDeviceInfo.typeLabel(): String {
    return when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> "Altavoz del teléfono"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth · A2DP"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth LE · audífonos"
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth LE · bocina"
        AudioDeviceInfo.TYPE_BLE_BROADCAST -> "Bluetooth LE · transmisión"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Audífonos con cable"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Audífonos con cable y micrófono"
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_ACCESSORY, AudioDeviceInfo.TYPE_USB_HEADSET -> "Salida USB"
        AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC -> "Salida HDMI"
        AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_LINE_DIGITAL -> "Salida de línea"
        AudioDeviceInfo.TYPE_DOCK -> "Base de audio"
        AudioDeviceInfo.TYPE_AUX_LINE -> "Salida auxiliar"
        AudioDeviceInfo.TYPE_HEARING_AID -> "Audífono asistivo"
        else -> "Salida de audio"
    }
}

private fun AudioDeviceInfo.iconGlyph(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        when (type) {
            AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER -> return "ᛒ"
        }
    }
    return when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "▣"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "ᛒ"
        AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER, AudioDeviceInfo.TYPE_BLE_BROADCAST -> "ᛒ"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "◉"
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "▤"
        else -> "♫"
    }
}
