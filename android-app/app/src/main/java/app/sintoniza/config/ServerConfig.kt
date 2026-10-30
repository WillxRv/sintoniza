package app.sintoniza.config

import android.content.Context
import app.sintoniza.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.json.JSONObject

/**
 * Projeto Firebase ao qual o app se conecta. Por padrão é o da instância oficial
 * (firebase.properties, embutido no build); quem hospeda a própria instância pode
 * trocar em tempo de execução (ver docs/AUTO-HOSPEDAGEM.md).
 */
data class ServerConfig(
    val projectId: String,
    val applicationId: String,
    val apiKey: String,
    val gcmSenderId: String,
) {
    val isComplete: Boolean
        get() = listOf(projectId, applicationId, apiKey, gcmSenderId).all { it.isNotBlank() }

    fun toOptions(): FirebaseOptions = FirebaseOptions.Builder()
        .setProjectId(projectId)
        .setApplicationId(applicationId)
        .setApiKey(apiKey)
        .setGcmSenderId(gcmSenderId)
        .build()

    fun toJson(): String = JSONObject()
        .put("projectId", projectId)
        .put("applicationId", applicationId)
        .put("apiKey", apiKey)
        .put("gcmSenderId", gcmSenderId)
        .toString()

    companion object {
        val builtIn = ServerConfig(
            projectId = BuildConfig.FIREBASE_PROJECT_ID,
            applicationId = BuildConfig.FIREBASE_APPLICATION_ID,
            apiKey = BuildConfig.FIREBASE_API_KEY,
            gcmSenderId = BuildConfig.FIREBASE_GCM_SENDER_ID,
        )

        /** @throws IllegalArgumentException se faltar algum campo. */
        fun fromJson(json: String): ServerConfig {
            val obj = JSONObject(json)
            return ServerConfig(
                projectId = obj.optString("projectId"),
                applicationId = obj.optString("applicationId"),
                apiKey = obj.optString("apiKey"),
                gcmSenderId = obj.optString("gcmSenderId"),
            ).also { require(it.isComplete) { "Config incompleta: precisa de projectId, applicationId, apiKey e gcmSenderId" } }
        }
    }
}

object ServerConfigStore {
    private const val PREFS = "server"
    private const val KEY_CUSTOM = "custom"

    fun current(context: Context): ServerConfig? =
        custom(context) ?: ServerConfig.builtIn.takeIf { it.isComplete }

    fun custom(context: Context): ServerConfig? =
        prefs(context).getString(KEY_CUSTOM, null)?.let { runCatching { ServerConfig.fromJson(it) }.getOrNull() }

    fun saveCustom(context: Context, config: ServerConfig) {
        prefs(context).edit().putString(KEY_CUSTOM, config.toJson()).commit()
    }

    fun clearCustom(context: Context) {
        prefs(context).edit().remove(KEY_CUSTOM).commit()
    }

    /** Inicializa o Firebase com a config atual. Retorna false se não houver config. */
    fun initializeFirebase(context: Context): Boolean {
        if (FirebaseApp.getApps(context).isNotEmpty()) return true
        val config = current(context) ?: return false
        FirebaseApp.initializeApp(context, config.toOptions())
        return true
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
