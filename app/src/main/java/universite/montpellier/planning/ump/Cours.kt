package universite.montpellier.planning.ump

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Un créneau de cours issu du fichier iCal ADE.
 *
 * @param code      code de l'UE (ex. "HAI305I"), null pour les événements sans code (conférence, réunion…)
 * @param nom       nom lisible de la matière (ex. "Programmation web")
 * @param precision complément du titre ADE (ex. "Info GrB"), vide si absent
 * @param couleur   index dans la palette de couleurs des matières (stable pour une même matière)
 */
data class Cours(
    val code: String?,
    val nom: String,
    val precision: String,
    val dateDebut: LocalDateTime,
    val dateFin: LocalDateTime,
    val salle: String,
    val enseignants: List<String>,
    val groupes: List<String>,
    val couleur: Int,
) {
    val jour: LocalDate get() = dateDebut.toLocalDate()
}
