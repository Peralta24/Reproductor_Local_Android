package mx.musicalesdoris.reproductorlocalandroid

import android.app.Service
import android.content.Intent
import android.os.IBinder

class PlaybackService : Service() {
    companion object {
        private var active: PlaybackService? = null
        fun stopForegroundPlayback() {
            active?.stopForeground(STOP_FOREGROUND_DETACH)
            active?.stopSelf()
        }
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val engine = PlaybackEngine.get(application as android.app.Application)
        val notification = engine.currentNotification() ?: run { stopSelf(); return START_NOT_STICKY }
        startForeground(42, notification)
        active = this
        return START_STICKY
    }
    override fun onDestroy() {
        if (active === this) active = null
        super.onDestroy()
    }
}
