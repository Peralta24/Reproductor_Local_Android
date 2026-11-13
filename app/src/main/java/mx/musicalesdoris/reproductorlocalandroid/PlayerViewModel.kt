package mx.musicalesdoris.reproductorlocalandroid

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerState(
    val selectedId: String? = null, val isPlaying: Boolean = false,
    val repeat: Boolean = false, val shuffle: Boolean = false,
    val queueIds: List<String>? = null, val volume: Float = .7f, val error: String? = null
)
data class PlaybackProgress(val currentMs: Int = 0, val durationMs: Int = 0)

class PlaybackEngine private constructor(private val application: Application) {
    companion object {
        @Volatile private var instance: PlaybackEngine? = null
        fun get(application: Application): PlaybackEngine = instance ?: synchronized(this) {
            instance ?: PlaybackEngine(application).also { instance = it }
        }
    }
    val library = SongLibrary(application)
    private val preferences = application.getSharedPreferences("player", 0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()
    private val _progress = MutableStateFlow(PlaybackProgress())
    val progress = _progress.asStateFlow()
    private val audioManager = application.getSystemService(AudioManager::class.java)
    private val notifications = application.getSystemService(NotificationManager::class.java)
    private val notificationId = 42
    private val channelId = "reproductor_playback"
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener { change ->
            if (change <= 0 && _state.value.isPlaying) pause()
        }.build()
    private val session = MediaSession(application, "ReproductorLocal").apply {
        setCallback(object : MediaSession.Callback() {
            override fun onPlay() = play()
            override fun onPause() = pause()
            override fun onSkipToNext() = nextSong()
            override fun onSkipToPrevious() = previousSong()
            override fun onSeekTo(pos: Long) = seek(pos.toInt())
        })
        isActive = true
    }
    private var mediaPlayer: MediaPlayer? = null
    private var hasStartedPlayback = false
    private var currentArtwork: android.graphics.Bitmap? = null
    private var artworkRequestKey: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                _progress.value = PlaybackProgress(player.currentPosition, player.duration.coerceAtLeast(0))
                publishSession()
                handler.postDelayed(this, 1000)
            }
        }
    }

    init {
        notifications.createNotificationChannel(NotificationChannel(channelId, "Reproducción",
            NotificationManager.IMPORTANCE_LOW))
        val saved = preferences.getString("selectedSongID", null)
        val initial = library.songs.value.firstOrNull { it.id == saved } ?: library.songs.value.firstOrNull()
        if (initial != null) select(initial, autoplay = false)
        scope.launch {
            library.songs.collect { songs ->
                val selected = songs.firstOrNull { it.id == _state.value.selectedId }
                if (selected != null) publishArtwork(selected)
            }
        }
    }

    fun selectedSong(songs: List<Song>): Song? = songs.firstOrNull { it.id == _state.value.selectedId }
    fun queueSongs(): List<Song> {
        val catalog = library.songs.value
        return _state.value.queueIds?.mapNotNull { id -> catalog.firstOrNull { it.id == id } } ?: catalog
    }
    fun select(song: Song, autoplay: Boolean = true, preserveQueue: Boolean = false) {
        if (library.songs.value.none { it.id == song.id }) return
        releasePlayer()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setWakeMode(application, PowerManager.PARTIAL_WAKE_LOCK)
                setVolume(_state.value.volume, _state.value.volume)
                setDataSource(library.fileFor(song).absolutePath)
                setOnCompletionListener { onSongEnded() }
                prepare()
            }
            mediaPlayer = player
            preferences.edit().putString("selectedSongID", song.id).apply()
            _state.value = _state.value.copy(selectedId = song.id, isPlaying = false,
                queueIds = if (preserveQueue) _state.value.queueIds else null,
                error = null)
            _progress.value = PlaybackProgress(0, player.duration.coerceAtLeast(0))
            currentArtwork = null
            artworkRequestKey = null
            publishSession(true)
            publishArtwork(song)
            handler.post(ticker)
            if (autoplay) play()
        } catch (exception: Exception) {
            releasePlayer()
            _state.value = _state.value.copy(error = "Error de audio: ${exception.localizedMessage}")
        }
    }
    fun playCollection(songs: List<Song>, shuffled: Boolean = false, startingWith: Song? = null) {
        val available = songs.filter { song -> library.songs.value.any { it.id == song.id } }
        if (available.isEmpty()) return
        _state.value = _state.value.copy(queueIds = available.map { it.id }, shuffle = shuffled)
        select(startingWith?.takeIf { it in available } ?: if (shuffled) available.random() else available.first(),
            preserveQueue = true)
    }
    fun nextSong() {
        val queue = queueSongs()
        val current = _state.value.selectedId
        val next = if (_state.value.shuffle) queue.filterNot { it.id == current }.randomOrNull()
            else queue.getOrNull(queue.indexOfFirst { it.id == current } + 1)
        if (next != null) select(next, preserveQueue = true)
    }
    fun previousSong() {
        if (_progress.value.currentMs > 3000) { seek(0); return }
        val queue = queueSongs()
        val index = queue.indexOfFirst { it.id == _state.value.selectedId }
        if (index > 0) select(queue[index - 1], preserveQueue = true) else seek(0)
    }
    fun togglePlayback() { if (_state.value.isPlaying) pause() else play() }
    fun play() {
        try {
            if (mediaPlayer == null) {
                val selected = library.songs.value.firstOrNull { it.id == _state.value.selectedId } ?: return
                select(selected, autoplay = true, preserveQueue = true)
                return
            }
            mediaPlayer?.let {
                if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
                if (it.duration > 0 && it.currentPosition >= it.duration) it.seekTo(0)
                it.start()
                hasStartedPlayback = true
                _state.value = _state.value.copy(isPlaying = true, error = null)
                publishSession(true)
                application.startForegroundService(Intent(application, PlaybackService::class.java))
            }
        } catch (exception: Exception) {
            _state.value = _state.value.copy(error = "Error de audio: ${exception.localizedMessage}")
        }
    }
    fun pause() {
        mediaPlayer?.pause()
        _state.value = _state.value.copy(isPlaying = false)
        publishSession(true)
        audioManager.abandonAudioFocusRequest(focusRequest)
        PlaybackService.stopForegroundPlayback()
    }
    fun seek(ms: Int) {
        val target = ms.coerceIn(0, _progress.value.durationMs)
        mediaPlayer?.seekTo(target)
        _progress.value = _progress.value.copy(currentMs = target)
        publishSession(true)
    }
    fun skip(ms: Int) = seek(_progress.value.currentMs + ms)
    fun toggleRepeat() { _state.value = _state.value.copy(repeat = !_state.value.repeat) }
    fun toggleShuffle() { _state.value = _state.value.copy(shuffle = !_state.value.shuffle) }
    fun toggleFavorite() { _state.value.selectedId?.let(library::toggleFavorite) }
    fun setVolume(value: Float) {
        val volume = value.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = volume)
        mediaPlayer?.setVolume(volume, volume)
    }
    fun importSongs(uris: List<android.net.Uri>) {
        scope.launch { library.importSongs(uris) }
    }
    fun delete(song: Song) {
        val wasSelected = song.id == _state.value.selectedId
        if (!library.delete(song) || !wasSelected) return
        val next = queueSongs().firstOrNull() ?: library.songs.value.firstOrNull()
        if (next != null) select(next, autoplay = false) else {
            releasePlayer()
            preferences.edit().remove("selectedSongID").apply()
            _state.value = PlayerState(repeat = _state.value.repeat, shuffle = _state.value.shuffle)
            publishSession(true)
            PlaybackService.stopForegroundPlayback()
        }
    }
    fun clearError() { _state.value = _state.value.copy(error = null); library.error.value = null }
    private fun onSongEnded() {
        _state.value = _state.value.copy(isPlaying = false)
        _progress.value = _progress.value.copy(currentMs = _progress.value.durationMs)
        if (_state.value.repeat) { seek(0); play(); return }
        val previousId = _state.value.selectedId
        nextSong()
        if (_state.value.selectedId == previousId) pause()
    }
    private fun publishSession(updateNotification: Boolean = false) {
        val current = selectedSong(library.songs.value)
        val metadata = android.media.MediaMetadata.Builder()
            .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, current?.title ?: "")
            .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, current?.artist ?: "")
            .putString(android.media.MediaMetadata.METADATA_KEY_ALBUM, current?.album ?: "")
            .putLong(android.media.MediaMetadata.METADATA_KEY_DURATION, _progress.value.durationMs.toLong())
        currentArtwork?.let { bitmap ->
            metadata.putBitmap(android.media.MediaMetadata.METADATA_KEY_ART, bitmap)
                .putBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap)
                .putBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON, bitmap)
        }
        session.setMetadata(metadata.build())
        val actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SEEK_TO or
            PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS
        session.setPlaybackState(PlaybackState.Builder().setActions(actions)
            .setState(if (_state.value.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                _progress.value.currentMs.toLong(), if (_state.value.isPlaying) 1f else 0f).build())
        if (updateNotification) {
            if (current == null || !hasStartedPlayback) notifications.cancel(notificationId)
            else notifications.notify(notificationId, buildNotification(current))
        }
    }
    private fun publishArtwork(song: Song) {
        val file = library.artworkFor(song) ?: return
        val requestKey = "${song.id}:${song.artworkFileName}:${file.lastModified()}:${file.length()}"
        if (artworkRequestKey == requestKey) return
        artworkRequestKey = requestKey
        scope.launch {
            val bitmap = mx.musicalesdoris.reproductorlocalandroid.library.components.artwork.ArtworkBitmapLoader.decodeFile(file, 512)
            withContext(Dispatchers.Main) {
                if (_state.value.selectedId == song.id) {
                    currentArtwork = bitmap
                    publishSession(true)
                }
            }
        }
    }
    fun stopForTaskRemoval() {
        if (mediaPlayer != null) pause()
        hasStartedPlayback = false
        releasePlayer()
        audioManager.abandonAudioFocusRequest(focusRequest)
        _state.value = _state.value.copy(isPlaying = false)
        publishSession(true)
    }
    private fun releasePlayer() { handler.removeCallbacks(ticker); mediaPlayer?.release(); mediaPlayer = null }
    fun currentNotification(): Notification? = selectedSong(library.songs.value)?.let { buildNotification(it) }
    private fun buildNotification(current: Song): Notification {
        fun pending(action: String, code: Int): PendingIntent {
            val intent = Intent(application, MainActivity::class.java).apply {
                this.action = action
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(application, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        return Notification.Builder(application, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(currentArtwork)
            .setContentTitle(current.title).setContentText(current.subtitle)
            .setContentIntent(pending("mx.musicalesdoris.OPEN_PLAYER", 0))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_previous, "Anterior",
                pending("mx.musicalesdoris.PREVIOUS", 1)).build())
            .addAction(Notification.Action.Builder(
                if (_state.value.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (_state.value.isPlaying) "Pausar" else "Reproducir",
                pending("mx.musicalesdoris.TOGGLE", 2)).build())
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_next, "Siguiente",
                pending("mx.musicalesdoris.NEXT", 3)).build())
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            .build()
    }
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = PlaybackEngine.get(application)
    val library get() = engine.library
    val state get() = engine.state
    val progress get() = engine.progress
    fun selectedSong(songs: List<Song>) = engine.selectedSong(songs)
    fun queueSongs() = engine.queueSongs()
    fun select(song: Song, autoplay: Boolean = true, preserveQueue: Boolean = false) = engine.select(song, autoplay, preserveQueue)
    fun playCollection(songs: List<Song>, shuffled: Boolean = false, startingWith: Song? = null) =
        engine.playCollection(songs, shuffled, startingWith)
    fun nextSong() = engine.nextSong()
    fun previousSong() = engine.previousSong()
    fun togglePlayback() = engine.togglePlayback()
    fun play() = engine.play()
    fun pause() = engine.pause()
    fun seek(ms: Int) = engine.seek(ms)
    fun skip(ms: Int) = engine.skip(ms)
    fun toggleRepeat() = engine.toggleRepeat()
    fun toggleShuffle() = engine.toggleShuffle()
    fun toggleFavorite() = engine.toggleFavorite()
    fun setVolume(value: Float) = engine.setVolume(value)
    fun importSongs(uris: List<android.net.Uri>) = engine.importSongs(uris)
    fun delete(song: Song) = engine.delete(song)
    fun clearError() = engine.clearError()
    fun stopForTaskRemoval() = engine.stopForTaskRemoval()
}
