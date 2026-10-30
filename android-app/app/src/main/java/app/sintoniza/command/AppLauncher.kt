package app.sintoniza.command

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

/** Transforma "abrir X" / "procurar X no YouTube" em Intents. */
class AppLauncher(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    fun resolve(appId: String?, spokenName: String?): Intent? {
        if (appId == AppCatalog.SETTINGS_ID) return Intent(Settings.ACTION_SETTINGS)

        val known = AppCatalog.byId(appId)
        known?.packages?.firstNotNullOfOrNull(::launchIntentFor)?.let { return it }

        // Pacote desconhecido ou diferente nesta TV: procura pelo nome no launcher.
        val installed = installedApps()
        val names = known?.labels.orEmpty() + listOfNotNull(spokenName)
        return names.firstNotNullOfOrNull { AppCatalog.bestMatch(it, installed) }
            ?.let { launchIntentFor(it.packageName) }
    }

    fun youtubeSearch(query: String): Intent? {
        val pkg = AppCatalog.byId(AppCatalog.YOUTUBE_ID)!!.packages.firstOrNull(::isInstalled) ?: return null

        val search = Intent(Intent.ACTION_SEARCH)
            .setPackage(pkg)
            .putExtra(SearchManager.QUERY, query)
        if (search.resolveActivity(pm) != null) return search

        val view = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query)))
            .setPackage(pkg)
        return view.takeIf { it.resolveActivity(pm) != null }
    }

    fun homeIntent(): Intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    private fun launchIntentFor(pkg: String): Intent? =
        pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)

    private fun isInstalled(pkg: String): Boolean = try {
        pm.getPackageInfo(pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun installedApps(): List<InstalledApp> =
        listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
            .flatMap { category ->
                pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)
            }
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
}
