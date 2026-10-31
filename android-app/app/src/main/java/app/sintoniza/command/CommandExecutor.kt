package app.sintoniza.command

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import app.sintoniza.adb.LocalAdb
import app.sintoniza.media.MediaSessions
import kotlin.math.roundToInt

/** Executa um [Command] no aparelho. Bloqueante: chame fora da main thread. */
class CommandExecutor(
    private val context: Context,
    private val adb: LocalAdb = LocalAdb(context),
    private val launcher: AppLauncher = AppLauncher(context),
    private val media: MediaSessions = MediaSessions(context),
) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val power = context.getSystemService(PowerManager::class.java)

    fun execute(command: Command) {
        Log.i(TAG, "Executando $command")
        when (command) {
            is Command.OpenApp -> {
                val intent = launcher.resolve(command.appId, command.spokenName)
                if (intent == null) {
                    Log.w(TAG, "App não encontrado: ${command.appId ?: command.spokenName}")
                    return
                }
                wakeScreen()
                startActivity(intent)
            }

            is Command.YouTubeSearch -> {
                val intent = launcher.youtubeSearch(command.query)
                if (intent == null) {
                    Log.w(TAG, "YouTube não instalado ou sem suporte a busca")
                    return
                }
                wakeScreen()
                startActivity(intent)
            }

            is Command.Media -> when (command.action) {
                MediaAction.FAST_FORWARD -> seek(SEEK_STEP_MS, command.action.keyCode)
                MediaAction.REWIND -> seek(-SEEK_STEP_MS, command.action.keyCode)
                else -> sendMediaKey(command.action.keyCode)
            }
            is Command.Volume -> changeVolume(command)
            is Command.Key -> pressKey(command.key, command.times)
        }
    }

    private fun startActivity(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        // Sem a permissão de sobreposição o Android 10+ ignora, em silêncio, telas abertas
        // em segundo plano. O ADB não tem essa restrição.
        if (!Settings.canDrawOverlays(context) && adb.isEnabled) {
            adb.startActivity(intent).onSuccess { return }.onFailure { Log.w(TAG, "am start falhou", it) }
        }
        context.startActivity(intent)
    }

    /** Pula pela sessão de mídia; sem acesso, tenta concedê-lo via ADB; por fim, a tecla. */
    private fun seek(deltaMs: Long, fallbackKeyCode: Int) {
        if (media.seekBy(deltaMs)) return
        if (!media.hasAccess && adb.isEnabled && adb.grantMediaAccess().isSuccess && media.seekBy(deltaMs)) return
        Log.w(TAG, "Sem sessão de mídia para pular; enviando tecla")
        sendMediaKey(fallbackKeyCode)
    }

    private fun sendMediaKey(keyCode: Int) {
        val now = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
    }

    private fun changeVolume(command: Command.Volume) {
        val stream = AudioManager.STREAM_MUSIC
        val flags = AudioManager.FLAG_SHOW_UI
        if (audio.isVolumeFixed) Log.w(TAG, "Este aparelho usa volume fixo; o ajuste pode não ter efeito")

        val max = audio.getStreamMaxVolume(stream)
        val current = audio.getStreamVolume(stream)
        val step = (max * command.percent / 100.0).roundToInt().coerceAtLeast(1)

        when (command.action) {
            VolumeAction.UP -> audio.setStreamVolume(stream, (current + step).coerceAtMost(max), flags)
            VolumeAction.DOWN -> audio.setStreamVolume(stream, (current - step).coerceAtLeast(0), flags)
            VolumeAction.SET -> audio.setStreamVolume(stream, (max * command.level / 100.0).roundToInt(), flags)
            VolumeAction.MUTE -> audio.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, flags)
            VolumeAction.UNMUTE -> audio.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, flags)
        }
    }

    private fun pressKey(key: TvKey, times: Int) {
        if (adb.isEnabled) {
            wakeScreen()
            adb.pressKeys(key.keyCode, times).onFailure { Log.w(TAG, "Falha ao enviar tecla pelo ADB", it) }
            return
        }
        // Sem ADB, só "início" tem equivalente na API pública.
        if (key == TvKey.HOME) {
            wakeScreen()
            startActivity(launcher.homeIntent())
        } else {
            Log.w(TAG, "Navegação ($key) requer o controle avançado (ADB)")
        }
    }

    /** Acorda a tela se o aparelho estiver em repouso. */
    private fun wakeScreen() {
        if (power.isInteractive) return
        if (adb.isEnabled && adb.shell("input keyevent KEYCODE_WAKEUP").isSuccess) return

        @Suppress("DEPRECATION") // Não há substituto público para acordar a tela a partir de um serviço.
        power.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "sintoniza:wake",
        ).acquire(WAKE_TIMEOUT_MS)
    }

    private companion object {
        const val TAG = "Sintoniza"
        const val WAKE_TIMEOUT_MS = 3_000L
        const val SEEK_STEP_MS = 15_000L
    }
}
