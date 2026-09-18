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



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val events = ReadIcalFile.getDatas(this)
        val courss = ArrayList<Cours>()
        for (event in events) {
            val cours =
                Cours(
                    nom = event.summary.value,
                    dateDebut = event.dateStart.value.toString(),
                    dateFin = event.dateEnd.value.toString(),
                    salle = event.location.value.toString(),
                )
            courss.add(cours)
        }

        enableEdgeToEdge()
        setContent {
            UMPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding),
                        courss = courss,
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(courss: List<Cours>, modifier: Modifier = Modifier) {
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
        Row {
            ListeCours(
                courss = courss
            )
        }
    }
}

@Composable
fun ListeCours(courss: List<Cours>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(courss) { cours ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(cours.nom, style = MaterialTheme.typography.titleMedium)
                Text("${cours.dateDebut} → ${cours.dateFin}")
                Text(cours.salle)
            }
        }
    }
}
