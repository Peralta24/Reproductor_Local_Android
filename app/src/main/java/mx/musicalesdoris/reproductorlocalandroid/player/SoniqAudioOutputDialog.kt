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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import androidx.compose.ui.window.DialogWindowProvider
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { dialogWindow?.setDimAmount(0f) }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color(0xFF182231).copy(alpha = .28f))
                    .clickable(onClick = onDismiss)
            )
            BoxWithConstraints(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                val dialogShape = RoundedCornerShape(24.dp)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .widthIn(max = 372.dp)
                        .heightIn(max = minOf(572.dp, maxHeight * .92f)),
                    shape = dialogShape,
                    color = Color.Transparent,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, Color(0xFFA0BEDD))
                ) {
                    Box {
                        Column(
                            Modifier.fillMaxWidth()
                                .clip(dialogShape)
                                .background(Brush.verticalGradient(listOf(
                                    Color(0xFFEAF2FB), Color(0xFFC8DCF0), Color(0xFFB0C8E8)
                                )))
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 21.dp, vertical = 21.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().height(44.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Salidas de audio", color = SoniqPalette.ink,
                                    fontFamily = SoniqHeadingFont, fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp, lineHeight = 34.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                OutputCloseButton(onDismiss)
                            }

                            Text("Elige dónde escuchar tu música.", color = SoniqPalette.secondaryText,
                                fontFamily = SoniqBodyFont, fontSize = 13.sp, lineHeight = 18.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(16.dp))
                            CurrentOutputCard(activeRoute, isPlaying)
                            Spacer(Modifier.height(16.dp))

                            if (devices.isEmpty()) {
                                InfoPanel("Android no reporta salidas de audio en este momento.")
                            } else {
                                val hasExternalOutput = devices.any { !it.device.isBuiltInOutput() }
                                if (!hasExternalOutput) {
                                    InfoPanel("No se detectan otras salidas conectadas. El altavoz interno sigue disponible.")
                                    Spacer(Modifier.height(8.dp))
                                }
                                val localDevices = devices.filter { it.device.isBuiltInOutput() }
                                val bluetoothDevices = devices.filter { it.device.isBluetoothOutput() }
                                val otherDevices = devices.filter {
                                    !it.device.isBuiltInOutput() && !it.device.isBluetoothOutput()
                                }
                                val sections = listOf(
                                    "ESTE DISPOSITIVO" to localDevices,
                                    "BLUETOOTH" to bluetoothDevices,
                                    "OTRAS SALIDAS" to otherDevices
                                )
                                var previousSectionHadItems = false
                                sections.forEachIndexed { sectionIndex, (sectionTitle, sectionDevices) ->
                                    if (sectionDevices.isNotEmpty()) {
                                        if (previousSectionHadItems) {
                                            Spacer(Modifier.height(if (sectionIndex == 1) 16.dp else 12.dp))
                                        }
                                        OutputSectionHeading(sectionTitle)
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    sectionDevices.forEachIndexed { rowIndex, choice ->
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
                                        if (rowIndex < sectionDevices.lastIndex) Spacer(Modifier.height(4.dp))
                                    }
                                    if (sectionDevices.isNotEmpty()) previousSectionHadItems = true
                                }
                            }

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                                InfoPanel("Android 8.x no ofrece selección directa desde el reproductor. Usa el selector del sistema o Ajustes Bluetooth.")
                            } else if (!player.hasPreparedMediaPlayer()) {
                                InfoPanel("Para solicitar una salida directamente desde SONIQ, carga una canción. También puedes abrir el selector de Android.")
                            }

                            message?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, color = SoniqPalette.electricBlue, fontFamily = SoniqBodyFont,
                                    fontSize = 11.sp, lineHeight = 15.sp)
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                                    "Android muestra las salidas disponibles para la reproducción."
                                else
                                    "En esta versión, Android abrirá Ajustes Bluetooth para conectar o desconectar una salida.",
                                color = SoniqPalette.secondaryText, fontFamily = SoniqBodyFont,
                                fontSize = 11.sp, lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier.fillMaxWidth().heightIn(min = 44.dp)
                                    .shadow(4.dp, RoundedCornerShape(12.dp),
                                        ambientColor = Color.White.copy(alpha = .4f),
                                        spotColor = SoniqPalette.ink.copy(alpha = .1f))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(soniqFieldGradient())
                                    .border(1.dp, Color(0xFFA0BEDD), RoundedCornerShape(12.dp))
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
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cambiar salida con Android", color = SoniqPalette.ink,
                                    fontFamily = SoniqBodyFont, fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
                            }
                        }
                        Box(Modifier.matchParentSize().padding(1.dp)
                            .border(1.dp, Color.White.copy(alpha = .85f), dialogShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun OutputSectionHeading(text: String) {
    Text(text, color = SoniqPalette.secondaryText, fontFamily = SoniqMonoFont,
        fontSize = 10.sp, lineHeight = 14.sp)
}

@Composable
private fun OutputCloseButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(40.dp)
            .shadow(4.dp, shape, ambientColor = Color.White.copy(alpha = .4f),
                spotColor = SoniqPalette.ink.copy(alpha = .1f))
            .clip(shape)
            .background(soniqFieldGradient())
            .border(1.dp, Color(0xFFA0BEDD), shape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "Cerrar salida de audio"
            },
        contentAlignment = Alignment.Center
    ) {
        Text("×", color = SoniqPalette.ink, fontFamily = SoniqBodyFont,
            fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun CurrentOutputCard(device: AudioDeviceInfo?, isPlaying: Boolean) {
    val cardShape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 88.dp).clip(cardShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1428), Color(0xFF06101E))))
            .border(1.dp, Color(0xFFA0BEDD), cardShape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape)
            .border(1.dp, Color(0xFF5BC8F5), CircleShape),
            contentAlignment = Alignment.Center) {
            if (device?.isBluetoothOutput() == true) {
                Text("ᛒ", color = Color(0xFFA0DCFF), fontFamily = SoniqMonoFont,
                    fontSize = 18.sp, maxLines = 1)
            } else {
                SoniqTransportIcon("volume", size = 20.dp, color = Color(0xFFA0DCFF))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("SALIDA ACTUAL", color = Color(0xFFA0DCFF),
                fontFamily = SoniqMonoFont, fontSize = 10.sp, lineHeight = 14.sp)
            if (device != null) {
                Text(device.outputDisplayName(), color = Color.White, fontFamily = SoniqBodyFont,
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (device.isBuiltInOutput()) "Este teléfono · Ruta confirmada"
                    else "${device.typeLabel()} · Ruta confirmada",
                    color = Color(0xFFA0DCFF), fontFamily = SoniqBodyFont, fontSize = 11.sp,
                    lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text("No confirmada", color = Color.White, fontFamily = SoniqBodyFont,
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (isPlaying) "Android no reporta la ruta activa"
                    else "La ruta se confirma durante la reproducción",
                    color = Color(0xFFA0DCFF), fontFamily = SoniqBodyFont, fontSize = 11.sp,
                    lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
        Modifier.fillMaxWidth().heightIn(min = 68.dp).clip(rowShape)
            .then(
                if (confirmedInUse) Modifier.background(soniqFieldGradient())
                else Modifier.background(Color.Transparent)
            )
            .then(if (confirmedInUse) Modifier.border(1.dp, Color(0xFF174BFA), rowShape) else Modifier)
            .clickable(enabled = directSelectionAvailable, onClick = onSelect)
            .semantics {
                role = Role.Button
                contentDescription = buildString {
                    append(device.outputDisplayName())
                    append(". ")
                    append(device.typeLabel())
                    if (confirmedInUse) append(". En uso, confirmado por Android")
                    if (selectionPending) append(". Esperando confirmación de Android")
                    if (!directSelectionAvailable && !selectionPending) append(". Usa cambiar salida con Android")
                }
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
            .background(soniqFieldGradient())
            .border(1.dp, Color(0xFFA0BEDD), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            if (device.isBuiltInOutput()) {
                SoniqTransportIcon("volume", size = 22.dp, color = Color(0xFF182231))
            } else {
                Text(device.iconGlyph(), color = Color(0xFF182231), fontSize = 17.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(device.outputDisplayName(), color = Color(0xFF182231),
                fontFamily = SoniqBodyFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    confirmedInUse -> "En uso · Ruta confirmada"
                    selectionPending -> "Solicitud enviada · Esperando confirmación"
                    device.isBluetoothOutput() -> "Conectado · Bluetooth"
                    else -> device.typeLabel()
                },
                color = when {
                    confirmedInUse -> Color(0xFF174BFA)
                    selectionPending -> SoniqPalette.electricBlue
                    else -> Color(0xFF626D7D)
                },
                fontFamily = SoniqBodyFont, fontSize = 11.sp, lineHeight = 16.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (confirmedInUse) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF174BFA)),
                contentAlignment = Alignment.Center) {
                Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        } else {
            Box(Modifier.size(20.dp).clip(CircleShape)
                .background(Color(0xFFF3F5F8))
                .border(1.dp, Color(0xFFA0BEDD), CircleShape))
        }
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

private fun soniqFieldGradient(): Brush = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to Color.White,
        .47f to Color(0xFFD6E8F8),
        .51f to Color(0xFFBFDBF7),
        1f to Color(0xFFEAF2FB)
    )
)

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

private fun AudioDeviceInfo.isBluetoothOutput(): Boolean = when (type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true
    AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER ->
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    AudioDeviceInfo.TYPE_BLE_BROADCAST -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    else -> false
}

private fun AudioDeviceInfo.displayName(): String = productName.toString().trim().ifBlank { typeLabel() }

private fun AudioDeviceInfo.outputDisplayName(): String =
    if (isBuiltInOutput()) "Bocina del dispositivo" else displayName()

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
