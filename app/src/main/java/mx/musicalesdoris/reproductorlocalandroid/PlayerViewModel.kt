package mx.musicalesdoris.reproductorlocalandroid

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException

data class PlayerState(
    val selectedId: String? = null,
    val isPlaying: Boolean = false,
    val currentMs: Int = 0,
    val durationMs: Int = 0,
    val repeat: Boolean = false,
    val shuffle: Boolean = false,
    val error: String? = null,
    val isImporting: Boolean = false,
    val isPreparing: Boolean = false,
    val selectedFileAvailable: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    val library = SongLibrary(application)
    private val preferences = application.getSharedPreferences("player", 0)
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    private data class PendingPlayback(val songId: String, val queue: List<Song>, val autoplay: Boolean)
    private var pendingPlayback: PendingPlayback? = null
    private var controller: MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var queueGeneration = 0L

    private val controllerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = controller ?: return
            val duration = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L)?.toInt() ?: 0
            _state.value = _state.value.copy(
                isPreparing = playbackState == Player.STATE_BUFFERING,
                durationMs = duration,
                isPlaying = player.isPlaying
            )
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId
            if (id != null) preferences.edit().putString("selectedSongID", id).apply()
            val song = id?.let { selectedId -> library.songs.value.firstOrNull { it.id == selectedId } }
            _state.value = _state.value.copy(
                selectedId = id,
                currentMs = 0,
                durationMs = (song?.duration?.times(1000))?.toInt()?.coerceAtLeast(0) ?: 0,
                selectedFileAvailable = song?.let(library::isAvailable) ?: false,
                isPreparing = controller?.playbackState == Player.STATE_BUFFERING,
                error = null
            )
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _state.value = _state.value.copy(
                isPlaying = false,
                isPreparing = false,
                selectedFileAvailable = false,
                error = "Error de audio: ${error.localizedMessage ?: "no se pudo reproducir el archivo"}"
            )
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) { library.scanMissingMetadata() }
        val token = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(application, token).buildAsync().also { future ->
            future.addListener({
                try {
                    val connected = future.get()
                    controller = connected
                    connected.addListener(controllerListener)
                    connected.repeatMode = if (_state.value.repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    connected.shuffleModeEnabled = _state.value.shuffle
                    val pending = pendingPlayback
                    if (pending != null) {
                        pendingPlayback = null
                        applyPlayback(pending)
                    } else {
                        val saved = preferences.getString("selectedSongID", null)
                        val initial = library.songs.value.firstOrNull { it.id == saved }
                            ?: library.songs.value.firstOrNull()
                        if (initial != null) select(initial, autoplay = false)
                    }
                } catch (exception: ExecutionException) {
                    _state.value = _state.value.copy(error = "No se pudo iniciar el reproductor: ${exception.cause?.localizedMessage}")
                } catch (exception: Exception) {
                    _state.value = _state.value.copy(error = "No se pudo conectar al servicio de audio: ${exception.localizedMessage}")
                }
            }, ContextCompat.getMainExecutor(application))
        }
        viewModelScope.launch {
            while (isActive) {
                controller?.let { player ->
                    val duration = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L)?.toInt()
                        ?: _state.value.durationMs
                    _state.value = _state.value.copy(
                        currentMs = player.currentPosition.coerceAtLeast(0L).toInt(),
                        durationMs = duration,
                        isPlaying = player.isPlaying
                    )
                }
                delay(500)
            }
        }
    }

    fun selectedSong(songs: List<Song>): Song? = songs.firstOrNull { it.id == _state.value.selectedId }

    fun select(song: Song, autoplay: Boolean = true) = select(song, library.songs.value, autoplay)

    fun select(song: Song, queue: List<Song>, autoplay: Boolean = true) {
        if (library.songs.value.none { it.id == song.id }) return
        preferences.edit().putString("selectedSongID", song.id).apply()
        if (!library.isAvailable(song)) {
            controller?.stop()
            _state.value = _state.value.copy(
                selectedId = song.id,
                isPlaying = false,
                currentMs = 0,
                durationMs = (song.duration?.times(1000))?.toInt()?.coerceAtLeast(0) ?: 0,
                isPreparing = false,
                selectedFileAvailable = false,
                error = "El archivo de audio de ${song.title} no está disponible"
            )
            return
        }
        val availableQueue = queue.filter(library::isAvailable).let { entries ->
            if (entries.any { it.id == song.id }) entries else listOf(song)
        }
        val pending = PendingPlayback(song.id, availableQueue, autoplay)
        if (controller == null) pendingPlayback = pending else applyPlayback(pending)
        _state.value = _state.value.copy(
            selectedId = song.id,
            isPlaying = false,
            currentMs = 0,
            durationMs = (song.duration?.times(1000))?.toInt()?.coerceAtLeast(0) ?: 0,
            isPreparing = true,
            selectedFileAvailable = true,
            error = null
        )
    }

    fun playCollection(songs: List<Song>, startingWith: Song, autoplay: Boolean = true) =
        select(startingWith, songs, autoplay)

    private fun applyPlayback(pending: PendingPlayback) {
        val connected = controller ?: run { pendingPlayback = pending; return }
        val generation = ++queueGeneration
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) {
                pending.queue.map { song ->
                    val artwork = library.artworkFileFor(song)?.takeIf { it.isFile }?.let { file ->
                        runCatching { file.readBytes() }.getOrNull()
                    }
                    SongMediaItemFactory.create(song, library.fileFor(song), artwork)
                }
            }
            if (generation != queueGeneration || controller !== connected || items.isEmpty()) return@launch
            val startIndex = items.indexOfFirst { it.mediaId == pending.songId }.takeIf { it >= 0 } ?: 0
            connected.setMediaItems(items, startIndex, 0L)
            connected.prepare()
            if (pending.autoplay) connected.play()
        }
    }

    fun togglePlayback() {
        val player = controller
        if (player == null) {
            _state.value = _state.value.copy(error = "El reproductor todavía se está iniciando")
            return
        }
        val selected = library.songs.value.firstOrNull { it.id == _state.value.selectedId }
        if (selected == null || !library.isAvailable(selected)) {
            _state.value = _state.value.copy(
                isPlaying = false,
                isPreparing = false,
                selectedFileAvailable = false,
                error = "El archivo de audio seleccionado no está disponible"
            )
            return
        }
        if (player.isPlaying) player.pause() else {
            if (player.currentMediaItem == null) select(selected, autoplay = true)
            else {
                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                player.play()
            }
        }
    }

    fun next() { controller?.seekToNextMediaItem() }

    fun previous() {
        val player = controller ?: return
        if (player.currentPosition > 3_000 || !player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)) {
            player.seekTo(0)
        } else player.seekToPreviousMediaItem()
    }

    fun seek(ms: Int) {
        val position = ms.coerceIn(0, _state.value.durationMs)
        controller?.seekTo(position.toLong())
        _state.value = _state.value.copy(currentMs = position)
    }

    fun skip(ms: Int) = seek(_state.value.currentMs + ms)

    fun toggleRepeat() {
        val repeat = !_state.value.repeat
        _state.value = _state.value.copy(repeat = repeat)
        controller?.repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun toggleShuffle() {
        val shuffle = !_state.value.shuffle
        _state.value = _state.value.copy(shuffle = shuffle)
        controller?.shuffleModeEnabled = shuffle
    }

    fun toggleFavorite() { _state.value.selectedId?.let(::toggleFavorite) }

    fun toggleFavorite(songId: String) {
        viewModelScope.launch(Dispatchers.IO) { library.toggleFavorite(songId) }
    }

    fun importSongs(uris: List<Uri>) {
        if (uris.isEmpty() || _state.value.isImporting) return
        _state.value = _state.value.copy(isImporting = true, error = null)
        viewModelScope.launch {
            try {
                val imported = withContext(Dispatchers.IO) { library.importSongs(uris) }
                imported.firstOrNull()?.let(::select)
            } catch (exception: Exception) {
                _state.value = _state.value.copy(error = "No se pudieron importar las canciones: ${exception.localizedMessage}")
            } finally {
                _state.value = _state.value.copy(isImporting = false)
            }
        }
    }

    fun delete(song: Song) {
        val wasSelected = song.id == _state.value.selectedId
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) { library.delete(song) }
            if (!deleted) {
                if (wasSelected && song.id == _state.value.selectedId && !library.isAvailable(song)) {
                    controller?.stop()
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isPreparing = false,
                        selectedFileAvailable = false,
                        error = library.error.value
                    )
                }
                return@launch
            }
            if (!wasSelected || song.id != _state.value.selectedId) return@launch
            val nextSong = library.songs.value.firstOrNull { library.isAvailable(it) }
            if (nextSong != null) select(nextSong, autoplay = false) else {
                controller?.clearMediaItems()
                preferences.edit().remove("selectedSongID").apply()
                _state.value = PlayerState(repeat = _state.value.repeat, shuffle = _state.value.shuffle)
            }
        }
    }

    fun clearError() {
        if (library.catalogNeedsRecovery.value) return
        _state.value = _state.value.copy(error = null)
        library.clearMessages()
    }

    fun recoverCatalog() {
        if (library.recoverUnreadableCatalog()) _state.value = _state.value.copy(error = null)
    }

    override fun onCleared() {
        controller?.removeListener(controllerListener)
        controller?.release()
        controller = null
        controllerFuture?.cancel(true)
        controllerFuture = null
        super.onCleared()
    }

}
