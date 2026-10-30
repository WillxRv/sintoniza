package app.sintoniza.command

import java.text.Normalizer

/** App conhecido pelo catálogo. Os ids batem com o slot APP_NAME da skill. */
data class KnownApp(val id: String, val packages: List<String>, val labels: List<String>)

/** App instalado na TV, como aparece no launcher. */
data class InstalledApp(val packageName: String, val label: String)

object AppCatalog {
    /**
     * Pacotes conhecidos (versões de TV primeiro). Se um pacote mudar ou faltar,
     * o app ainda é encontrado pelo nome no launcher via [labels].
     */
    val known: List<KnownApp> = listOf(
        KnownApp("youtube", listOf("com.google.android.youtube.tv", "com.google.android.youtube.googletv"), listOf("YouTube")),
        KnownApp("youtube_kids", listOf("com.google.android.youtube.tvkids"), listOf("YouTube Kids")),
        KnownApp("youtube_music", listOf("com.google.android.youtube.tvmusic"), listOf("YouTube Music")),
        KnownApp("netflix", listOf("com.netflix.ninja"), listOf("Netflix")),
        KnownApp("prime_video", listOf("com.amazon.amazonvideo.livingroom"), listOf("Prime Video", "Amazon Prime Video")),
        KnownApp("disney_plus", listOf("com.disney.disneyplus"), listOf("Disney+", "Disney Plus")),
        KnownApp("max", listOf("com.wbd.stream", "com.hbo.hbonow"), listOf("Max", "HBO Max")),
        KnownApp("globoplay", listOf("com.globo.globotv"), listOf("Globoplay")),
        KnownApp("paramount_plus", listOf("com.cbs.ott"), listOf("Paramount+", "Paramount Plus")),
        KnownApp("apple_tv", listOf("com.apple.atve.androidtv.appletv"), listOf("Apple TV")),
        KnownApp("spotify", listOf("com.spotify.tv.android"), listOf("Spotify")),
        KnownApp("deezer", listOf("deezer.android.tv"), listOf("Deezer")),
        KnownApp("twitch", listOf("tv.twitch.android.app", "tv.twitch.android.viewer"), listOf("Twitch")),
        KnownApp("pluto_tv", listOf("tv.pluto.android"), listOf("Pluto TV")),
        KnownApp("crunchyroll", listOf("com.crunchyroll.crunchyroid"), listOf("Crunchyroll")),
        KnownApp("plex", listOf("com.plexapp.android"), listOf("Plex")),
        KnownApp("kodi", listOf("org.xbmc.kodi"), listOf("Kodi")),
        KnownApp("vlc", listOf("org.videolan.vlc"), listOf("VLC")),
        KnownApp("stremio", listOf("com.stremio.one"), listOf("Stremio")),
        KnownApp("play_store", listOf("com.android.vending"), listOf("Google Play Store", "Play Store")),
    )

    const val SETTINGS_ID = "settings"
    const val YOUTUBE_ID = "youtube"

    fun byId(id: String?): KnownApp? = known.firstOrNull { it.id == id }

    /** "Disney+" -> "disney", "Globo Play" -> "globoplay", "Configurações" -> "configuracoes". */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .replace(Regex("^(o|a|os|as|app|aplicativo)\\s+"), "")
            .replace(Regex("[^a-z0-9]"), "")

    /**
     * Escolhe o app instalado cujo nome mais se parece com [query]:
     * igual > prefixo > contém. Empate: o nome mais curto. Nada parecido: null.
     */
    fun bestMatch(query: String, installed: List<InstalledApp>): InstalledApp? {
        val q = normalize(query)
        if (q.length < 2) return null

        return installed
            .map { it to score(q, normalize(it.label)) }
            .filter { (_, score) -> score > 0 }
            .sortedWith(compareByDescending<Pair<InstalledApp, Int>> { it.second }.thenBy { it.first.label.length })
            .firstOrNull()
            ?.first
    }

    private fun score(query: String, label: String): Int = when {
        label.isEmpty() -> 0
        label == query -> 3
        label.length >= 3 && query.length >= 3 && (label.startsWith(query) || query.startsWith(label)) -> 2
        label.length >= 3 && query.length >= 3 && (label.contains(query) || query.contains(label)) -> 1
        else -> 0
    }
}
