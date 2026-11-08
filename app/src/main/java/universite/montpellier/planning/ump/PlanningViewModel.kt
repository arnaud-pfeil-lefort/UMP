package universite.montpellier.planning.ump

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import universite.montpellier.planning.ump.data.PlanningRepository
import java.time.Instant
import java.time.LocalDate

data class PlanningEtat(
    val coursParJour: Map<LocalDate, List<Cours>> = emptyMap(),
    val chargementInitial: Boolean = true,
    val rafraichissement: Boolean = false,
    val miseAJour: Instant? = null,
    val erreur: String? = null,
)

class PlanningViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlanningRepository(application)

    var etat by mutableStateOf(PlanningEtat())
        private set

    init {
        viewModelScope.launch {
            // Affiche tout de suite les données locales, puis récupère la version à jour depuis ADE.
            runCatching { repository.chargerLocal() }.onSuccess(::appliquer)
            etat = etat.copy(chargementInitial = false)
            rafraichir()
        }
    }

    fun rafraichir() {
        if (etat.rafraichissement) return
        etat = etat.copy(rafraichissement = true, erreur = null)
        viewModelScope.launch {
            runCatching { repository.telecharger() }
                .onSuccess(::appliquer)
                .onFailure { etat = etat.copy(erreur = "Mise à jour impossible — affichage de la dernière version enregistrée") }
            etat = etat.copy(rafraichissement = false)
        }
    }

    fun ignorerErreur() {
        etat = etat.copy(erreur = null)
    }

    private fun appliquer(resultat: PlanningRepository.Resultat) {
        etat = etat.copy(
            coursParJour = resultat.cours.groupBy { it.jour },
            miseAJour = resultat.miseAJour,
        )
    }
}
