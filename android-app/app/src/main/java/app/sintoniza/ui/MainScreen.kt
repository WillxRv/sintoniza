package app.sintoniza.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme

private val Accent = Color(0xFF4FD1C5)
private val Ok = Color(0xFF68D391)
private val Warn = Color(0xFFF6AD55)
private val PanelColor = Color(0xFF242A4D)

@Composable
fun SintonizaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Color(0xFF1B1F3B),
            surface = PanelColor,
        ),
        content = content,
    )
}

@Composable
fun MainScreen(
    state: UiState,
    onNewCode: () -> Unit,
    onUnpairAll: () -> Unit,
    onGrantOverlay: () -> Unit,
    onEnableAdb: () -> Unit,
    onDisableAdb: () -> Unit,
    onApplyConfig: () -> Unit,
    onDismissConfig: () -> Unit,
    onResetServer: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.dp, vertical = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column(modifier = Modifier.weight(1.2f).verticalScroll(rememberScrollState())) {
            Text("Sintoniza", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Accent)
            Text("Controle esta TV pela Alexa", fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(24.dp))

            when {
                state.pendingConfig != null -> PendingConfigPanel(state, onApplyConfig, onDismissConfig)
                !state.serverConfigured -> Panel {
                    Title("Servidor não configurado")
                    Body("Este build não tem a configuração do Firebase. Veja docs/AUTO-HOSPEDAGEM.md.")
                }
                state.loading -> Panel { Body("Conectando…") }
                else -> PairingPanel(state, onNewCode, onUnpairAll)
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Warn, fontSize = 14.sp)
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Panel {
                Title("Abrir apps em segundo plano")
                if (state.overlayGranted) {
                    Status("Permissão concedida", Ok)
                } else {
                    Status("Falta a permissão \"Sobrepor a outros apps\"", Warn)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onGrantOverlay) { Text("Conceder") }
                }
            }

            Panel {
                Title("Controle avançado (navegação)")
                Body("Permite setas, OK e voltar. Requer \"Depuração USB\" nas opções do desenvolvedor.")
                Spacer(Modifier.height(8.dp))
                when {
                    state.adbBusy -> Status("Conectando… aceite o aviso na TV (marque \"Sempre permitir\")", Accent)
                    state.adbEnabled -> {
                        Status("Ativo", Ok)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onDisableAdb) { Text("Desativar") }
                    }
                    else -> Button(onClick = onEnableAdb) { Text("Ativar") }
                }
                state.adbError?.let { Text(it, color = Warn, fontSize = 13.sp) }
            }

            Panel {
                Title("Último comando")
                Body(state.lastCommand ?: "Nenhum ainda")
            }

            if (state.usingCustomServer) {
                OutlinedButton(onClick = onResetServer) { Text("Usar servidor oficial") }
            }
        }
    }
}

@Composable
private fun PairingPanel(state: UiState, onNewCode: () -> Unit, onUnpairAll: () -> Unit) {
    Panel {
        if (state.paired) {
            Status("Pareado com a Alexa ✓", Ok)
            Spacer(Modifier.height(12.dp))
            Body("Experimente: \"Alexa, pede pro Sintoniza abrir o YouTube\".")
        }

        val code = state.pairingCode
        if (code != null) {
            if (state.paired) Spacer(Modifier.height(16.dp))
            Title("Diga para a Alexa:")
            Body("\"Alexa, pede pro Sintoniza parear com o código…\"")
            Spacer(Modifier.height(12.dp))
            Text(
                text = code.chunked(3).joinToString(" "),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                color = Color.White,
            )
            Body("O código vale por 10 minutos e é renovado sozinho.")
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onNewCode) { Text(if (state.paired) "Parear outra Alexa" else "Novo código") }
            if (state.paired) OutlinedButton(onClick = onUnpairAll) { Text("Desparear todas") }
        }
    }
}

@Composable
private fun PendingConfigPanel(state: UiState, onApply: () -> Unit, onDismiss: () -> Unit) {
    val config = state.pendingConfig ?: return
    Panel {
        Title("Trocar de servidor?")
        Body("Um comando pediu para usar outro projeto Firebase:")
        Spacer(Modifier.height(8.dp))
        Body("Projeto: ${config.projectId}")
        Body("Só aceite se foi você quem configurou.")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onApply) { Text("Usar este servidor") }
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelColor, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) { content() }
}

@Composable
private fun Title(text: String) = Text(text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

@Composable
private fun Body(text: String) = Text(text, fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f))

@Composable
private fun Status(text: String, color: Color) = Row(verticalAlignment = Alignment.CenterVertically) {
    Text("● ", color = color, fontSize = 16.sp)
    Text(text, color = color, fontSize = 16.sp)
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun MainScreenPreview() = SintonizaTheme {
    MainScreen(
        state = UiState(serverConfigured = true, loading = false, pairingCode = "482913", lastCommand = "abrir youtube"),
        onNewCode = {}, onUnpairAll = {}, onGrantOverlay = {}, onEnableAdb = {}, onDisableAdb = {},
        onApplyConfig = {}, onDismissConfig = {}, onResetServer = {},
    )
}
