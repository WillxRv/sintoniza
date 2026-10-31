package app.sintoniza.adb

import android.content.Context
import android.content.Intent
import app.sintoniza.media.MediaAccessService
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File

/**
 * "Controle avançado": o app conversa com o ADB do próprio aparelho (127.0.0.1:5555)
 * para o que a API pública do Android não permite, como apertar setas/OK/voltar.
 *
 * Requer "Depuração USB" ligada nas opções do desenvolvedor. Na primeira conexão a TV
 * mostra "Permitir depuração?": é preciso aceitar (marcando "Sempre permitir").
 *
 * Todos os métodos são bloqueantes: chame fora da main thread.
 */
class LocalAdb(private val context: Context) {

    private val prefs = context.getSharedPreferences("adb", Context.MODE_PRIVATE)

    /** Se o usuário ativou e testou o controle avançado. */
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        private set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Executa um comando shell e devolve a saída. */
    fun shell(command: String): Result<String> = runCatching {
        Dadb.create(HOST, PORT, keyPair()).use { dadb ->
            val response = dadb.shell(command)
            check(response.exitCode == 0) { "exit ${response.exitCode}: ${response.allOutput.trim()}" }
            response.output
        }
    }

    fun pressKeys(keyCode: Int, times: Int): Result<String> =
        shell("input keyevent " + List(times) { keyCode }.joinToString(" "))

    /** Pacote do app em primeiro plano, ou null se não der para saber. */
    fun foregroundPackage(): String? =
        shell("dumpsys activity activities | grep -m1 mResumedActivity").getOrNull()
            ?.let { Regex(""" u\d+ ([\w.]+)/""").find(it)?.groupValues?.get(1) }

    /** Abre uma Activity pelo ADB, que não sofre o bloqueio de abrir telas em segundo plano. */
    fun startActivity(intent: Intent): Result<String> =
        shell("am start " + quote(intent.toUri(Intent.URI_INTENT_SCHEME)))

    /**
     * Ativa o controle avançado: conecta (disparando o aviso na TV, se for a primeira vez)
     * e já concede a permissão de sobreposição, que algumas TVs não deixam conceder pela interface.
     */
    fun enable(): Result<Unit> =
        shell("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
            .map {
                grantMediaAccess()
                isEnabled = true
            }
            .onFailure { isEnabled = false }

    /** Acesso às sessões de mídia (ver MediaSessions). TVs costumam não ter essa tela nas configurações. */
    fun grantMediaAccess(): Result<String> =
        shell("cmd notification allow_listener ${context.packageName}/${MediaAccessService::class.java.name}")

    fun disable() {
        isEnabled = false
    }

    private fun keyPair(): AdbKeyPair {
        val privateKey = File(context.filesDir, "adbkey")
        val publicKey = File(context.filesDir, "adbkey.pub")
        if (!privateKey.exists() || !publicKey.exists()) AdbKeyPair.generate(privateKey, publicKey)
        return AdbKeyPair.read(privateKey, publicKey)
    }

    companion object {
        private const val HOST = "127.0.0.1"
        private const val PORT = 5555
        private const val KEY_ENABLED = "enabled"

        /** Aspas simples para o shell do Android. */
        fun quote(arg: String): String = "'" + arg.replace("'", "'\\''") + "'"
    }
}
