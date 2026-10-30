package app.sintoniza.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleConfigIntent(intent)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            SintonizaTheme {
                MainScreen(
                    state = state,
                    onNewCode = viewModel::newCode,
                    onUnpairAll = viewModel::unpairAll,
                    onGrantOverlay = ::openOverlaySettings,
                    onEnableAdb = viewModel::enableAdb,
                    onDisableAdb = viewModel::disableAdb,
                    onApplyConfig = { if (viewModel.applyPendingConfig()) restart() },
                    onDismissConfig = viewModel::dismissPendingConfig,
                    onResetServer = { if (viewModel.resetToOfficialServer()) restart() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleConfigIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshLocalStatus()
    }

    /**
     * Auto-hospedagem: `adb shell am start -n app.sintoniza/.ui.MainActivity --es server_config '<json>'`.
     * A troca só acontece depois que o usuário confirma na tela.
     */
    private fun handleConfigIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_SERVER_CONFIG)?.let(viewModel::offerConfig)
    }

    private fun openOverlaySettings() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                "Esta TV não tem a tela de sobreposição. Ative o controle avançado: ele concede a permissão sozinho.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun restart() {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
        Process.killProcess(Process.myPid())
    }

    private companion object {
        const val EXTRA_SERVER_CONFIG = "server_config"
    }
}
