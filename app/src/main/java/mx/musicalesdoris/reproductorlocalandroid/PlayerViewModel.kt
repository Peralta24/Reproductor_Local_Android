package mx.musicalesdoris.reproductorlocalandroid

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioRouting
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Build
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
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
    val queueIds: List<String>? = null, val volume: Float = .7f, val error: String? = null,
    val isPreparing: Boolean = false
)
data class PlaybackProgress(val currentMs: Int = 0, val durationMs: Int = 0)

internal data class SavedPlaybackSnapshot(
    val selectedSongId: String?,
    val positionMs: Int,
    val queueIds: List<String>?,
    val shuffle: Boolean,
    val repeat: Boolean,
    val contentUri: String? = null,
    val importSourceIdentity: String? = null,
    val privateAudioFingerprint: String? = null,
    val mediaStoreVolume: String? = null
)

internal fun restoredPlayerState(snapshot: SavedPlaybackSnapshot): PlayerState = PlayerState(
    selectedId = snapshot.selectedSongId,
    isPlaying = false,
    repeat = snapshot.repeat,
    shuffle = snapshot.shuffle,
    queueIds = snapshot.queueIds
)

internal fun clampRestoredPosition(positionMs: Int, durationMs: Int): Int =
    positionMs.coerceIn(0, durationMs.coerceAtLeast(0))

private const val PLAYBACK_QUEUE_SEPARATOR = "\u001f"

internal fun savedPlaybackSnapshot(
    selectedSongId: String?, positionMs: Int, hasCustomQueue: Boolean,
    serializedQueueIds: String?, shuffle: Boolean, repeat: Boolean,
    contentUri: String? = null, importSourceIdentity: String? = null,
    privateAudioFingerprint: String? = null, mediaStoreVolume: String? = null
): SavedPlaybackSnapshot = SavedPlaybackSnapshot(
    selectedSongId = selectedSongId,
    positionMs = positionMs.coerceAtLeast(0),
    queueIds = if (!hasCustomQueue) null else serializedQueueIds
        ?.takeIf(String::isNotEmpty)?.split(PLAYBACK_QUEUE_SEPARATOR) ?: emptyList(),
    shuffle = shuffle,
    repeat = repeat,
    contentUri = contentUri,
    importSourceIdentity = importSourceIdentity,
    privateAudioFingerprint = privateAudioFingerprint,
    mediaStoreVolume = mediaStoreVolume
)

internal fun serializePlaybackQueue(queueIds: List<String>?): String? =
    queueIds?.joinToString(PLAYBACK_QUEUE_SEPARATOR)

internal fun playbackIdentitySnapshot(song: Song, positionMs: Int = 0): SavedPlaybackSnapshot =
    SavedPlaybackSnapshot(
        selectedSongId = song.id,
        positionMs = positionMs.coerceAtLeast(0),
        queueIds = null,
        shuffle = false,
        repeat = false,
        contentUri = song.contentUri,
        importSourceIdentity = song.importSourceIdentity ?: song.contentUri?.let {
            mediaStoreIdentity(it, song.fileName, song.sizeBytes, song.importedAt)
        },
        privateAudioFingerprint = song.privateAudioFingerprint,
        mediaStoreVolume = song.mediaStoreVolume
    )

internal fun resolveSavedSongId(snapshot: SavedPlaybackSnapshot, songs: List<Song>): String? {
    songs.firstOrNull { it.id == snapshot.selectedSongId }?.let { return it.id }
    if (snapshot.contentUri != null) {
        songs.firstOrNull { song -> song.contentUri == snapshot.contentUri &&
            (snapshot.mediaStoreVolume == null || song.mediaStoreVolume == snapshot.mediaStoreVolume) }?.let { return it.id }
    }
    if (snapshot.importSourceIdentity != null) {
        songs.firstOrNull { it.importSourceIdentity == snapshot.importSourceIdentity &&
            (snapshot.privateAudioFingerprint == null || it.privateAudioFingerprint == snapshot.privateAudioFingerprint) }
            ?.let { return it.id }
    }
    val fingerprintMatches = snapshot.privateAudioFingerprint?.let { fingerprint ->
        songs.filter { it.privateAudioFingerprint == fingerprint }
    }.orEmpty()
    return fingerprintMatches.singleOrNull()?.id
}

class PlaybackEngine private constructor(private val application: Application) {
    companion object {
        private const val AUDIO_ROUTE_TAG = "SONIQ_AUDIO_ROUTE"
        @Volatile private var instance: PlaybackEngine? = null
        fun get(application: Application): PlaybackEngine = instance ?: synchronized(this) {
            instance ?: PlaybackEngine(application).also { instance = it }
        }
    }
    val library = SongLibrary(application)
    private val preferences = application.getSharedPreferences("player", 0)
    private val restoredSnapshot = readSavedPlaybackSnapshot()
    private fun readSavedPlaybackSnapshot() = savedPlaybackSnapshot(
        selectedSongId = preferences.getString("selectedSongID", null),
        positionMs = preferences.getInt("positionMs", 0),
        hasCustomQueue = preferences.getBoolean("hasCustomQueue", false),
        serializedQueueIds = preferences.getString("queueIds", null),
        shuffle = preferences.getBoolean("shuffle", false),
        repeat = preferences.getBoolean("repeat", false),
        contentUri = preferences.getString("selectedContentUri", null),
        importSourceIdentity = preferences.getString("selectedImportSourceIdentity", null),
        privateAudioFingerprint = preferences.getString("selectedAudioFingerprint", null),
        mediaStoreVolume = preferences.getString("selectedMediaStoreVolume", null)
    )
    private val resolvedRestoredSongId = resolveSavedSongId(restoredSnapshot, library.songs.value)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(restoredPlayerState(restoredSnapshot).copy(
        selectedId = resolvedRestoredSongId ?: restoredSnapshot.selectedSongId
    ))
    val state = _state.asStateFlow()
    private val _progress = MutableStateFlow(PlaybackProgress(
        currentMs = restoredSnapshot.positionMs,
        durationMs = library.songs.value.firstOrNull { it.id == _state.value.selectedId }
            ?.durationMs?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt() ?: 0
    ))
    val progress = _progress.asStateFlow()
    private val audioManager = application.getSystemService(AudioManager::class.java)
    private val diagnosticLoggingEnabled: Boolean
        get() = (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    private val notifications = application.getSystemService(NotificationManager::class.java)
    private val notificationId = 42
    private val channelId = "reproductor_playback"
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener(::onAudioFocusChange).build()
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
    private val playbackPhase = PlaybackPhaseTracker()
    private var playWhenPrepared = false
    private var publishedMetadataKey: String? = null
    private var queueCacheCatalog: List<Song>? = null
    private var queueCacheIds: List<String>? = null
    private var queueCache: List<Song> = emptyList()
    private var knownSongsById = library.songs.value.associateBy { it.id }
    private var resumeAfterTransientFocusLoss = false
    private var duckedForAudioFocus = false
    private var preferredOutputDeviceId: Int? = null
    private val outputRoutingObservers = linkedSetOf<(AudioDeviceInfo?) -> Unit>()
    private var observedRoutingPlayer: MediaPlayer? = null
    private var routingChangedListener: AudioRouting.OnRoutingChangedListener? = null
    private var diagnosticOutputCallbackRegistered = false
    private val diagnosticOutputCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            logDeviceChanges("connected", addedDevices)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            logDeviceChanges("disconnected", removedDevices)
        }
    }
    private var hasStartedPlayback = false
    private var lastProgressPersistedAtMs = 0L
    private var currentArtwork: android.graphics.Bitmap? = null
    private var artworkRequestKey: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            val player = mediaPlayer ?: return
            if (!playbackPhase.canControlPlayer) return
            try {
                _progress.value = PlaybackProgress(player.currentPosition, player.duration.coerceAtLeast(0))
                publishSession()
                val now = SystemClock.elapsedRealtime()
                if (_state.value.isPlaying && now - lastProgressPersistedAtMs >= 5_000L) {
                    persistPlaybackState(_progress.value.currentMs)
                    lastProgressPersistedAtMs = now
                }
                handler.postDelayed(this, 1000)
            } catch (exception: IllegalStateException) {
                handleMediaPlayerError(player, "Error de audio: ${exception.localizedMessage ?: "estado inválido"}")
            }
        }
    }

    init {
        notifications.createNotificationChannel(NotificationChannel(channelId, "Reproducción",
            NotificationManager.IMPORTANCE_LOW))
        scope.launch {
            library.songs.collect { songs ->
                val previousById = knownSongsById
                val currentState = _state.value
                fun resolveId(id: String): String {
                    if (songs.any { it.id == id }) return id
                    val snapshot = previousById[id]?.let { playbackIdentitySnapshot(it) }
                        ?: readSavedPlaybackSnapshot().takeIf { it.selectedSongId == id }
                        ?: return id
                    return resolveSavedSongId(snapshot, songs) ?: id
                }
                val resolvedSelectedId = currentState.selectedId?.let(::resolveId)
                val resolvedQueueIds = currentState.queueIds?.map(::resolveId)
                val identityChanged = resolvedSelectedId != currentState.selectedId || resolvedQueueIds != currentState.queueIds
                if (identityChanged) {
                    _state.value = currentState.copy(selectedId = resolvedSelectedId, queueIds = resolvedQueueIds)
                    persistPlaybackState(_progress.value.currentMs)
                }
                knownSongsById = songs.associateBy { it.id }
                val selected = songs.firstOrNull { it.id == _state.value.selectedId }
                if (selected != null) publishArtwork(selected)
                publishSession(identityChanged)
            }
        }
        publishSession(true)
    }

    fun selectedSong(songs: List<Song>): Song? = songs.firstOrNull { it.id == _state.value.selectedId }
    fun queueSongs(): List<Song> {
        val catalog = library.songs.value
        val ids = _state.value.queueIds ?: return catalog
        if (queueCacheCatalog === catalog && queueCacheIds === ids) return queueCache
        val byId = catalog.associateBy { it.id }
        return ids.mapNotNull(byId::get).also {
            queueCacheCatalog = catalog
            queueCacheIds = ids
            queueCache = it
        }
    }
    fun select(song: Song, autoplay: Boolean = true, preserveQueue: Boolean = false,
               resumePositionMs: Int = 0) {
        if (!song.mediaAvailable) {
            _state.value = _state.value.copy(error = "Esta canción no está disponible ahora. Puedes quitarla de la biblioteca desde su menú.")
            return
        }
        if (library.songs.value.none { it.id == song.id }) return
        releasePlayer()
        try {
            val player = MediaPlayer()
            mediaPlayer = player
            playbackPhase.beginPreparation()
            player.apply {
                setAudioAttributes(attributes)
                setWakeMode(application, PowerManager.PARTIAL_WAKE_LOCK)
                val playbackVolume = _state.value.volume * focusVolumeMultiplier()
                setVolume(playbackVolume, playbackVolume)
                setDataSource(application, library.uriFor(song))
                setOnCompletionListener { onSongEnded() }
                setOnPreparedListener { prepared ->
                    if (mediaPlayer === prepared && playbackPhase.onPrepared()) {
                        try {
                            _state.value = _state.value.copy(isPreparing = false)
                            attachRoutingListenerIfNeeded()
                            applyPreferredOutput(prepared)
                            applyPlayerVolume()
                            val duration = prepared.duration.coerceAtLeast(0)
                            val target = clampRestoredPosition(resumePositionMs, duration)
                            if (target > 0) prepared.seekTo(target)
                            _progress.value = PlaybackProgress(target, duration)
                            persistPlaybackState(target)
                            publishSession(true)
                            handler.post(ticker)
                            if (playWhenPrepared) play()
                        } catch (exception: Exception) {
                            handleMediaPlayerError(prepared, "Error de audio: ${exception.localizedMessage}")
                        }
                    }
                }
                setOnErrorListener { failed, what, extra ->
                    handleMediaPlayerError(failed, "Error de audio ($what/$extra)")
                    true
                }
            }
            playWhenPrepared = autoplay
            _state.value = _state.value.copy(selectedId = song.id, isPlaying = false,
                queueIds = if (preserveQueue) _state.value.queueIds else null,
                error = null, isPreparing = true)
            _progress.value = PlaybackProgress(resumePositionMs.coerceAtLeast(0),
                song.durationMs?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt() ?: 0)
            persistPlaybackState(_progress.value.currentMs)
            currentArtwork = null
            artworkRequestKey = null
            publishedMetadataKey = null
            publishSession(true)
            publishArtwork(song)
            player.prepareAsync()
            logAudioRouteSnapshot("player_preparing")
        } catch (exception: Exception) {
            playbackPhase.onError()
            releasePlayer(preserveErrorPhase = playbackPhase.phase == PlaybackPhase.ERROR)
            resumeAfterTransientFocusLoss = false
            duckedForAudioFocus = false
            hasStartedPlayback = false
            audioManager.abandonAudioFocusRequest(focusRequest)
            PlaybackService.stopForegroundPlayback()
            _state.value = _state.value.copy(isPlaying = false, isPreparing = false,
                error = "Error de audio: ${exception.localizedMessage}")
            publishSession(true)
        }
    }
    fun playCollection(songs: List<Song>, shuffled: Boolean = false, startingWith: Song? = null) {
        val libraryIds = library.songs.value.mapTo(HashSet()) { it.id }
        val available = songs.filter { it.id in libraryIds && it.mediaAvailable }
        if (available.isEmpty()) return
        _state.value = _state.value.copy(queueIds = available.map { it.id }, shuffle = shuffled)
        persistPlaybackState(0)
        select(startingWith?.takeIf { it in available } ?: if (shuffled) available.random() else available.first(),
            preserveQueue = true)
    }
    fun nextSong() {
        val queue = queueSongs().filter { it.mediaAvailable }
        val current = _state.value.selectedId
        val next = if (_state.value.shuffle) queue.filterNot { it.id == current }.randomOrNull()
            else queue.getOrNull(queue.indexOfFirst { it.id == current } + 1)
        if (next != null) select(next, preserveQueue = true)
    }
    fun previousSong() {
        if (_progress.value.currentMs > 3000) { seek(0); return }
        val queue = queueSongs().filter { it.mediaAvailable }
        val index = queue.indexOfFirst { it.id == _state.value.selectedId }
        if (index > 0) select(queue[index - 1], preserveQueue = true) else seek(0)
    }
    fun togglePlayback() { if (_state.value.isPlaying) pause() else play() }
    fun play() {
        try {
            if (mediaPlayer == null) {
                val selected = library.songs.value.firstOrNull { it.id == _state.value.selectedId } ?: return
                if (!selected.mediaAvailable) {
                    _state.value = _state.value.copy(error = "Esta canción no está disponible ahora. Puedes intentarlo de nuevo cuando vuelva a estar accesible.")
                    return
                }
                select(selected, autoplay = true, preserveQueue = true, resumePositionMs = _progress.value.currentMs)
                return
            }
            if (!playbackPhase.canControlPlayer) {
                playWhenPrepared = true
                return
            }
            if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
            resumeAfterTransientFocusLoss = false
            duckedForAudioFocus = false
            applyPlayerVolume()
            startMediaPlayerAfterFocusGain()
        } catch (exception: Exception) {
            _state.value = _state.value.copy(error = "Error de audio: ${exception.localizedMessage}")
        }
    }
    fun pause() {
        resumeAfterTransientFocusLoss = false
        duckedForAudioFocus = false
        playWhenPrepared = false
        if (playbackPhase.phase == PlaybackPhase.PLAYING) {
            val current = mediaPlayer
            try {
                val position = current?.currentPosition
                current?.pause()
                playbackPhase.onPlaybackPaused()
                if (position != null) _progress.value = _progress.value.copy(currentMs = position)
            } catch (exception: IllegalStateException) {
                current?.let { handleMediaPlayerError(it, "Error de audio: ${exception.localizedMessage}") }
            }
        }
        applyPlayerVolume()
        _state.value = _state.value.copy(isPlaying = false)
        notifyOutputRoutingObservers()
        publishSession(true)
        persistPlaybackState(_progress.value.currentMs)
        audioManager.abandonAudioFocusRequest(focusRequest)
        PlaybackService.stopForegroundPlayback()
    }
    fun seek(ms: Int) {
        if (!playbackPhase.canControlPlayer) return
        val target = ms.coerceIn(0, _progress.value.durationMs)
        val current = mediaPlayer ?: return
        try {
            current.seekTo(target)
        } catch (exception: IllegalStateException) {
            handleMediaPlayerError(current, "Error de audio: ${exception.localizedMessage}")
            return
        }
        _progress.value = _progress.value.copy(currentMs = target)
        publishSession(true)
        persistPlaybackState(target)
    }
    fun skip(ms: Int) = seek(_progress.value.currentMs + ms)
    fun toggleRepeat() { _state.value = _state.value.copy(repeat = !_state.value.repeat); persistPlaybackState() }
    fun toggleShuffle() { _state.value = _state.value.copy(shuffle = !_state.value.shuffle); persistPlaybackState() }
    fun toggleFavorite() { _state.value.selectedId?.let(library::toggleFavorite) }
    private fun persistPlaybackState(positionMs: Int? = null) {
        val currentPosition = positionMs ?: runCatching {
            mediaPlayer?.takeIf { playbackPhase.canControlPlayer }?.currentPosition
        }.getOrNull() ?: _progress.value.currentMs
        val current = _state.value
        val selectedSong = library.songs.value.firstOrNull { it.id == current.selectedId }
        preferences.edit().apply {
            if (current.selectedId == null) remove("selectedSongID")
            else putString("selectedSongID", current.selectedId)
            putString("selectedContentUri", selectedSong?.contentUri)
            putString("selectedImportSourceIdentity", selectedSong?.let { song ->
                song.importSourceIdentity ?: song.contentUri?.let {
                    mediaStoreIdentity(it, song.fileName, song.sizeBytes, song.importedAt)
                }
            })
            putString("selectedAudioFingerprint", selectedSong?.privateAudioFingerprint)
            putString("selectedMediaStoreVolume", selectedSong?.mediaStoreVolume)
            putInt("positionMs", currentPosition.coerceAtLeast(0))
            putBoolean("hasCustomQueue", current.queueIds != null)
            putString("queueIds", serializePlaybackQueue(current.queueIds))
            putBoolean("shuffle", current.shuffle)
            putBoolean("repeat", current.repeat)
            apply()
        }
        lastProgressPersistedAtMs = SystemClock.elapsedRealtime()
    }
    fun persistCurrentPlaybackState() = persistPlaybackState()
    fun setVolume(value: Float) {
        val volume = value.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = volume)
        applyPlayerVolume()
    }

    private fun onAudioFocusChange(change: Int) {
        logAudioRoute("audio_focus=${focusChangeLabel(change)} playing=${_state.value.isPlaying}")
        logAudioRouteSnapshot("audio_focus_${focusChangeLabel(change)}")
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                duckedForAudioFocus = false
                applyPlayerVolume()
                if (resumeAfterTransientFocusLoss) {
                    resumeAfterTransientFocusLoss = false
                    startMediaPlayerAfterFocusGain()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (_state.value.isPlaying) {
                    duckedForAudioFocus = true
                    applyPlayerVolume()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_state.value.isPlaying) {
                    resumeAfterTransientFocusLoss = true
                    duckedForAudioFocus = false
                    val current = mediaPlayer
                    try {
                        current?.pause()
                        playbackPhase.onPlaybackPaused()
                    } catch (exception: IllegalStateException) {
                        current?.let { handleMediaPlayerError(it, "Error de audio: ${exception.localizedMessage}") }
                        return
                    }
                    applyPlayerVolume()
                    _state.value = _state.value.copy(isPlaying = false)
                    persistPlaybackState(runCatching { current?.currentPosition }.getOrNull() ?: _progress.value.currentMs)
                    notifyOutputRoutingObservers()
                    publishSession(true)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeAfterTransientFocusLoss = false
                pause()
            }
        }
    }

    private fun focusVolumeMultiplier(): Float = if (duckedForAudioFocus) 0.2f else 1f

    private fun applyPlayerVolume() {
        if (!playbackPhase.canControlPlayer) return
        val player = mediaPlayer ?: return
        val volume = _state.value.volume * focusVolumeMultiplier()
        try {
            player.setVolume(volume, volume)
        } catch (exception: IllegalStateException) {
            handleMediaPlayerError(player, "Error de audio: ${exception.localizedMessage}")
        }
    }

    private fun startMediaPlayerAfterFocusGain() {
        val player = mediaPlayer ?: return
        if (!playbackPhase.canControlPlayer) return
        try {
            if (player.isPlaying) return
            if (player.duration > 0 && player.currentPosition >= player.duration) player.seekTo(0)
            player.start()
            playbackPhase.onPlaybackStarted()
            hasStartedPlayback = true
            _state.value = _state.value.copy(isPlaying = true, isPreparing = false, error = null)
            notifyOutputRoutingObservers()
            publishSession(true)
            application.startForegroundService(Intent(application, PlaybackService::class.java))
        } catch (exception: Exception) {
            handleMediaPlayerError(player, "Error de audio: ${exception.localizedMessage}")
        }
    }

    private fun handleMediaPlayerError(failed: MediaPlayer, message: String) {
        if (mediaPlayer !== failed || !playbackPhase.onError()) return
        logAudioRoute("player_error player=${playerToken(failed)}")
        logAudioRouteSnapshot("player_error_snapshot", failed)
        handler.removeCallbacks(ticker)
        playWhenPrepared = false
        resumeAfterTransientFocusLoss = false
        duckedForAudioFocus = false
        hasStartedPlayback = false
        audioManager.abandonAudioFocusRequest(focusRequest)
        PlaybackService.stopForegroundPlayback()
        _state.value = _state.value.copy(isPlaying = false, isPreparing = false, error = message)
        releasePlayer(preserveErrorPhase = true)
        publishSession(true)
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
            _state.value = PlayerState(repeat = _state.value.repeat, shuffle = _state.value.shuffle)
            _progress.value = PlaybackProgress()
            persistPlaybackState(0)
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
        val metadataKey = "${current?.id}:${current?.title}:${current?.artist}:${current?.album}:${_progress.value.durationMs}:$artworkRequestKey:${currentArtwork?.let(System::identityHashCode)}"
        if (publishedMetadataKey != metadataKey) {
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
            publishedMetadataKey = metadataKey
        }
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
        persistPlaybackState()
        hasStartedPlayback = false
        releasePlayer()
        audioManager.abandonAudioFocusRequest(focusRequest)
        _state.value = _state.value.copy(isPlaying = false, isPreparing = false)
        publishSession(true)
    }
    fun availableAudioOutputs(): List<AudioDeviceInfo> =
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()

    fun currentRoutedAudioOutput(): AudioDeviceInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        if (!playbackPhase.canControlPlayer) return null
        return runCatching { mediaPlayer?.takeIf { it.isPlaying }?.routedDevice }.getOrNull()
    }

    fun hasPreparedMediaPlayer(): Boolean = playbackPhase.canControlPlayer

    fun isPreparingMediaPlayer(): Boolean = playbackPhase.isPreparing

    fun setPreferredAudioOutput(device: AudioDeviceInfo?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            logAudioRoute("setPreferredDevice target=${deviceToken(device)} result=unsupported_api")
            return false
        }
        val current = mediaPlayer ?: run {
            logAudioRoute("setPreferredDevice target=${deviceToken(device)} result=no_player")
            return false
        }
        if (playbackPhase.isPreparing) {
            preferredOutputDeviceId = device?.id
            logAudioRoute("setPreferredDevice target=${deviceToken(device)} result=deferred_until_prepared")
            return true
        }
        if (!playbackPhase.canControlPlayer) {
            logAudioRoute("setPreferredDevice target=${deviceToken(device)} result=player_not_prepared phase=${playbackPhase.phase}")
            return false
        }
        val accepted = runCatching { current.setPreferredDevice(device) }.getOrDefault(false)
        logAudioRoute("setPreferredDevice target=${deviceToken(device)} accepted=$accepted")
        logAudioRouteSnapshot("setPreferredDevice_result")
        if (accepted) {
            preferredOutputDeviceId = device?.id
            notifyOutputRoutingObservers()
        }
        return accepted
    }

    fun clearPreferredAudioOutput(): Boolean {
        val previousPreferredId = preferredOutputDeviceId
        preferredOutputDeviceId = null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            logAudioRoute("clearPreferredDevice previousId=$previousPreferredId result=unsupported_api")
            return true
        }
        val current = mediaPlayer ?: run {
            logAudioRoute("clearPreferredDevice previousId=$previousPreferredId result=no_player_preference_cleared")
            return true
        }
        if (!playbackPhase.canControlPlayer) {
            logAudioRoute("clearPreferredDevice previousId=$previousPreferredId result=deferred_until_prepared")
            return true
        }
        val accepted = runCatching { current.setPreferredDevice(null) }.getOrDefault(false)
        logAudioRoute("clearPreferredDevice previousId=$previousPreferredId accepted=$accepted")
        logAudioRouteSnapshot("clearPreferredDevice_result")
        notifyOutputRoutingObservers()
        return accepted
    }

    fun observeAudioOutputRouting(observer: (AudioDeviceInfo?) -> Unit): AutoCloseable {
        outputRoutingObservers.add(observer)
        attachRoutingListenerIfNeeded()
        attachDiagnosticOutputCallbackIfNeeded()
        observer(currentRoutedAudioOutput())
        return AutoCloseable {
            outputRoutingObservers.remove(observer)
            if (outputRoutingObservers.isEmpty()) {
                detachRoutingListener()
                detachDiagnosticOutputCallback()
            }
        }
    }

    private fun applyPreferredOutput(player: MediaPlayer) {
        val preferredId = preferredOutputDeviceId ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            preferredOutputDeviceId = null
            return
        }
        val device = availableAudioOutputs().firstOrNull { it.id == preferredId }
        if (device == null) {
            logAudioRoute("reapplyPreferredDevice targetId=$preferredId result=not_connected preference_preserved=true")
            return
        }
        val accepted = runCatching { player.setPreferredDevice(device) }.getOrDefault(false)
        logAudioRoute(
            "reapplyPreferredDevice target=${deviceToken(device)} accepted=$accepted " +
                "preference_preserved=true"
        )
    }

    private fun attachRoutingListenerIfNeeded() {
        if (outputRoutingObservers.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val current = mediaPlayer ?: return
        if (observedRoutingPlayer === current) return
        detachRoutingListener()
        val listener = AudioRouting.OnRoutingChangedListener {
            logAudioRouteSnapshot("onRoutingChanged")
            notifyOutputRoutingObservers()
        }
        val attached = runCatching {
            current.addOnRoutingChangedListener(listener, handler)
            true
        }.getOrDefault(false)
        if (attached) {
            observedRoutingPlayer = current
            routingChangedListener = listener
            logAudioRoute("routing_listener_registered player=${playerToken(current)}")
        }
    }

    private fun detachRoutingListener() {
        val oldPlayer = observedRoutingPlayer
        val oldListener = routingChangedListener
        if (oldPlayer != null && oldListener != null) {
            runCatching { oldPlayer.removeOnRoutingChangedListener(oldListener) }
            logAudioRoute("routing_listener_removed player=${playerToken(oldPlayer)}")
        }
        observedRoutingPlayer = null
        routingChangedListener = null
    }

    private fun notifyOutputRoutingObservers() {
        logAudioRouteSnapshot("routing_observers_notified")
        val routed = currentRoutedAudioOutput()
        outputRoutingObservers.toList().forEach { it(routed) }
    }

    private fun releasePlayer(preserveErrorPhase: Boolean = false) {
        handler.removeCallbacks(ticker)
        playWhenPrepared = false
        logAudioRouteSnapshot("player_release_begin")
        mediaPlayer?.let { logAudioRoute("player_release player=${playerToken(it)}") }
        detachRoutingListener()
        mediaPlayer?.let { player ->
            runCatching { player.release() }
                .onFailure { logAudioRoute("player_release_failed exception=${it.javaClass.simpleName}") }
        }
        mediaPlayer = null
        if (!preserveErrorPhase) playbackPhase.onReleased()
        logAudioRouteSnapshot("player_release_complete")
        notifyOutputRoutingObservers()
    }

    private fun attachDiagnosticOutputCallbackIfNeeded() {
        if (!diagnosticLoggingEnabled || diagnosticOutputCallbackRegistered) return
        runCatching { audioManager.registerAudioDeviceCallback(diagnosticOutputCallback, handler) }
            .onSuccess {
                diagnosticOutputCallbackRegistered = true
                logAudioRoute("output_device_callback_registered")
                logConnectedOutputSnapshot("output_device_callback_initial_snapshot")
            }
            .onFailure { logAudioRoute("output_device_callback_registration_failed") }
    }

    private fun detachDiagnosticOutputCallback() {
        if (!diagnosticOutputCallbackRegistered) return
        runCatching { audioManager.unregisterAudioDeviceCallback(diagnosticOutputCallback) }
        diagnosticOutputCallbackRegistered = false
        logAudioRoute("output_device_callback_removed")
    }

    private fun logDeviceChanges(event: String, devices: Array<out AudioDeviceInfo>) {
        if (!diagnosticLoggingEnabled) return
        val outputTokens = devices.filter { it.isSink }.joinToString { deviceToken(it) }.ifBlank { "none" }
        logAudioRoute("devices_$event outputs=$outputTokens")
        logConnectedOutputSnapshot("devices_${event}_snapshot")
        logAudioRouteSnapshot("devices_$event")
    }

    private fun logConnectedOutputSnapshot(event: String) {
        if (!diagnosticLoggingEnabled) return
        val outputTokens = runCatching { availableAudioOutputs() }
            .getOrDefault(emptyList())
            .filter { it.isSink }
            .joinToString { deviceToken(it) }
            .ifBlank { "none" }
        logAudioRoute("$event connected_outputs=$outputTokens")
    }

    private fun logAudioRouteSnapshot(event: String, player: MediaPlayer? = mediaPlayer) {
        if (!diagnosticLoggingEnabled) return
        if (playbackPhase.isPreparing || playbackPhase.phase == PlaybackPhase.ERROR) {
            logAudioRoute("$event player=${playerToken(player)} phase=${playbackPhase.phase}")
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            logAudioRoute("$event api=${Build.VERSION.SDK_INT} player=${playerToken(player)}")
            return
        }
        val routed = runCatching { player?.routedDevice }.getOrNull()
        val preferred = runCatching { player?.preferredDevice }.getOrNull()
        val playing = runCatching { player?.isPlaying }.getOrNull() ?: false
        logAudioRoute(
            "$event player=${playerToken(player)} playing=$playing " +
                "routed=${deviceToken(routed)} preferred=${deviceToken(preferred)}"
        )
    }

    private fun logAudioRoute(message: String) {
        if (diagnosticLoggingEnabled) Log.d(AUDIO_ROUTE_TAG, message)
    }

    private fun focusChangeLabel(change: Int): String = when (change) {
        AudioManager.AUDIOFOCUS_GAIN -> "GAIN"
        AudioManager.AUDIOFOCUS_LOSS -> "LOSS"
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> "LOSS_TRANSIENT"
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> "LOSS_TRANSIENT_CAN_DUCK"
        else -> "OTHER_$change"
    }

    private fun deviceToken(device: AudioDeviceInfo?): String =
        device?.let { "type=${it.type},id=${it.id}" } ?: "none"

    private fun playerToken(player: MediaPlayer?): String =
        player?.let { System.identityHashCode(it).toString() } ?: "none"
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
    fun availableAudioOutputs() = engine.availableAudioOutputs()
    fun currentRoutedAudioOutput() = engine.currentRoutedAudioOutput()
    fun hasPreparedMediaPlayer() = engine.hasPreparedMediaPlayer()
    fun isPreparingMediaPlayer() = engine.isPreparingMediaPlayer()
    fun setPreferredAudioOutput(device: AudioDeviceInfo) = engine.setPreferredAudioOutput(device)
    fun clearPreferredAudioOutput() = engine.clearPreferredAudioOutput()
    fun observeAudioOutputRouting(observer: (AudioDeviceInfo?) -> Unit) = engine.observeAudioOutputRouting(observer)
    fun stopForTaskRemoval() = engine.stopForTaskRemoval()
    fun persistCurrentPlaybackState() = engine.persistCurrentPlaybackState()
}
