package universite.montpellier.planning.ump

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import universite.montpellier.planning.ump.data.PlanningRepository
import universite.montpellier.planning.ump.data.Profil
import universite.montpellier.planning.ump.data.ProfilStore
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class PlanningEtat(
    val profils: List<Profil> = emptyList(),
    val profilActif: Profil? = null,
    val coursParJour: Map<LocalDate, List<Cours>> = emptyMap(),
    val chargementInitial: Boolean = true,
    val rafraichissement: Boolean = false,
    val miseAJour: Instant? = null,
    val erreur: String? = null,
    /** Vérification d'un lien en cours dans le formulaire de profil. */
    val formulaireEnCours: Boolean = false,
    val erreurFormulaire: String? = null,
)

class PlanningViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlanningRepository(application)
    private val store = ProfilStore(application)
    private var jobRafraichir: Job? = null

    var etat by mutableStateOf(PlanningEtat())
        private set

    init {
        val profils = store.charger()
        etat = etat.copy(profils = profils)
        val actif = profils.firstOrNull { it.id == store.actifId } ?: profils.firstOrNull()
        if (actif != null) choisirProfil(actif.id) else etat = etat.copy(chargementInitial = false)
    }

    /** Active un profil : affiche son planning enregistré puis télécharge la version à jour. */
    fun choisirProfil(id: String) {
        val profil = etat.profils.firstOrNull { it.id == id } ?: return
        jobRafraichir?.cancel()
        store.actifId = id
        etat = etat.copy(
            profilActif = profil,
            coursParJour = emptyMap(),
            miseAJour = null,
            chargementInitial = true,
            rafraichissement = false,
            erreur = null,
        )
        viewModelScope.launch {
            val cache = repository.chargerCache(profil)
            if (etat.profilActif?.id != id) return@launch
            if (cache != null) appliquer(cache)
            etat = etat.copy(chargementInitial = false)
            rafraichir()
        }
    }

    fun rafraichir() {
        val profil = etat.profilActif ?: return
        if (jobRafraichir?.isActive == true) return
        etat = etat.copy(rafraichissement = true, erreur = null)
        jobRafraichir = viewModelScope.launch {
            val resultat = runCatching { repository.telecharger(profil) }
            if (!isActive || etat.profilActif?.id != profil.id) return@launch
            resultat
                .onSuccess(::appliquer)
                .onFailure {
                    etat = etat.copy(
                        erreur = if (etat.miseAJour == null) "Impossible de récupérer le planning (hors ligne ?)"
                        else "Mise à jour impossible — affichage de la dernière version enregistrée"
                    )
                }
            etat = etat.copy(rafraichissement = false)
        }
    }

    fun ignorerErreur() {
        etat = etat.copy(erreur = null)
    }

    /**
     * Crée un profil (id null) ou modifie un profil existant. Si le lien est nouveau, il est d'abord
     * vérifié en téléchargeant le planning : on n'enregistre jamais un lien qui ne fonctionne pas.
     */
    fun enregistrerProfil(id: String?, nom: String, url: String, onSucces: () -> Unit) {
        val lien = url.trim()
        if (!lien.startsWith("http://") && !lien.startsWith("https://")) {
            etat = etat.copy(erreurFormulaire = "Le lien doit commencer par https://")
            return
        }
        val existant = id?.let { i -> etat.profils.firstOrNull { it.id == i } }
        val profil = Profil(
            id = existant?.id ?: UUID.randomUUID().toString(),
            nom = nom.trim().ifEmpty { existant?.nom ?: "Profil ${etat.profils.size + 1}" },
            url = lien,
        )

        etat = etat.copy(formulaireEnCours = true, erreurFormulaire = null)
        viewModelScope.launch {
            if (existant == null || existant.url != lien) {
                val verification = runCatching { repository.telecharger(profil) }
                if (verification.isFailure) {
                    etat = etat.copy(
                        formulaireEnCours = false,
                        erreurFormulaire = "Impossible de récupérer un planning avec ce lien. Vérifie le lien et ta connexion.",
                    )
                    return@launch
                }
            }

            val profils = if (existant == null) etat.profils + profil
            else etat.profils.map { if (it.id == profil.id) profil else it }
            store.enregistrer(profils)
            etat = etat.copy(profils = profils, formulaireEnCours = false)

            when {
                // Nouveau profil, ou lien du profil affiché modifié : on (ré)affiche ce planning.
                existant == null || (existant.url != lien && etat.profilActif?.id == profil.id) -> choisirProfil(profil.id)
                etat.profilActif?.id == profil.id -> etat = etat.copy(profilActif = profil)
            }
            onSucces()
        }
    }

    fun supprimerProfil(id: String) {
        val profil = etat.profils.firstOrNull { it.id == id } ?: return
        repository.supprimerCache(profil)
        val profils = etat.profils - profil
        store.enregistrer(profils)
        etat = etat.copy(profils = profils)

        if (etat.profilActif?.id == id) {
            val suivant = profils.firstOrNull()
            if (suivant != null) {
                choisirProfil(suivant.id)
            } else {
                jobRafraichir?.cancel()
                store.actifId = null
                etat = PlanningEtat(chargementInitial = false)
            }
        }
    }

    fun effacerErreurFormulaire() {
        etat = etat.copy(erreurFormulaire = null)
    }

    private fun appliquer(resultat: PlanningRepository.Resultat) {
        etat = etat.copy(
            coursParJour = resultat.cours.groupBy { it.jour },
            miseAJour = resultat.miseAJour,
        )
    }
}
