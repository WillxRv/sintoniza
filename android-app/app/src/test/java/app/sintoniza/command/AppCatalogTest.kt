package app.sintoniza.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class AppCatalogTest {

    private val installed = listOf(
        InstalledApp("com.netflix.ninja", "Netflix"),
        InstalledApp("com.globo.globotv", "Globoplay"),
        InstalledApp("com.disney.disneyplus", "Disney+"),
        InstalledApp("br.com.claro.now", "Claro tv+"),
        InstalledApp("com.android.tv.settings", "Configurações"),
        InstalledApp("com.google.android.youtube.tv", "YouTube"),
        InstalledApp("com.google.android.youtube.tvkids", "YouTube Kids"),
    )

    @Test
    fun `normaliza acentos, simbolos e artigos`() {
        assertEquals("configuracoes", AppCatalog.normalize("Configurações"))
        assertEquals("disney", AppCatalog.normalize("Disney+"))
        assertEquals("globoplay", AppCatalog.normalize("o Globo Play"))
        assertEquals("netflix", AppCatalog.normalize("aplicativo Netflix"))
    }

    @Test
    fun `encontra pelo nome falado`() {
        assertEquals("com.globo.globotv", AppCatalog.bestMatch("globo play", installed)?.packageName)
        assertEquals("com.disney.disneyplus", AppCatalog.bestMatch("disney plus", installed)?.packageName)
        assertEquals("br.com.claro.now", AppCatalog.bestMatch("claro tv", installed)?.packageName)
    }

    @Test
    fun `nome exato vence prefixo`() {
        assertEquals("com.google.android.youtube.tv", AppCatalog.bestMatch("youtube", installed)?.packageName)
        assertEquals("com.google.android.youtube.tvkids", AppCatalog.bestMatch("youtube kids", installed)?.packageName)
    }

    @Test
    fun `sem correspondencia devolve null`() {
        assertNull(AppCatalog.bestMatch("spotify", installed))
        assertNull(AppCatalog.bestMatch("a", installed))
    }

    @Test
    fun `ids do catalogo batem com o slot APP_NAME da skill`() {
        val model = File("../../alexa-skill/skill-package/interactionModels/custom/pt-BR.json").readText()
        val skillIds = Regex("\"id\": \"([a-z_]+)\"").findAll(
            model.substringAfter("\"name\": \"APP_NAME\"").substringBefore("\"name\": \"NAV_KEY\""),
        ).map { it.groupValues[1] }.toSet()

        val appIds = AppCatalog.known.map { it.id }.toSet() + AppCatalog.SETTINGS_ID
        assertEquals(skillIds, appIds)
    }
}
