package mx.musicalesdoris.reproductorlocalandroid

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import mx.musicalesdoris.reproductorlocalandroid.library.PlaylistCatalogState
import mx.musicalesdoris.reproductorlocalandroid.library.Playlist
import mx.musicalesdoris.reproductorlocalandroid.library.FolderGroupingState
import mx.musicalesdoris.reproductorlocalandroid.library.FolderSummary
import mx.musicalesdoris.reproductorlocalandroid.library.groupFoldersOnIo
import mx.musicalesdoris.reproductorlocalandroid.library.playlists.buildPlaylistPlaybackQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException

data class PlayerState(
    val selectedId: String? = null,
    val isPlaying: Boolean = false,
    val currentMs: Int = 0,
    val durationMs: Int = 0,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffle: Boolean = false,
    val error: String? = null,
    val isImporting: Boolean = false,
    val isPreparing: Boolean = false,
    val selectedFileAvailable: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSeek: Boolean = false,
    val queueSongIds: List<String> = emptyList(),
    val queueIndex: Int = -1,
    val playedSongIds: List<String> = emptyList(),
    val upcomingSongIds: List<String> = emptyList()
)

private data class FolderGroupingInput(
    val songs: List<Song>,
    val availability: Map<String, Boolean>,
    val key: List<Triple<String, String, Boolean>>
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    val library = SongLibrary(application)
    private val playlistRepository = JsonPlaylistCatalogRepository(java.io.File(application.filesDir, "playlists.json"))
    private val playlistMutex = Mutex()
    private val _playlistCatalog = kotlinx.coroutines.flow.MutableStateFlow(PlaylistCatalogState())
    val playlistCatalog = _playlistCatalog.asStateFlow()
    private val _catalogRecoveryInProgress = kotlinx.coroutines.flow.MutableStateFlow(false)
    val catalogRecoveryInProgress = _catalogRecoveryInProgress.asStateFlow()
    private val _folderGrouping = kotlinx.coroutines.flow.MutableStateFlow(FolderGroupingState())
    val folderGrouping = _folderGrouping.asStateFlow()
    private val preferences = application.getSharedPreferences("player", 0)
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    private data class PendingPlayback(
        val songId: String,
        val queue: List<Song>,
        val autoplay: Boolean,
        val shuffleMode: Boolean? = null,
        val startAtShuffledDefault: Boolean = false
    )
    private var pendingPlayback: PendingPlayback? = null
    private var controller: MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var queueGeneration = 0L

    private val controllerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            controller?.let(::synchronizeFromPlayer)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            controller?.let(::synchronizeFromPlayer)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId
            val previousId = _state.value.selectedId
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                previousId != null && previousId != id && previousId in _state.value.queueSongIds) {
                _state.value = _state.value.copy(playedSongIds = (_state.value.playedSongIds + previousId).distinct())
            }
            if (id != null) preferences.edit().putString("selectedSongID", id).apply()
            controller?.let {
                synchronizeFromPlayer(it)
                _state.value = _state.value.copy(error = null)
            }
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            controller?.let(::synchronizeFromPlayer)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            controller?.let(::synchronizeFromPlayer)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            controller?.let(::synchronizeFromPlayer)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            controller?.let(::synchronizeFromPlayer)
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val activeController = controller
            val id = activeController?.currentMediaItem?.mediaId
            val song = id?.let { selectedId -> library.songs.value.firstOrNull { it.id == selectedId } }
            val extension = song?.fileName?.substringAfterLast('.', "unknown") ?: "unknown"
            val playerDuration = activeController?.duration ?: -1L
            val position = activeController?.currentPosition ?: -1L
            val playbackState = activeController?.playbackState ?: -1
            val causeTypes = generateSequence(error.cause) { it.cause }
                .take(5).joinToString("->") { it.javaClass.simpleName }
            _state.value = _state.value.copy(
                isPlaying = false,
                isPreparing = false,
                error = "No se pudo reproducir ${song?.title ?: "el audio"}; verificando el archivo"
            )
            viewModelScope.launch {
                val verification = withContext(Dispatchers.IO) {
                    val available = song?.let { library.refreshAvailability(it.id) } == true
                    val file = song?.let(library::fileFor)
                    val readable = available && file != null &&
                        runCatching { file.canRead() && file.length() > 0L }.getOrDefault(false)
                    readable to (file?.let { runCatching { it.length() }.getOrDefault(-1L) } ?: -1L)
                }
                if (_state.value.selectedId == id && controller?.currentMediaItem?.mediaId == id) {
                    _state.value = _state.value.copy(
                        selectedFileAvailable = verification.first,
                        error = playbackSourceErrorMessage(song?.title, verification.first)
                    )
                }
                Log.e(PLAYBACK_LOG_TAG,
                    "Media3 error code=${error.errorCode} type=${error.javaClass.simpleName} causes=$causeTypes " +
                        "mediaId=${id ?: "none"} extension=$extension bytes=${verification.second} " +
                        "catalogDurationMs=${song?.duration?.times(1000)?.toLong() ?: -1L} " +
                        "playerDurationMs=$playerDuration positionMs=$position playbackState=$playbackState")
            }
        }
    }

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { library.initializeCatalog() }
            controller?.let(::restoreSavedSelectionIfNeeded)
            if (loaded) withContext(Dispatchers.IO) { library.scanMissingMetadata() }
        }
        viewModelScope.launch {
            combine(library.songs, library.availabilityBySongId, ::folderGroupingInput)
                .distinctUntilChanged { old, new -> old.key == new.key }
                .collectLatest { input ->
                    _folderGrouping.value = FolderGroupingState(isLoading = true)
                    val availableSongs = input.songs.filter { input.availability[it.id] == true }
                    val folders = groupFoldersOnIo(availableSongs, library::fileFor)
                    _folderGrouping.value = FolderGroupingState(isLoading = false, folders = folders)
                }
        }
        refreshPlaylists()
        val token = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(application, token).buildAsync().also { future ->
            future.addListener({
                try {
                    val connected = future.get()
                    controller = connected
                    connected.addListener(controllerListener)
                    synchronizeFromPlayer(connected)
                    val pending = pendingPlayback
                    if (pending != null) {
                        pendingPlayback = null
                        applyPlayback(pending)
                } else {
                    restoreSavedSelectionIfNeeded(connected)
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
                    synchronizeFromPlayer(player)
                }
                delay(500)
            }
        }
    }

    private fun folderGroupingInput(songs: List<Song>, availability: Map<String, Boolean>): FolderGroupingInput =
        FolderGroupingInput(
            songs = songs,
            availability = availability,
            key = songs.map { song -> Triple(song.id, song.fileName, availability[song.id] == true) }
        )

    /** Copies the active session snapshot into UI state; never issues playback commands. */
    private fun synchronizeFromPlayer(player: MediaController) {
        val item = player.currentMediaItem
        val id = item?.mediaId
        val song = id?.let { mediaId -> library.songs.value.firstOrNull { it.id == mediaId } }
        val actualDuration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L }
            ?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
        val queueSnapshot = Media3QueueController(player).snapshot()
        val nextAvailable = player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) &&
                canAdvanceNext(item != null, player.hasNextMediaItem(), player.repeatMode,
                    Player.REPEAT_MODE_OFF, player.shuffleModeEnabled)
        _state.value = _state.value.withSessionSnapshot(PlaybackSessionSnapshot(
            songId = id,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            durationMs = actualDuration ?: (song?.duration?.times(1000))?.toInt()?.coerceAtLeast(0) ?: 0,
            isPreparing = player.playbackState == Player.STATE_BUFFERING,
            fileAvailable = song?.let(library::isAvailable) ?: false,
            repeatMode = player.repeatMode,
            shuffle = player.shuffleModeEnabled,
            canPrevious = item != null,
            canNext = nextAvailable,
            canSeek = item != null && actualDuration != null && player.playbackState != Player.STATE_IDLE,
            queueSongIds = queueSnapshot.ids,
            queueIndex = queueSnapshot.currentIndex,
            upcomingSongIds = queueSnapshot.upcomingIds
        ))
    }

    fun selectedSong(songs: List<Song>): Song? = songs.firstOrNull { it.id == _state.value.selectedId }

    private fun restoreSavedSelectionIfNeeded(player: MediaController) {
        if (library.catalogLoading.value || !shouldRestoreSavedSelection(player.currentMediaItem != null)) return
        val saved = preferences.getString("selectedSongID", null)
        val initial = library.songs.value.firstOrNull { it.id == saved }
            ?: library.songs.value.firstOrNull()
        if (initial != null) select(initial, autoplay = false)
    }

    fun select(song: Song, autoplay: Boolean = true) = select(song, library.songs.value, autoplay)

    fun select(song: Song, queue: List<Song>, autoplay: Boolean = true) {
        selectFromQueue(song, queue, autoplay, shuffleMode = null, startAtShuffledDefault = false)
    }

    private fun selectFromQueue(
        song: Song,
        queue: List<Song>,
        autoplay: Boolean,
        shuffleMode: Boolean?,
        startAtShuffledDefault: Boolean
    ) {
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
        val pending = PendingPlayback(song.id, availableQueue, autoplay, shuffleMode, startAtShuffledDefault)
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

    fun playCollection(
        songs: List<Song>,
        startingWith: Song? = null,
        autoplay: Boolean = true,
        shuffled: Boolean? = null
    ) {
        val available = songs.filter(library::isAvailable)
        if (available.isEmpty()) {
            _state.value = _state.value.copy(error = "La lista no contiene canciones disponibles para reproducir")
            return
        }
        val selected = startingWith?.let { requested -> available.firstOrNull { it.id == requested.id } }
            ?: available.first()
        selectFromQueue(
            selected, available, autoplay, shuffleMode = shuffled,
            startAtShuffledDefault = shuffled == true && startingWith == null
        )
    }

    fun playPlaylist(playlist: Playlist, startingSongId: String? = null, shuffled: Boolean? = null) {
        val queue = buildPlaylistPlaybackQueue(playlist, library.songs.value, library::isAvailable, startingSongId)
        if (queue.songs.isEmpty()) {
            _state.value = _state.value.copy(error = "La lista no contiene canciones disponibles para reproducir")
            return
        }
        val start = startingSongId?.let { queue.songs.getOrNull(queue.startIndex) }
        playCollection(queue.songs, startingWith = start, shuffled = shuffled)
    }

    fun togglePlaylistShuffle(playlist: Playlist) {
        val playlistQueue = buildPlaylistPlaybackQueue(playlist, library.songs.value, library::isAvailable)
        if (playlistQueue.songs.isEmpty()) {
            _state.value = _state.value.copy(error = "La lista no contiene canciones disponibles para reproducir")
            return
        }

        val activePlayer = controller
        val activeQueueIds = activePlayer?.let { player ->
            (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
        } ?: _state.value.queueSongIds
        val selectedSongId = activePlayer?.currentMediaItem?.mediaId ?: _state.value.selectedId
        val shuffleEnabled = activePlayer?.shuffleModeEnabled ?: _state.value.shuffle
        val decision = decidePlaylistShuffleToggle(
            playlistQueueIds = playlistQueue.songs.map(Song::id),
            activeQueueIds = activeQueueIds,
            selectedSongId = selectedSongId,
            currentShuffleEnabled = shuffleEnabled
        )

        if (decision.shouldStartPlaylist) {
            playCollection(playlistQueue.songs, shuffled = decision.shuffleEnabled)
            return
        }

        val player = activePlayer ?: run {
            _state.value = _state.value.copy(error = "El reproductor no está conectado; vuelve a intentarlo")
            return
        }
        player.shuffleModeEnabled = decision.shuffleEnabled
        synchronizeFromPlayer(player)
        _state.value = _state.value.copy(error = null)
    }

    private fun applyPlayback(pending: PendingPlayback) {
        val connected = controller ?: run { pendingPlayback = pending; return }
        val generation = ++queueGeneration
        _state.value = _state.value.copy(playedSongIds = emptyList())
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
            installMedia3Queue(
                sink = object : Media3QueueSink {
                    override fun setMediaItems(items: List<MediaItem>, startIndex: Int) =
                        connected.setMediaItems(items, startIndex, 0L)
                    override fun setShuffleModeEnabled(enabled: Boolean) { connected.shuffleModeEnabled = enabled }
                    override fun seekToDefaultPosition() { connected.seekToDefaultPosition() }
                    override fun prepare() { connected.prepare() }
                    override fun play() { connected.play() }
                },
                items = items,
                startIndex = startIndex,
                shuffleMode = pending.shuffleMode,
                startAtShuffledDefault = pending.startAtShuffledDefault,
                autoplay = pending.autoplay
            )
            Log.d(PLAYBACK_LOG_TAG,
                "Queue ready size=${items.size} startIndex=$startIndex shuffle=${connected.shuffleModeEnabled} " +
                    "randomStart=${pending.startAtShuffledDefault}")
        }
    }

    fun togglePlayback() {
        val player = controller
        if (player == null) {
            _state.value = _state.value.copy(error = "El reproductor todavía se está iniciando")
            return
        }
        val selected = library.songs.value.firstOrNull { it.id == _state.value.selectedId }
        if (player.isPlaying) {
            player.pause()
            return
        }
        if (selected == null || !library.isAvailable(selected)) {
            _state.value = _state.value.copy(
                isPlaying = false,
                isPreparing = false,
                selectedFileAvailable = false,
                error = "El archivo de audio seleccionado no está disponible"
            )
            return
        }
        if (player.currentMediaItem == null) select(selected, autoplay = true)
        else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
    }

    fun next() {
        val player = controller ?: return
        if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) && canAdvanceNext(
                hasCurrentItem = player.currentMediaItem != null,
                hasNextItem = player.hasNextMediaItem(),
                repeatMode = player.repeatMode,
                repeatOffMode = Player.REPEAT_MODE_OFF,
                shuffleEnabled = player.shuffleModeEnabled
            )) {
            player.seekToNextMediaItem()
        }
    }

    fun previous() {
        val player = controller ?: return
        val canSeekPrevious = player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        when (previousAction(player.currentPosition, player.currentMediaItem != null,
            player.hasPreviousMediaItem() && canSeekPrevious)) {
            PreviousAction.NONE -> Unit
            PreviousAction.RESTART_CURRENT -> player.seekTo(0)
            PreviousAction.GO_TO_PREVIOUS -> player.seekToPreviousMediaItem()
        }
    }

    fun seek(ms: Int) {
        val player = controller ?: return
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L } ?: return
        if (player.currentMediaItem == null) return
        val song = player.currentMediaItem?.mediaId?.let { id -> library.songs.value.firstOrNull { it.id == id } }
        val guard = seekEndGuardMs(song?.fileName?.substringAfterLast('.', ""))
        val target = clampSeekPosition(ms.toLong(), duration, guard) ?: return
        player.seekTo(target)
        _state.value = _state.value.copy(currentMs = target.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }

    fun skip(ms: Int) {
        val player = controller ?: return
        if (player.currentMediaItem == null || player.playbackState == Player.STATE_IDLE ||
            player.playbackState == Player.STATE_BUFFERING) return
        val actualDuration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L }
        val song = player.currentMediaItem?.mediaId?.let { id -> library.songs.value.firstOrNull { it.id == id } }
        val guard = seekEndGuardMs(song?.fileName?.substringAfterLast('.', ""))
        val target = calculateSkipTarget(
            currentPositionMs = player.currentPosition,
            durationMs = actualDuration,
            deltaMs = ms.toLong(),
            playbackEnded = player.playbackState == Player.STATE_ENDED,
            endGuardMs = guard
        ) ?: return
        player.seekTo(target)
        _state.value = _state.value.copy(
            currentMs = target.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            durationMs = actualDuration?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: _state.value.durationMs
        )
    }

    fun toggleRepeat() {
        val player = controller ?: run {
            _state.value = _state.value.copy(error = "El reproductor todavía se está iniciando")
            return
        }
        player.repeatMode = nextRepeatMode(player.repeatMode)
        synchronizeFromPlayer(player)
        _state.value = _state.value.copy(error = null)
    }

    fun toggleShuffle() {
        val player = controller ?: run {
            _state.value = _state.value.copy(error = "El reproductor todavía se está iniciando")
            return
        }
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        synchronizeFromPlayer(player)
        _state.value = _state.value.copy(error = null)
    }

    /** Selects an existing Media3 item without reinstalling or duplicating the active queue. */
    fun playQueueItem(index: Int) {
        val player = controller ?: return
        Media3QueueController(player).playItem(index)
    }

    fun removeQueueItem(index: Int): Boolean {
        val player = controller ?: return false
        val removed = Media3QueueController(player).removeItem(index)
        if (removed && player.mediaItemCount == 0) {
            preferences.edit().remove("selectedSongID").apply()
            _state.value = _state.value.copy(selectedId = null, playedSongIds = emptyList(), queueSongIds = emptyList(),
                upcomingSongIds = emptyList(), queueIndex = -1, isPlaying = false, isPreparing = false)
        }
        return removed
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int): Boolean {
        val player = controller ?: return false
        return Media3QueueController(player).moveItem(fromIndex, toIndex)
    }

    fun toggleFavorite() { _state.value.selectedId?.let(::toggleFavorite) }

    fun toggleFavorite(songId: String) {
        viewModelScope.launch(Dispatchers.IO) { library.toggleFavorite(songId) }
    }

    fun refreshPlaylists() {
        viewModelScope.launch {
            playlistMutex.withLock {
                _playlistCatalog.value = _playlistCatalog.value.copy(isLoading = true)
                try {
                    val playlists = withContext(Dispatchers.IO) { playlistRepository.load() }
                    _playlistCatalog.value = PlaylistCatalogState(playlists, isLoading = false)
                } catch (exception: Exception) {
                    _playlistCatalog.value = _playlistCatalog.value.copy(
                        isLoading = false,
                        isReadable = false,
                        needsRecovery = exception is PlaylistCatalogReadException,
                        error = "No se pudieron leer las listas: ${exception.localizedMessage ?: "error de persistencia"}"
                    )
                }
            }
        }
    }

    suspend fun createPlaylist(title: String, description: String? = null, artworkUri: Uri? = null): Boolean = mutatePlaylists {
        val bytes = artworkUri?.let(::readPlaylistArtwork)
        playlistRepository.create(title, description, bytes)
    }

    suspend fun renamePlaylist(id: String, title: String): Boolean = mutatePlaylists {
        playlistRepository.rename(id, title)
    }

    suspend fun updatePlaylist(
        id: String,
        title: String,
        description: String?,
        artworkUri: Uri?,
        removeArtwork: Boolean
    ): Boolean = mutatePlaylists {
        val bytes = artworkUri?.let(::readPlaylistArtwork)
        playlistRepository.updateDetails(id, title, description, bytes, removeArtwork)
    }

    fun playlistArtworkFile(path: String?): java.io.File? = PlaylistArtworkStore.pathFor(
        getApplication<Application>().filesDir, path
    )

    private fun readPlaylistArtwork(uri: Uri): ByteArray {
        return getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_PLAYLIST_ARTWORK_BYTES) { "La imagen supera el límite de 20 MB" }
                output.write(buffer, 0, count)
            }
            output.toByteArray().also { require(it.isNotEmpty()) { "La imagen está vacía" } }
        } ?: throw java.io.IOException("No se pudo leer la imagen seleccionada")
    }

    suspend fun deletePlaylist(id: String): Boolean = mutatePlaylists {
        playlistRepository.delete(id)
    }

    suspend fun addSongsToPlaylist(id: String, songIds: List<String>): Boolean = mutatePlaylists {
        playlistRepository.addSongs(id, songIds)
    }

    suspend fun removeSongFromPlaylist(id: String, songId: String): Boolean = mutatePlaylists {
        playlistRepository.removeSong(id, songId)
    }

    fun clearPlaylistError() {
        _playlistCatalog.value = _playlistCatalog.value.copy(error = null)
    }

    suspend fun recoverPlaylists(): Boolean = playlistMutex.withLock {
        val before = _playlistCatalog.value
        if (!before.needsRecovery) return@withLock false
        try {
            val recovered = withContext(Dispatchers.IO) {
                val backup = playlistRepository.recoverUnreadableCatalog()
                backup to playlistRepository.load()
            }
            _playlistCatalog.value = PlaylistCatalogState(
                playlists = recovered.second,
                isLoading = false,
                isReadable = true,
                recoveryMessage = "Se creó un catálogo nuevo. El original se conservó en ${recovered.first.name}."
            )
            true
        } catch (exception: Exception) {
            _playlistCatalog.value = before.copy(isLoading = false,
                error = "No se pudo recuperar el catálogo de listas: ${exception.localizedMessage ?: "error de persistencia"}")
            false
        }
    }

    private suspend fun mutatePlaylists(mutation: () -> Any?): Boolean = playlistMutex.withLock {
        val before = _playlistCatalog.value
        if (before.isLoading || !before.isReadable) {
            _playlistCatalog.value = before.copy(error = before.error ?: "No se pueden modificar las listas hasta poder leer el catálogo")
            return@withLock false
        }
        try {
            val updated = withContext(Dispatchers.IO) {
                val result = mutation()
                if (result == false) return@withContext null
                playlistRepository.load()
            } ?: return@withLock false
            _playlistCatalog.value = PlaylistCatalogState(updated, isLoading = false)
            true
        } catch (exception: Exception) {
            _playlistCatalog.value = before.copy(
                isLoading = false,
                isReadable = before.isReadable && exception !is PlaylistCatalogReadException,
                error = "No se pudo guardar la lista: ${exception.localizedMessage ?: "error de persistencia"}"
            )
            false
        }
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
                _state.value = PlayerState(
                    repeatMode = controller?.repeatMode ?: _state.value.repeatMode,
                    shuffle = controller?.shuffleModeEnabled ?: _state.value.shuffle
                )
            }
        }
    }

    fun clearError() {
        if (library.catalogNeedsRecovery.value) return
        _state.value = _state.value.copy(error = null)
        library.clearMessages()
    }

    fun recoverCatalog() {
        if (_catalogRecoveryInProgress.value) return
        _catalogRecoveryInProgress.value = true
        viewModelScope.launch {
            try {
                val recovered = withContext(Dispatchers.IO) { library.recoverUnreadableCatalog() }
                if (recovered) _state.value = _state.value.copy(error = null)
            } finally {
                _catalogRecoveryInProgress.value = false
            }
        }
    }

    override fun onCleared() {
        controller?.removeListener(controllerListener)
        controller?.release()
        controller = null
        controllerFuture?.cancel(true)
        controllerFuture = null
        super.onCleared()
    }

    private companion object {
        const val PLAYBACK_LOG_TAG = "SoniqPlayback"
        const val MAX_PLAYLIST_ARTWORK_BYTES = 20 * 1024 * 1024
    }

}
