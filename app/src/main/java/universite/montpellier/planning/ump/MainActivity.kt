package universite.montpellier.planning.ump

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import universite.montpellier.planning.ump.composes.PlanningScreen
import universite.montpellier.planning.ump.ui.theme.UMPTheme

class MainActivity : ComponentActivity() {

    // Le ViewModel télécharge le planning à chaque lancement de l'application.
    private val viewModel: PlanningViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UMPTheme {
                PlanningScreen(
                    etat = viewModel.etat,
                    onRafraichir = viewModel::rafraichir,
                    onIgnorerErreur = viewModel::ignorerErreur,
                )
            }
        }
    }
}
