package mx.musicalesdoris.reproductorlocalandroid

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

data class PlayerState(
    val selectedId: String? = null,
    val isPlaying: Boolean = false,
    val currentMs: Int = 0,
    val durationMs: Int = 0,
    val repeat: Boolean = false,
    val error: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    val library = SongLibrary(application)
    private val preferences = application.getSharedPreferences("player", 0)
    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                _state.value = _state.value.copy(currentMs = player.currentPosition, durationMs = player.duration.coerceAtLeast(0))
                handler.postDelayed(this, 500)
            }
        }
    }

    init {
        val saved = preferences.getString("selectedSongID", null)
        val initial = library.songs.value.firstOrNull { it.id == saved } ?: library.songs.value.firstOrNull()
        if (initial != null) select(initial, autoplay = false)
    }

    fun selectedSong(songs: List<Song>): Song? = songs.firstOrNull { it.id == _state.value.selectedId }

    fun select(song: Song, autoplay: Boolean = true) {
        if (library.songs.value.none { it.id == song.id }) return
        releasePlayer()
        try {
            val player = MediaPlayer().apply {
                setDataSource(library.fileFor(song).absolutePath)
                setOnCompletionListener { onSongEnded() }
                prepare()
            }
            mediaPlayer = player
            preferences.edit().putString("selectedSongID", song.id).apply()
            _state.value = _state.value.copy(selectedId = song.id, isPlaying = false, currentMs = 0,
                durationMs = player.duration.coerceAtLeast(0), error = null)
            handler.post(ticker)
            if (autoplay) play()
        } catch (exception: Exception) {
            releasePlayer()
            _state.value = _state.value.copy(error = "Error de audio: ${exception.localizedMessage}")
        }
    }

    fun togglePlayback() { if (_state.value.isPlaying) pause() else play() }
    private fun play() {
        try {
            mediaPlayer?.let {
                if (it.duration > 0 && it.currentPosition >= it.duration) it.seekTo(0)
                it.start()
                _state.value = _state.value.copy(isPlaying = true, error = null)
            }
        } catch (exception: Exception) {
            _state.value = _state.value.copy(error = "Error de audio: ${exception.localizedMessage}")
        }
    }
    private fun pause() { mediaPlayer?.pause(); _state.value = _state.value.copy(isPlaying = false) }
    fun seek(ms: Int) {
        mediaPlayer?.seekTo(ms.coerceIn(0, _state.value.durationMs))
        _state.value = _state.value.copy(currentMs = ms.coerceIn(0, _state.value.durationMs))
    }
    fun skip(ms: Int) = seek(_state.value.currentMs + ms)
    fun toggleRepeat() { _state.value = _state.value.copy(repeat = !_state.value.repeat) }
    fun toggleFavorite() { _state.value.selectedId?.let(library::toggleFavorite) }
    fun importSongs(uris: List<Uri>) { library.importSongs(uris).firstOrNull()?.let { select(it) } }
    fun delete(song: Song) {
        val wasSelected = song.id == _state.value.selectedId
        if (!library.delete(song) || !wasSelected) return
        val next = library.songs.value.firstOrNull()
        if (next != null) select(next, autoplay = false) else {
            releasePlayer()
            preferences.edit().remove("selectedSongID").apply()
            _state.value = PlayerState(repeat = _state.value.repeat)
        }
    }
    fun clearError() { _state.value = _state.value.copy(error = null); library.error.value = null }
    private fun onSongEnded() {
        _state.value = _state.value.copy(isPlaying = false, currentMs = _state.value.durationMs)
        if (_state.value.repeat) { seek(0); play(); return }
        val songs = library.songs.value
        val index = songs.indexOfFirst { it.id == _state.value.selectedId }
        if (index >= 0 && index + 1 < songs.size) select(songs[index + 1])
    }
    private fun releasePlayer() { handler.removeCallbacks(ticker); mediaPlayer?.release(); mediaPlayer = null }
    override fun onCleared() { releasePlayer(); super.onCleared() }
}
