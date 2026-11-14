package mx.musicalesdoris.reproductorlocalandroid

internal enum class PlaybackPhase {
    IDLE,
    PREPARING,
    PREPARED,
    PLAYING,
    ERROR
}

internal class PlaybackPhaseTracker {
    var phase: PlaybackPhase = PlaybackPhase.IDLE
        private set

    val canControlPlayer: Boolean
        get() = phase == PlaybackPhase.PREPARED || phase == PlaybackPhase.PLAYING

    val isPreparing: Boolean
        get() = phase == PlaybackPhase.PREPARING

    fun beginPreparation() {
        phase = PlaybackPhase.PREPARING
    }

    fun onPrepared(): Boolean {
        if (phase != PlaybackPhase.PREPARING) return false
        phase = PlaybackPhase.PREPARED
        return true
    }

    fun onPlaybackStarted(): Boolean {
        if (!canControlPlayer) return false
        phase = PlaybackPhase.PLAYING
        return true
    }

    fun onPlaybackPaused(): Boolean {
        if (phase != PlaybackPhase.PLAYING) return false
        phase = PlaybackPhase.PREPARED
        return true
    }

    fun onError(): Boolean {
        if (phase == PlaybackPhase.IDLE || phase == PlaybackPhase.ERROR) return false
        phase = PlaybackPhase.ERROR
        return true
    }

    fun onReleased() {
        phase = PlaybackPhase.IDLE
    }
}
