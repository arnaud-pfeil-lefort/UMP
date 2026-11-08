package universite.montpellier.planning.ump

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import universite.montpellier.planning.ump.composes.EcranBienvenue
import universite.montpellier.planning.ump.composes.FeuilleProfils
import universite.montpellier.planning.ump.composes.PlanningScreen
import universite.montpellier.planning.ump.ui.theme.UMPTheme

class MainActivity : ComponentActivity() {

    // Le ViewModel télécharge le planning du profil actif à chaque lancement de l'application.
    private val viewModel: PlanningViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UMPTheme {
                val etat = viewModel.etat
                var profilsOuverts by rememberSaveable { mutableStateOf(false) }

                if (etat.profilActif == null) {
                    // Premier lancement (ou tous les profils supprimés) : on demande un lien.
                    EcranBienvenue(
                        etat = etat,
                        onValider = { nom, url -> viewModel.enregistrerProfil(null, nom, url) {} },
                        onChange = viewModel::effacerErreurFormulaire,
                    )
                } else {
                    PlanningScreen(
                        etat = etat,
                        onRafraichir = viewModel::rafraichir,
                        onIgnorerErreur = viewModel::ignorerErreur,
                        onOuvrirProfils = { profilsOuverts = true },
                    )
                    if (profilsOuverts) {
                        FeuilleProfils(
                            etat = etat,
                            onFermer = { profilsOuverts = false; viewModel.effacerErreurFormulaire() },
                            onChoisir = viewModel::choisirProfil,
                            onEnregistrer = viewModel::enregistrerProfil,
                            onSupprimer = viewModel::supprimerProfil,
                            onChangeFormulaire = viewModel::effacerErreurFormulaire,
                        )
                    }
                }
            }
        }
    }
}
