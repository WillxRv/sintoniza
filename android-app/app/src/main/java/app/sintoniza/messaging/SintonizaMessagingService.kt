package app.sintoniza.messaging

import android.util.Log
import app.sintoniza.command.CommandExecutor
import app.sintoniza.command.CommandParser
import app.sintoniza.config.ServerConfigStore
import app.sintoniza.pairing.DeviceRegistry
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Recebe os comandos da skill via FCM. O Google entrega a mensagem e acorda o app,
 * então não precisamos de serviço rodando o tempo todo.
 *
 * Os callbacks rodam numa thread de trabalho do Firebase, por isso podemos bloquear.
 */
class SintonizaMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        if (!ServerConfigStore.initializeFirebase(this)) return
        runBlocking {
            withTimeoutOrNull(REGISTER_TIMEOUT_MS) {
                runCatching { DeviceRegistry().register(token) }
                    .onFailure { Log.w(TAG, "Falha ao registrar novo token", it) }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val raw = message.data["cmd"] ?: return
        val envelope = runCatching { CommandParser.parse(raw) }.getOrElse {
            Log.w(TAG, "Comando ignorado: ${it.message}")
            return
        }
        LastCommand.record(this, envelope.command)
        runCatching { CommandExecutor(applicationContext).execute(envelope.command) }
            .onFailure { Log.e(TAG, "Falha ao executar ${envelope.command}", it) }
    }

    private companion object {
        const val TAG = "Sintoniza"
        const val REGISTER_TIMEOUT_MS = 15_000L
    }
}
