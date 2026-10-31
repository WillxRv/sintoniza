package app.sintoniza.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CommandParserTest {

    private fun parse(json: String) = CommandParser.parse(json).command

    @Test
    fun `abre app pelo id e nome falado`() {
        assertEquals(
            Command.OpenApp("netflix", "netflics"),
            parse("""{"v":1,"id":"x","sentAt":1,"type":"open_app","app":"netflix","name":"netflics"}"""),
        )
    }

    @Test
    fun `abre app sem id do catalogo`() {
        assertEquals(
            Command.OpenApp(null, "claro tv"),
            parse("""{"v":1,"type":"open_app","app":null,"name":"claro tv"}"""),
        )
    }

    @Test
    fun `teclas e repeticao limitada`() {
        assertEquals(Command.Key(TvKey.DOWN, 10), parse("""{"v":1,"type":"key","key":"down","times":50}"""))
        assertEquals(Command.Key(TvKey.OK, 1), parse("""{"v":1,"type":"key","key":"ok"}"""))
    }

    @Test
    fun `volume e midia`() {
        assertEquals(
            Command.Volume(VolumeAction.SET, percent = 10, level = 30),
            parse("""{"v":1,"type":"volume","action":"set","level":30}"""),
        )
        assertEquals(Command.Media(MediaAction.FAST_FORWARD), parse("""{"v":1,"type":"media","action":"fast_forward"}"""))
    }

    @Test
    fun `avancar com segundos e sem segundos`() {
        assertEquals(
            Command.Media(MediaAction.FAST_FORWARD, seconds = 300),
            parse("""{"v":1,"type":"media","action":"fast_forward","seconds":300}"""),
        )
        assertEquals(Command.Media(MediaAction.REWIND, seconds = null), parse("""{"v":1,"type":"media","action":"rewind"}"""))
        assertEquals(Command.Media(MediaAction.REWIND, seconds = null), parse("""{"v":1,"type":"media","action":"rewind","seconds":-5}"""))
    }

    @Test
    fun `envelope preserva id e horario`() {
        val envelope = CommandParser.parse("""{"v":1,"id":"abc","sentAt":123,"type":"media","action":"pause"}""")
        assertEquals("abc", envelope.id)
        assertEquals(123L, envelope.sentAt)
    }

    @Test
    fun `rejeita entradas invalidas`() {
        listOf(
            "não é json",
            """{"v":2,"type":"media","action":"pause"}""",
            """{"v":1,"type":"desligar"}""",
            """{"v":1,"type":"media","action":"explodir"}""",
            """{"v":1,"type":"open_app"}""",
            """{"v":1,"type":"youtube_search","query":""}""",
        ).forEach { json ->
            assertThrows(json, IllegalArgumentException::class.java) { CommandParser.parse(json) }
        }
    }
}
