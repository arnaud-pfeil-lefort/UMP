package universite.montpellier.planning.ump

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import universite.montpellier.planning.ump.ui.theme.UMPTheme
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import universite.montpellier.planning.ump.UmpUtils.Companion.filtreCours
import java.time.LocalDateTime


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val coursData = ReadIcalFile.getDatas(this)

        enableEdgeToEdge()
        setContent {
            UMPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding),
                        coursData = coursData,
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(coursData: List<Cours>, modifier: Modifier = Modifier) {

    var currentDay by remember { mutableStateOf(LocalDateTime.now()) }
    val currentCours = remember(currentDay) { filtreCours(coursData, currentDay) }

    Column(
        modifier = Modifier
            .padding(
                top = 48.dp,
                start = 32.dp,
                bottom = 32.dp

            )
    ) {
        Row {
            Text(
                text = "Planning"
            )
        }
        Row(
            modifier = Modifier
                .padding(16.dp)
        ) {
            BoutonJour(onPresser = { currentDay = currentDay.minusDays(1) }, "Précédent")
            Text(text = currentDay.dayOfMonth.toString() + " " + currentDay.monthValue)
            BoutonJour(onPresser = { currentDay = currentDay.plusDays(1) }, "Suivant")
        }
        Row {
            ListeCours(
                courss = currentCours
            )
        }
    }
}

@Composable
fun BoutonJour(onPresser: () -> Unit, jour: String) {
    Button(onClick = onPresser) {
        Text("Jour" + jour)
    }
}



@Composable
fun ListeCours(courss: List<Cours>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(courss) { cours ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(cours.nom, style = MaterialTheme.typography.titleMedium)
                Text("${cours.dateDebut.toString()} → ${cours.dateFin.toString()}")
                Text(cours.salle)
            }
        }
    }
}
