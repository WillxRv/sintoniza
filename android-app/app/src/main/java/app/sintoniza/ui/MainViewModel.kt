package app.sintoniza.ui

import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.sintoniza.adb.LocalAdb
import app.sintoniza.config.ServerConfig
import app.sintoniza.config.ServerConfigStore
import app.sintoniza.messaging.LastCommand
import app.sintoniza.pairing.DeviceRegistry
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val serverConfigured: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val pairingCode: String? = null,
    val paired: Boolean = false,
    val overlayGranted: Boolean = false,
    val adbEnabled: Boolean = false,
    val adbBusy: Boolean = false,
    val adbError: String? = null,
    val lastCommand: String? = null,
    val usingCustomServer: Boolean = false,
    /** Config recebida por intent, aguardando confirmação do usuário. */
    val pendingConfig: ServerConfig? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val context get() = getApplication<Application>()
    private val adb = LocalAdb(app)
    private val _state = MutableStateFlow(
        UiState(
            serverConfigured = ServerConfigStore.initializeFirebase(app),
            usingCustomServer = ServerConfigStore.custom(app) != null,
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var registry: DeviceRegistry? = null
    private var pairingJob: Job? = null
    private var codeJob: Job? = null

    init {
        refreshLocalStatus()
        if (_state.value.serverConfigured) start() else _state.update { it.copy(loading = false) }
    }

    private fun start() {
        val registry = DeviceRegistry().also { registry = it }
        pairingJob?.cancel()
        pairingJob = viewModelScope.launch {
            try {
                registry.register()
                registry.pairedAt(registry.uid()).collect { pairedAt ->
                    _state.update { it.copy(paired = pairedAt != null, loading = false) }
                    when {
                        pairedAt != null -> stopCode()
                        codeJob == null -> newCode()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao conectar ao Firebase", e)
                _state.update { it.copy(loading = false, error = "Não foi possível conectar ao servidor: ${e.message}") }
            }
        }
    }

    /** Pareou (ou desistiu): o código atual não deve mais ser aceito. */
    private fun stopCode() {
        codeJob?.cancel()
        codeJob = null
        val code = _state.value.pairingCode ?: return
        _state.update { it.copy(pairingCode = null) }
        viewModelScope.launch { registry?.deletePairingCode(code) }
    }

    /** Gera um código e o renova antes de expirar enquanto a tela estiver aberta. */
    fun newCode() {
        val registry = registry ?: return
        codeJob?.cancel()
        codeJob = viewModelScope.launch {
            while (true) {
                val previous = _state.value.pairingCode
                try {
                    val code = registry.newPairingCode()
                    _state.update { it.copy(pairingCode = code, error = null) }
                    previous?.let { registry.deletePairingCode(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Falha ao gerar código", e)
                    _state.update { it.copy(error = "Não foi possível gerar o código: ${e.message}") }
                }
                delay(CODE_REFRESH_MS)
            }
        }
    }

    fun unpairAll() {
        val registry = registry ?: return
        viewModelScope.launch {
            pairingJob?.cancel()
            stopCode()
            runCatching { registry.unpairAll() }
                .onFailure { e -> _state.update { it.copy(error = "Falha ao desparear: ${e.message}") } }
            _state.update { it.copy(paired = false) }
            // O novo uid ainda não tem pairedAt: observa de novo e gera código.
            start()
        }
    }

    fun enableAdb() {
        _state.update { it.copy(adbBusy = true, adbError = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { adb.enable() }
            _state.update {
                it.copy(
                    adbBusy = false,
                    adbError = result.exceptionOrNull()?.let { e -> "Não conectou ao ADB: ${e.message}" },
                )
            }
            refreshLocalStatus()
        }
    }

    fun disableAdb() {
        adb.disable()
        refreshLocalStatus()
    }

    /** Chamado ao voltar para o app (as permissões podem ter mudado). */
    fun refreshLocalStatus() {
        _state.update {
            it.copy(
                overlayGranted = Settings.canDrawOverlays(context),
                adbEnabled = adb.isEnabled,
                lastCommand = LastCommand.read(context)?.first,
            )
        }
    }

    fun offerConfig(json: String) {
        runCatching { ServerConfig.fromJson(json) }
            .onSuccess { config -> _state.update { it.copy(pendingConfig = config) } }
            .onFailure { e -> _state.update { it.copy(error = "Config de servidor inválida: ${e.message}") } }
    }

    fun dismissPendingConfig() = _state.update { it.copy(pendingConfig = null) }

    /** Salva a config. O Firebase só pode ser reinicializado reiniciando o processo. */
    fun applyPendingConfig(): Boolean {
        val config = _state.value.pendingConfig ?: return false
        ServerConfigStore.saveCustom(context, config)
        return true
    }

    fun resetToOfficialServer(): Boolean {
        if (ServerConfigStore.custom(context) == null) return false
        ServerConfigStore.clearCustom(context)
        return true
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        // Não deixa código válido para trás quando a tela fecha (o viewModelScope já foi cancelado).
        val code = _state.value.pairingCode ?: return
        val registry = registry ?: return
        GlobalScope.launch { registry.deletePairingCode(code) }
    }

    private companion object {
        const val TAG = "Sintoniza"
        const val CODE_REFRESH_MS = 9 * 60 * 1000L // skill aceita códigos de até 10 min
    }
}
