package universite.montpellier.planning.ump.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Un profil = un planning ADE (le sien, celui d'un ami…) identifié par son lien iCal. */
data class Profil(val id: String, val nom: String, val url: String)

/** Sauvegarde la liste des profils et le profil actif dans les préférences de l'application. */
class ProfilStore(context: Context) {

    private val prefs = context.getSharedPreferences("profils", Context.MODE_PRIVATE)

    fun charger(): List<Profil> {
        val json = JSONArray(prefs.getString(CLE_LISTE, "[]"))
        return (0 until json.length()).map { i ->
            val o = json.getJSONObject(i)
            Profil(o.getString("id"), o.getString("nom"), o.getString("url"))
        }
    }

    fun enregistrer(profils: List<Profil>) {
        val json = JSONArray()
        profils.forEach { json.put(JSONObject().put("id", it.id).put("nom", it.nom).put("url", it.url)) }
        prefs.edit().putString(CLE_LISTE, json.toString()).apply()
    }

    var actifId: String?
        get() = prefs.getString(CLE_ACTIF, null)
        set(value) = prefs.edit().putString(CLE_ACTIF, value).apply()

    private companion object {
        const val CLE_LISTE = "liste"
        const val CLE_ACTIF = "actif"
    }
}
