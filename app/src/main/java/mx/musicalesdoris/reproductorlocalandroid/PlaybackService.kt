package mx.musicalesdoris.reproductorlocalandroid

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import android.util.Log

/** Owns the only audio player so playback survives Activity and ViewModel recreation. */
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                val mediaId = player.currentMediaItem?.mediaId ?: return
                val audioGroup = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO } ?: return
                val selected = (0 until audioGroup.length).firstOrNull(audioGroup::isTrackSelected) ?: 0
                val format = audioGroup.getTrackFormat(selected)
                Log.i(PLAYBACK_LOG_TAG,
                    "Audio format mediaId=$mediaId mime=${format.sampleMimeType ?: "unknown"} " +
                        "codecs=${format.codecs ?: "unknown"} sampleRate=${format.sampleRate} " +
                        "channels=${format.channelCount} bitrate=${format.bitrate}")
            }
        })
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private companion object {
        const val PLAYBACK_LOG_TAG = "SoniqPlayback"
    }
}
