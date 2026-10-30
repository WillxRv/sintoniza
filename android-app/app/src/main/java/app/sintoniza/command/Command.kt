package app.sintoniza.command

import org.json.JSONObject

/**
 * Comandos enviados pela skill (alexa-skill/lambda/src/commands.js).
 * O protocolo está documentado em docs/ARQUITETURA.md.
 */
sealed interface Command {
    /** [appId] é o id do catálogo (ex.: "netflix") ou null; [spokenName] é o que o usuário falou. */
    data class OpenApp(val appId: String?, val spokenName: String?) : Command

    data class YouTubeSearch(val query: String) : Command

    data class Media(val action: MediaAction) : Command

    /** [percent] para UP/DOWN, [level] (0–100) para SET. */
    data class Volume(val action: VolumeAction, val percent: Int = 0, val level: Int = 0) : Command

    data class Key(val key: TvKey, val times: Int) : Command
}

enum class MediaAction(val wire: String, val keyCode: Int) {
    PLAY("play", 126), // KEYCODE_MEDIA_PLAY
    PAUSE("pause", 127), // KEYCODE_MEDIA_PAUSE
    NEXT("next", 87), // KEYCODE_MEDIA_NEXT
    PREVIOUS("previous", 88), // KEYCODE_MEDIA_PREVIOUS
    FAST_FORWARD("fast_forward", 90), // KEYCODE_MEDIA_FAST_FORWARD
    REWIND("rewind", 89), // KEYCODE_MEDIA_REWIND
}

enum class VolumeAction(val wire: String) {
    UP("up"),
    DOWN("down"),
    SET("set"),
    MUTE("mute"),
    UNMUTE("unmute"),
}

enum class TvKey(val wire: String, val keyCode: Int) {
    UP("up", 19), // KEYCODE_DPAD_UP
    DOWN("down", 20), // KEYCODE_DPAD_DOWN
    LEFT("left", 21), // KEYCODE_DPAD_LEFT
    RIGHT("right", 22), // KEYCODE_DPAD_RIGHT
    OK("ok", 23), // KEYCODE_DPAD_CENTER
    BACK("back", 4), // KEYCODE_BACK
    HOME("home", 3), // KEYCODE_HOME
}

data class CommandEnvelope(val version: Int, val id: String, val sentAt: Long, val command: Command)

object CommandParser {
    const val PROTOCOL_VERSION = 1
    private const val MAX_KEY_REPEAT = 10

    /** @throws IllegalArgumentException se o JSON não for um comando válido desta versão. */
    fun parse(json: String): CommandEnvelope {
        val obj = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("JSON inválido", e)
        }

        val version = obj.optInt("v", -1)
        require(version == PROTOCOL_VERSION) { "Versão de protocolo não suportada: $version" }

        val command = when (val type = obj.optString("type")) {
            "open_app" -> Command.OpenApp(obj.optStringOrNull("app"), obj.optStringOrNull("name"))
                .also { require(it.appId != null || it.spokenName != null) { "open_app sem app" } }

            "youtube_search" -> Command.YouTubeSearch(
                obj.optStringOrNull("query") ?: throw IllegalArgumentException("youtube_search sem query"),
            )

            "media" -> Command.Media(enumByWire(obj.optString("action"), MediaAction.entries) { it.wire })

            "volume" -> Command.Volume(
                action = enumByWire(obj.optString("action"), VolumeAction.entries) { it.wire },
                percent = obj.optInt("percent", 10).coerceIn(1, 100),
                level = obj.optInt("level", 0).coerceIn(0, 100),
            )

            "key" -> Command.Key(
                key = enumByWire(obj.optString("key"), TvKey.entries) { it.wire },
                times = obj.optInt("times", 1).coerceIn(1, MAX_KEY_REPEAT),
            )

            else -> throw IllegalArgumentException("Tipo de comando desconhecido: $type")
        }

        return CommandEnvelope(version, obj.optString("id"), obj.optLong("sentAt"), command)
    }

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun <T> enumByWire(value: String, entries: List<T>, wire: (T) -> String): T =
        entries.firstOrNull { wire(it) == value } ?: throw IllegalArgumentException("Valor desconhecido: $value")
}
