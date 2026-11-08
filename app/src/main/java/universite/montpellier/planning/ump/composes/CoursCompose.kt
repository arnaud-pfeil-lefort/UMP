package universite.montpellier.planning.ump.composes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import universite.montpellier.planning.ump.Cours
import universite.montpellier.planning.ump.ui.theme.couleurMatiere
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun LocalDateTime.heureCourte(): String = "%dh%02d".format(hour, minute)

fun dureeLisible(cours: Cours): String {
    val minutes = Duration.between(cours.dateDebut, cours.dateFin).toMinutes()
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "${m} min"
        m == 0L -> "${h} h"
        else -> "${h} h ${m}"
    }
}

/** Fond teinté de la couleur de la matière, opaque pour masquer les lignes de la grille horaire. */
@Composable
fun fondMatiere(accent: Color): Color {
    val alpha = if (isSystemInDarkTheme()) 0.28f else 0.14f
    return accent.copy(alpha = alpha).compositeOver(MaterialTheme.colorScheme.surfaceContainer)
}

/**
 * Carte d'un cours dans la grille de la journée. Le contenu s'adapte à la hauteur disponible :
 * les créneaux courts n'affichent que l'essentiel.
 */
@Composable
fun CoursCompose(cours: Cours, hauteur: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = couleurMatiere(cours.couleur)
    val compact = hauteur < 64.dp

    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(fondMatiere(accent))
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(accent)
        )
        Column(
            Modifier
                .padding(horizontal = 10.dp, vertical = if (compact) 4.dp else 8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${cours.dateDebut.heureCourte()} – ${cours.dateFin.heureCourte()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
                if (cours.code != null) {
                    Text(
                        "  ·  ${cours.code}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                cours.nom,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!compact) {
                val infos = listOf(cours.salle, cours.precision).filter { it.isNotBlank() }.joinToString("  ·  ")
                if (infos.isNotEmpty()) {
                    Text(
                        infos,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (hauteur >= 110.dp && cours.enseignants.isNotEmpty()) {
                    Text(
                        cours.enseignants.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Feuille de détails affichée quand on touche un cours. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsCours(cours: Cours, onFermer: () -> Unit) {
    val accent = couleurMatiere(cours.couleur)
    val formatJour = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

    ModalBottomSheet(onDismissRequest = onFermer) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    listOfNotNull(cours.code, cours.precision.ifBlank { null }).joinToString(" · ").ifEmpty { "Événement" },
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(cours.nom, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LigneDetail(
                "Horaire",
                "${formatJour.format(cours.dateDebut).replaceFirstChar { it.titlecase(Locale.FRENCH) }}\n" +
                    "${cours.dateDebut.heureCourte()} – ${cours.dateFin.heureCourte()}  (${dureeLisible(cours)})"
            )
            if (cours.salle.isNotBlank()) LigneDetail("Salle", cours.salle.replace(",", "\n"))
            if (cours.enseignants.isNotEmpty()) {
                LigneDetail(if (cours.enseignants.size > 1) "Enseignants" else "Enseignant", cours.enseignants.joinToString("\n"))
            }
            if (cours.groupes.isNotEmpty()) LigneDetail("Groupes", cours.groupes.joinToString("\n"))
        }
    }
}

@Composable
private fun LigneDetail(titre: String, valeur: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            titre.uppercase(Locale.FRENCH),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(valeur, style = MaterialTheme.typography.bodyLarge)
    }
}
