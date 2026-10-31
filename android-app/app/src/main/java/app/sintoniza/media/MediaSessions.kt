package app.sintoniza.media

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock

/**
 * Controle direto da sessão de mídia do app que está tocando. Usado para avançar/retroceder:
 * o YouTube ignora as teclas de mídia de avanço, mas aceita seekTo.
 */
class MediaSessions(private val context: Context) {

    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val listener = ComponentName(context, MediaAccessService::class.java)

    val hasAccess: Boolean
        get() = context.getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(listener)

    /** Pula [deltaMs] a partir da posição atual. Retorna false se não houver mídia ou acesso. */
    fun seekBy(deltaMs: Long): Boolean {
        val controller = active() ?: return false
        val state = controller.playbackState ?: return false
        if ((state.actions and PlaybackState.ACTION_SEEK_TO) == 0L) return false

        val elapsed = if (state.state == PlaybackState.STATE_PLAYING) {
            ((SystemClock.elapsedRealtime() - state.lastPositionUpdateTime) * state.playbackSpeed).toLong()
        } else {
            0L
        }
        val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0 } ?: Long.MAX_VALUE
        controller.transportControls.seekTo((state.position + elapsed + deltaMs).coerceIn(0, duration))
        return true
    }

    /** Pacotes com sessão de mídia, ou null sem acesso a notificações. */
    fun packagesWithSession(): Set<String>? = try {
        manager.getActiveSessions(listener).map { it.packageName }.toSet()
    } catch (_: SecurityException) {
        null
    }

    private fun active(): MediaController? = try {
        manager.getActiveSessions(listener).firstOrNull { it.playbackState?.state in SEEKABLE_STATES }
    } catch (_: SecurityException) {
        null // sem acesso a notificações
    }

    private companion object {
        val SEEKABLE_STATES = setOf(
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_PAUSED,
            PlaybackState.STATE_BUFFERING,
        )
    }
}
