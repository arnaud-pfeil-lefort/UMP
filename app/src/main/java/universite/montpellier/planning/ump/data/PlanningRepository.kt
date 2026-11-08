package universite.montpellier.planning.ump.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import universite.montpellier.planning.ump.Cours
import universite.montpellier.planning.ump.R
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Source des données du planning :
 *  1. le fichier téléchargé depuis ADE (mis en cache dans le stockage interne),
 *  2. à défaut, le fichier embarqué dans l'APK (res/raw/adecal.ics).
 */
class PlanningRepository(private val context: Context) {

    private val cache = File(context.filesDir, "adecal.ics")

    data class Resultat(val cours: List<Cours>, val miseAJour: Instant?)

    /** Charge immédiatement les données locales (dernier téléchargement, sinon fichier embarqué). */
    suspend fun chargerLocal(): Resultat = withContext(Dispatchers.IO) {
        if (cache.exists()) {
            val depuisCache = runCatching {
                Resultat(IcalParser.parse(cache.readText()), Instant.ofEpochMilli(cache.lastModified()))
            }.getOrNull()
            if (depuisCache != null) return@withContext depuisCache
        }
        val embarque = context.resources.openRawResource(R.raw.adecal).bufferedReader().use { it.readText() }
        Resultat(IcalParser.parse(embarque), null)
    }

    /** Télécharge la dernière version du planning depuis ADE, la met en cache et la renvoie. */
    suspend fun telecharger(): Resultat = withContext(Dispatchers.IO) {
        val contenu = telechargerTexte(ICAL_URL)
        if (!contenu.contains("BEGIN:VCALENDAR")) throw IOException("Réponse inattendue du serveur ADE")
        val cours = IcalParser.parse(contenu) // on valide avant d'écraser le cache

        val tmp = File(context.filesDir, "adecal.ics.tmp")
        tmp.writeText(contenu)
        if (!tmp.renameTo(cache)) {
            cache.delete()
            tmp.renameTo(cache)
        }
        Resultat(cours, Instant.now())
    }

    private fun telechargerTexte(adresse: String): String {
        val connexion = URL(adresse).openConnection() as HttpURLConnection
        try {
            connexion.connectTimeout = 15_000
            connexion.readTimeout = 30_000
            connexion.instanceFollowRedirects = true
            connexion.useCaches = false
            connexion.setRequestProperty("Accept", "text/calendar, */*")
            val code = connexion.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return connexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connexion.disconnect()
        }
    }

    companion object {
        const val ICAL_URL =
            "https://proseconsult.umontpellier.fr/jsp/custom/modules/plannings/direct_cal.jsp?data=" +
                "58c99062bab31d256bee14356aca3f2423c0f022cb9660eba051b2653be722c4c08ac8ba0419d2b4ed736e8036" +
                "93aacedc5c094f7d1a811b903031bde802c7f5dabe87749382d6bf26d6f96bc6ef50b8d4f3447fb15789446772" +
                "a717328d6264111a08fa32aa1cc99cb2980c20462b1d166c54e36382c1aa3eb0ff5cb8980cdb,1"
    }
}
