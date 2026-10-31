package app.sintoniza.messaging

import android.content.Context
import app.sintoniza.command.Command

/** Guarda o último comando recebido para mostrar na tela (ajuda a diagnosticar). */
object LastCommand {
    private const val PREFS = "last_command"

    fun record(context: Context, command: Command) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("text", describe(command))
            .putLong("at", System.currentTimeMillis())
            .apply()
    }

    /** @return descrição e horário (epoch ms), ou null se nada chegou ainda. */
    fun read(context: Context): Pair<String, Long>? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val text = prefs.getString("text", null) ?: return null
        return text to prefs.getLong("at", 0)
    }

    fun describe(command: Command): String = when (command) {
        is Command.OpenApp -> "abrir ${command.spokenName ?: command.appId}"
        is Command.YouTubeSearch -> "procurar \"${command.query}\" no YouTube"
        is Command.Media -> "mídia: ${command.action.wire}" + (command.seconds?.let { " ${it}s" } ?: "")
        is Command.Volume -> when (command.action.wire) {
            "set" -> "volume em ${command.level}%"
            "up", "down" -> "volume ${command.action.wire} ${command.percent}%"
            else -> "volume: ${command.action.wire}"
        }
        is Command.Key -> "tecla ${command.key.wire}" + if (command.times > 1) " ×${command.times}" else ""
    }
}
