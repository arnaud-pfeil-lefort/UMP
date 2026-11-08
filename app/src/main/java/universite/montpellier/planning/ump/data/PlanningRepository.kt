package universite.montpellier.planning.ump.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import universite.montpellier.planning.ump.Cours
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Télécharge les plannings ADE et garde une copie locale par profil (files/plannings/<id>.ics),
 * pour pouvoir afficher le planning instantanément et hors connexion.
 */
class PlanningRepository(context: Context) {

    private val dossier = File(context.filesDir, "plannings")

    init {
        // Ancien emplacement du cache (avant les profils) : il n'est plus utilisé.
        File(context.filesDir, "adecal.ics").delete()
    }

    data class Resultat(val cours: List<Cours>, val miseAJour: Instant)

    private fun cache(profil: Profil) = File(dossier, "${profil.id}.ics")

    /** Dernière version enregistrée du planning de ce profil, ou null s'il n'a jamais été téléchargé. */
    suspend fun chargerCache(profil: Profil): Resultat? = withContext(Dispatchers.IO) {
        val fichier = cache(profil)
        if (!fichier.exists()) return@withContext null
        runCatching { Resultat(IcalParser.parse(fichier.readText()), Instant.ofEpochMilli(fichier.lastModified())) }
            .getOrNull()
    }

    /** Télécharge la dernière version du planning du profil, la met en cache et la renvoie. */
    suspend fun telecharger(profil: Profil): Resultat = withContext(Dispatchers.IO) {
        val contenu = telechargerTexte(profil.url)
        if (!contenu.contains("BEGIN:VCALENDAR")) throw IOException("Ce lien ne renvoie pas un calendrier iCal")
        val cours = IcalParser.parse(contenu) // on valide avant d'écraser le cache

        dossier.mkdirs()
        val tmp = File(dossier, "${profil.id}.ics.tmp")
        tmp.writeText(contenu)
        val fichier = cache(profil)
        if (!tmp.renameTo(fichier)) {
            fichier.delete()
            tmp.renameTo(fichier)
        }
        Resultat(cours, Instant.now())
    }

    fun supprimerCache(profil: Profil) {
        cache(profil).delete()
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
}
